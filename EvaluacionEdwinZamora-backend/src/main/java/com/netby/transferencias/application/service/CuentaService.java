package com.netby.transferencias.application.service;

import com.netby.transferencias.domain.shared.Pagina;
import com.netby.transferencias.domain.shared.Paginacion;
import com.netby.transferencias.application.in.ResumenCuenta;
import com.netby.transferencias.application.in.ListarCuentas;
import com.netby.transferencias.application.in.ObtenerResumenCuenta;
import com.netby.transferencias.application.in.RegistrarCuenta;
import com.netby.transferencias.application.out.CuentaRepository;
import com.netby.transferencias.application.out.TransferenciaRepository;
import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.cuenta.Titular;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.CuentaDuplicadaException;
import com.netby.transferencias.domain.error.CuentaNoEncontradaException;
import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import com.netby.transferencias.domain.transferencia.Periodo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.util.Set;

@ApplicationScoped
public class CuentaService implements RegistrarCuenta, ListarCuentas, ObtenerResumenCuenta {

    private final CuentaRepository cuentas;
    private final TransferenciaRepository transferencias;
    private final Clock reloj;

    @Inject
    public CuentaService(CuentaRepository cuentas, TransferenciaRepository transferencias, Clock reloj) {
        this.cuentas = cuentas;
        this.transferencias = transferencias;
        this.reloj = reloj;
    }

    @Override
    public Cuenta registrar(RegistrarCuenta.Command comando) {
        Cuenta nueva = Cuenta.abrir(NumeroCuenta.de(comando.numero()), Titular.de(comando.titular()),
                comando.tipo(), Dinero.de(comando.saldoInicial(), "saldoInicial"), comando.estado());
        if (cuentas.existePorNumero(nueva.numero())) {
            throw new CuentaDuplicadaException("Ya existe una cuenta con el número " + nueva.numero().enmascarado());
        }
        return cuentas.guardar(nueva);
    }

    @Override
    public Pagina<Cuenta> listar(ListarCuentas.Query consulta) {
        Paginacion pagina = new Paginacion(consulta.page(), consulta.size());
        return cuentas.listar(pagina.page(), pagina.size());
    }

    @Override
    public ResumenCuenta obtener(ObtenerResumenCuenta.Query consulta) {
        Cuenta cuenta = cuentas.buscarPorId(consulta.cuentaId())
                .orElseThrow(() -> new CuentaNoEncontradaException("No existe la cuenta con id " + consulta.cuentaId()));
        Periodo hoy = Periodo.diaCalendario(reloj);
        return new ResumenCuenta(
                cuenta.id(),
                cuenta.saldo(),
                transferencias.sumar(cuenta.numero(), EstadoTransferencia.CONSUMEN_CUPO, hoy),
                transferencias.contar(cuenta.numero(), EstadoTransferencia.CONSUMEN_CUPO, hoy),
                transferencias.contar(cuenta.numero(), Set.of(EstadoTransferencia.EN_REVISION), hoy));
    }
}
