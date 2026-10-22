package com.netby.transferencias.application.service;

import com.netby.transferencias.application.in.FiltroTransferencias;
import com.netby.transferencias.domain.shared.Pagina;
import com.netby.transferencias.domain.shared.Paginacion;
import com.netby.transferencias.application.in.ConsultarHistorialTransferencias;
import com.netby.transferencias.application.in.RealizarTransferencia;
import com.netby.transferencias.application.out.CuentaRepository;
import com.netby.transferencias.application.out.TransferenciaRepository;
import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.CuentaNoEncontradaException;
import com.netby.transferencias.domain.error.DatosInvalidosException;
import com.netby.transferencias.domain.error.ReglaNegocioException;
import com.netby.transferencias.domain.transferencia.DeteccionSospechosaService;
import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import com.netby.transferencias.domain.transferencia.LimiteDiarioService;
import com.netby.transferencias.domain.transferencia.Periodo;
import com.netby.transferencias.domain.transferencia.PoliticaTransferencia;
import com.netby.transferencias.domain.transferencia.Transferencia;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@ApplicationScoped
public class TransferenciaService implements RealizarTransferencia, ConsultarHistorialTransferencias {

    private final CuentaRepository cuentas;
    private final TransferenciaRepository transferencias;
    private final DeteccionSospechosaService deteccion;
    private final LimiteDiarioService limiteDiario;
    private final Clock reloj;

    @Inject
    public TransferenciaService(CuentaRepository cuentas, TransferenciaRepository transferencias,
                                DeteccionSospechosaService deteccion, LimiteDiarioService limiteDiario, Clock reloj) {
        this.cuentas = cuentas;
        this.transferencias = transferencias;
        this.deteccion = deteccion;
        this.limiteDiario = limiteDiario;
        this.reloj = reloj;
    }

    /**
     * Un rechazo por regla (422) se persiste como RECHAZADA y NO revierte la transacción (D2); cualquier otro
     * error (404, 409, técnico) sí revierte.
     */
    @Override
    @Transactional(dontRollbackOn = ReglaNegocioException.class)
    public Transferencia realizar(RealizarTransferencia.Command comando) {
        Transferencia candidata = validarSolicitud(comando);
        Dinero monto = candidata.monto();

        Map<NumeroCuenta, Cuenta> bloqueadas = bloquearEnOrden(candidata.cuentaOrigen(), candidata.cuentaDestino());
        Cuenta cuentaOrigen = bloqueadas.get(candidata.cuentaOrigen());
        Cuenta cuentaDestino = bloqueadas.get(candidata.cuentaDestino());

        long previasUltimaHora = verificarReglas(cuentaOrigen, cuentaDestino, candidata);
        Transferencia resultado = deteccion.esSospechosa(monto, previasUltimaHora)
                ? Transferencia.enRevision(candidata.cuentaOrigen(), candidata.cuentaDestino(), monto, candidata.fecha())
                : candidata;

        cuentaOrigen.debitar(monto);
        cuentaDestino.acreditar(monto);
        cuentas.guardar(cuentaOrigen);
        cuentas.guardar(cuentaDestino);
        return transferencias.guardar(resultado);
    }

    /** Valida los datos de entrada (400) y construye la transferencia candidata, aún no aceptada por las reglas. */
    private Transferencia validarSolicitud(RealizarTransferencia.Command comando) {
        NumeroCuenta origen = NumeroCuenta.de(comando.cuentaOrigen());
        NumeroCuenta destino = NumeroCuenta.de(comando.cuentaDestino());
        Dinero monto = Dinero.de(comando.monto());
        return Transferencia.completada(origen, destino, monto, reloj.instant()); // valida invariantes (400)
    }

    /** Bloquea ambas cuentas en orden ascendente de número (evita deadlock A↔B). */
    private Map<NumeroCuenta, Cuenta> bloquearEnOrden(NumeroCuenta origen, NumeroCuenta destino) {
        Map<NumeroCuenta, Cuenta> bloqueadas = new HashMap<>();
        for (NumeroCuenta numero : Stream.of(origen, destino).sorted().toList()) {
            bloqueadas.put(numero, cuentas.buscarPorNumeroParaActualizar(numero).orElseThrow(
                    () -> new CuentaNoEncontradaException("No existe la cuenta " + numero.enmascarado())));
        }
        return bloqueadas;
    }

    /** Aplica las reglas (422); si alguna falla registra el rechazo y relanza. Devuelve las previas de la última hora. */
    private long verificarReglas(Cuenta origen, Cuenta destino, Transferencia candidata) {
        try {
            PoliticaTransferencia.validar(origen, destino, candidata.monto());
            Dinero acumuladoHoy = transferencias.sumar(
                    origen.numero(), EstadoTransferencia.CONSUMEN_CUPO, Periodo.diaCalendario(reloj));
            limiteDiario.verificar(acumuladoHoy, candidata.monto());
        } catch (ReglaNegocioException e) {
            rechazar(candidata, e);
            throw e;
        }
        return contarPrevias(origen);
    }

    private void rechazar(Transferencia candidata, ReglaNegocioException motivo) {
        transferencias.guardar(Transferencia.rechazada(candidata.cuentaOrigen(), candidata.cuentaDestino(),
                candidata.monto(), candidata.fecha(), motivo.getMessage()));
    }

    private long contarPrevias(Cuenta origen) {
        return transferencias.contar(origen.numero(), EstadoTransferencia.CONSUMEN_CUPO, Periodo.ultimaHora(reloj));
    }

    @Override
    public Pagina<Transferencia> consultar(ConsultarHistorialTransferencias.Query consulta) {
        Paginacion pagina = new Paginacion(consulta.page(), consulta.size());
        if (consulta.desde() != null && consulta.hasta() != null && consulta.hasta().isBefore(consulta.desde())) {
            throw new DatosInvalidosException("hasta", "La fecha 'hasta' no puede ser anterior a 'desde'");
        }
        FiltroTransferencias filtro = new FiltroTransferencias(
                numeroOpcional(consulta.origen()), numeroOpcional(consulta.destino()),
                consulta.desde(), consulta.hasta(), consulta.estado());
        return transferencias.buscar(filtro, pagina.page(), pagina.size());
    }

    private static NumeroCuenta numeroOpcional(String valor) {
        return valor == null ? null : NumeroCuenta.de(valor);
    }
}
