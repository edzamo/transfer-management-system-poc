package com.netby.transferencias.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netby.transferencias.application.in.FiltroTransferencias;
import com.netby.transferencias.application.fake.InMemoryCuentaRepository;
import com.netby.transferencias.application.fake.InMemoryTransferenciaRepository;
import com.netby.transferencias.application.in.ConsultarHistorialTransferencias;
import com.netby.transferencias.application.in.RealizarTransferencia;
import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.EstadoCuenta;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.cuenta.TipoCuenta;
import com.netby.transferencias.domain.cuenta.Titular;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.CuentaInactivaException;
import com.netby.transferencias.domain.error.CuentaNoEncontradaException;
import com.netby.transferencias.domain.error.DatosInvalidosException;
import com.netby.transferencias.domain.error.LimiteDiarioExcedidoException;
import com.netby.transferencias.domain.error.SaldoInsuficienteException;
import com.netby.transferencias.domain.transferencia.DeteccionSospechosaService;
import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import com.netby.transferencias.domain.transferencia.LimiteDiarioService;
import com.netby.transferencias.domain.transferencia.Transferencia;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransferenciaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-22T15:00:00Z"); // 10:00 en Guayaquil
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneId.of("America/Guayaquil"));
    private static final NumeroCuenta A = NumeroCuenta.de("0000000001");
    private static final NumeroCuenta B = NumeroCuenta.de("0000000002");

    private List<String> eventos;
    private InMemoryCuentaRepository cuentas;
    private InMemoryTransferenciaRepository transferencias;
    private TransferenciaService servicio;

    @BeforeEach
    void preparar() {
        eventos = new ArrayList<>();
        cuentas = new InMemoryCuentaRepository(eventos);
        transferencias = new InMemoryTransferenciaRepository(eventos);
        servicio = new TransferenciaService(cuentas, transferencias, new DeteccionSospechosaService(),
                new LimiteDiarioService(), RELOJ);
    }

    private static Dinero d(String v) {
        return Dinero.de(new BigDecimal(v));
    }

    private void cuenta(NumeroCuenta numero, String saldo, EstadoCuenta estado) {
        cuentas.guardar(Cuenta.abrir(numero, Titular.de("T"), TipoCuenta.AHORRO, d(saldo), estado));
    }

    private Dinero saldo(NumeroCuenta numero) {
        return cuentas.buscarPorNumero(numero).orElseThrow().saldo();
    }

    private Transferencia transferir(NumeroCuenta o, NumeroCuenta dst, String monto) {
        return servicio.realizar(new RealizarTransferencia.Command(o.valor(), dst.valor(), new BigDecimal(monto)));
    }

    private void previa(EstadoTransferencia estado, String monto, Instant fecha) {
        Transferencia t = switch (estado) {
            case COMPLETADA -> Transferencia.completada(A, B, d(monto), fecha);
            case EN_REVISION -> Transferencia.enRevision(A, B, d(monto), fecha);
            case RECHAZADA -> Transferencia.rechazada(A, B, d(monto), fecha, "previa");
        };
        transferencias.guardar(t);
    }

    @Test
    void debe_completar_y_mover_ambos_saldos() {
        cuenta(A, "1000", EstadoCuenta.ACTIVA);
        cuenta(B, "50", EstadoCuenta.ACTIVA);

        Transferencia t = transferir(A, B, "100.50");

        assertThat(t.estado()).isEqualTo(EstadoTransferencia.COMPLETADA);
        assertThat(t.id()).isNotNull();
        assertThat(t.fecha()).isEqualTo(AHORA);
        assertThat(saldo(A)).isEqualTo(d("899.50"));
        assertThat(saldo(B)).isEqualTo(d("150.50"));
        assertThat(transferencias.todas()).hasSize(1);
    }

    @Test
    void debe_dar_400_sin_tocar_cuentas_ni_persistir_cuando_los_datos_son_invalidos() {
        cuenta(A, "1000", EstadoCuenta.ACTIVA);
        cuenta(B, "50", EstadoCuenta.ACTIVA);

        for (var cmd : List.of(
                new RealizarTransferencia.Command(A.valor(), B.valor(), BigDecimal.ZERO),
                new RealizarTransferencia.Command(A.valor(), B.valor(), new BigDecimal("1.001")),
                new RealizarTransferencia.Command(A.valor(), B.valor(), new BigDecimal("-5")),
                new RealizarTransferencia.Command(A.valor(), B.valor(), null),
                new RealizarTransferencia.Command(A.valor(), A.valor(), BigDecimal.TEN),
                new RealizarTransferencia.Command("123", B.valor(), BigDecimal.TEN))) {
            assertThatThrownBy(() -> servicio.realizar(cmd)).isInstanceOf(DatosInvalidosException.class);
        }
        assertThat(cuentas.ordenDeBloqueos()).isEmpty();
        assertThat(transferencias.todas()).isEmpty();
        assertThat(saldo(A)).isEqualTo(d("1000"));
    }

    @Test
    void debe_dar_404_sin_persistir_cuando_el_origen_o_el_destino_no_existen() {
        cuenta(A, "1000", EstadoCuenta.ACTIVA);

        assertThatThrownBy(() -> transferir(A, B, "10")).isInstanceOf(CuentaNoEncontradaException.class);
        assertThatThrownBy(() -> transferir(B, A, "10")).isInstanceOf(CuentaNoEncontradaException.class);

        assertThat(transferencias.todas()).isEmpty();
        assertThat(saldo(A)).isEqualTo(d("1000"));
    }

    @Test
    void debe_bloquear_cuentas_en_orden_ascendente_sin_importar_el_sentido_de_la_transferencia() {
        cuenta(A, "1000", EstadoCuenta.ACTIVA);
        cuenta(B, "1000", EstadoCuenta.ACTIVA);

        transferir(A, B, "1");
        transferir(B, A, "1");

        assertThat(cuentas.ordenDeBloqueos()).containsExactly(A, B, A, B);
    }

    @Test
    void debe_rechazar_por_origen_inactivo_persistiendo_rechazada_con_motivo_y_saldos_intactos() {
        cuenta(A, "1000", EstadoCuenta.INACTIVA);
        cuenta(B, "50", EstadoCuenta.ACTIVA);

        assertThatThrownBy(() -> transferir(A, B, "10")).isInstanceOf(CuentaInactivaException.class);

        assertRechazadaUnica("inactiva");
        assertThat(saldo(A)).isEqualTo(d("1000"));
        assertThat(saldo(B)).isEqualTo(d("50"));
    }

    @Test
    void debe_rechazar_por_destino_inactivo() {
        cuenta(A, "1000", EstadoCuenta.ACTIVA);
        cuenta(B, "50", EstadoCuenta.INACTIVA);

        assertThatThrownBy(() -> transferir(A, B, "10")).isInstanceOf(CuentaInactivaException.class);

        assertRechazadaUnica("inactiva");
        assertThat(saldo(A)).isEqualTo(d("1000"));
    }

    @Test
    void debe_rechazar_por_saldo_insuficiente() {
        cuenta(A, "10", EstadoCuenta.ACTIVA);
        cuenta(B, "50", EstadoCuenta.ACTIVA);

        assertThatThrownBy(() -> transferir(A, B, "10.01")).isInstanceOf(SaldoInsuficienteException.class);

        assertRechazadaUnica("Saldo insuficiente");
        assertThat(saldo(A)).isEqualTo(d("10"));
        assertThat(saldo(B)).isEqualTo(d("50"));
    }

    @Test
    void debe_rechazar_por_limite_diario_informando_el_restante() {
        cuenta(A, "50000", EstadoCuenta.ACTIVA);
        cuenta(B, "0", EstadoCuenta.ACTIVA);
        previa(EstadoTransferencia.COMPLETADA, "4000", AHORA.minusSeconds(7200));
        previa(EstadoTransferencia.EN_REVISION, "5500", AHORA.minusSeconds(7300)); // D1: cuenta para el límite

        assertThatThrownBy(() -> transferir(A, B, "500.01"))
                .isInstanceOfSatisfying(LimiteDiarioExcedidoException.class,
                        e -> assertThat(e.montoRestante()).isEqualByComparingTo("500.00"));

        assertThat(transferencias.todas()).hasSize(3);
        assertThat(transferencias.todas().get(2).estado()).isEqualTo(EstadoTransferencia.RECHAZADA);
        assertThat(saldo(A)).isEqualTo(d("50000"));
    }

    @Test
    void debe_permitir_llegar_exactamente_al_limite_diario() {
        cuenta(A, "50000", EstadoCuenta.ACTIVA);
        cuenta(B, "0", EstadoCuenta.ACTIVA);
        previa(EstadoTransferencia.COMPLETADA, "5000", AHORA.minusSeconds(7200));
        previa(EstadoTransferencia.COMPLETADA, "4000", AHORA.minusSeconds(7300));

        assertThat(transferir(A, B, "1000").estado()).isEqualTo(EstadoTransferencia.COMPLETADA);
    }

    @Test
    void no_debe_contar_rechazadas_ni_dias_anteriores_para_el_limite() {
        cuenta(A, "50000", EstadoCuenta.ACTIVA);
        cuenta(B, "0", EstadoCuenta.ACTIVA);
        previa(EstadoTransferencia.RECHAZADA, "9000", AHORA.minusSeconds(7200));
        previa(EstadoTransferencia.COMPLETADA, "9000", Instant.parse("2026-10-22T04:59:59Z")); // ayer, 23:59 local

        assertThat(transferir(A, B, "5000").estado()).isEqualTo(EstadoTransferencia.COMPLETADA);
    }

    @Test
    void debe_marcar_en_revision_y_mover_saldos_cuando_el_monto_supera_5000() {
        cuenta(A, "9000", EstadoCuenta.ACTIVA);
        cuenta(B, "0", EstadoCuenta.ACTIVA);

        Transferencia t = transferir(A, B, "5000.01");

        assertThat(t.estado()).isEqualTo(EstadoTransferencia.EN_REVISION);
        assertThat(saldo(A)).isEqualTo(d("3999.99"));
        assertThat(saldo(B)).isEqualTo(d("5000.01"));
    }

    @Test
    void no_debe_marcar_sospechosa_con_monto_de_5000_exacto() {
        cuenta(A, "9000", EstadoCuenta.ACTIVA);
        cuenta(B, "0", EstadoCuenta.ACTIVA);

        assertThat(transferir(A, B, "5000").estado()).isEqualTo(EstadoTransferencia.COMPLETADA);
    }

    @Test
    void debe_marcar_en_revision_la_sexta_transferencia_en_una_hora() {
        cuenta(A, "9000", EstadoCuenta.ACTIVA);
        cuenta(B, "0", EstadoCuenta.ACTIVA);
        for (int i = 0; i < 5; i++) {
            assertThat(transferir(A, B, "1").estado()).isEqualTo(EstadoTransferencia.COMPLETADA);
        }

        assertThat(transferir(A, B, "1").estado()).isEqualTo(EstadoTransferencia.EN_REVISION);
    }

    @Test
    void no_debe_contar_previas_viejas_ni_rechazadas_para_la_sospecha() {
        cuenta(A, "9000", EstadoCuenta.ACTIVA);
        cuenta(B, "0", EstadoCuenta.ACTIVA);
        previa(EstadoTransferencia.COMPLETADA, "1", AHORA.minusSeconds(3601));
        previa(EstadoTransferencia.COMPLETADA, "1", AHORA.minusSeconds(4000));
        previa(EstadoTransferencia.RECHAZADA, "1", AHORA.minusSeconds(60));
        previa(EstadoTransferencia.RECHAZADA, "1", AHORA.minusSeconds(60));
        previa(EstadoTransferencia.RECHAZADA, "1", AHORA.minusSeconds(60));
        previa(EstadoTransferencia.COMPLETADA, "1", AHORA.minusSeconds(3600)); // borde: aún cuenta (1 previa)

        assertThat(transferir(A, B, "1").estado()).isEqualTo(EstadoTransferencia.COMPLETADA);
    }

    @Test
    void debe_leer_acumulado_y_conteo_solo_despues_de_bloquear_el_origen() {
        cuenta(A, "1000", EstadoCuenta.ACTIVA);
        cuenta(B, "0", EstadoCuenta.ACTIVA);
        eventos.clear();

        transferir(A, B, "10");

        assertThat(eventos).containsExactly(
                "bloquear:0000000001", "bloquear:0000000002", "sumar", "contar", "guardar:COMPLETADA");
    }

    @Test
    void debe_consultar_historial_filtrado_por_fecha_descendente_y_paginado() {
        cuenta(A, "1000", EstadoCuenta.ACTIVA);
        previa(EstadoTransferencia.COMPLETADA, "1", AHORA.minusSeconds(30));
        previa(EstadoTransferencia.RECHAZADA, "2", AHORA.minusSeconds(10));
        previa(EstadoTransferencia.COMPLETADA, "3", AHORA.minusSeconds(20));

        var todas = servicio.consultar(new ConsultarHistorialTransferencias.Query(
                null, null, null, null, null, 0, 2));
        assertThat(todas.contenido()).extracting(t -> t.monto().valor().toPlainString()).containsExactly("2.00", "3.00");
        assertThat(todas.totalElements()).isEqualTo(3);

        var rechazadas = servicio.consultar(new ConsultarHistorialTransferencias.Query(
                A.valor(), B.valor(), AHORA.minusSeconds(15), AHORA, EstadoTransferencia.RECHAZADA, 0, 10));
        assertThat(rechazadas.contenido()).hasSize(1);
    }

    @Test
    void debe_dar_400_si_el_filtro_es_invalido() {
        assertThatThrownBy(() -> servicio.consultar(new ConsultarHistorialTransferencias.Query(
                "abc", null, null, null, null, 0, 10))).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.consultar(new ConsultarHistorialTransferencias.Query(
                null, null, AHORA, AHORA.minusSeconds(1), null, 0, 10))).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.consultar(new ConsultarHistorialTransferencias.Query(
                null, null, null, null, null, 0, 0))).isInstanceOf(DatosInvalidosException.class);
    }

    private void assertRechazadaUnica(String fragmentoMotivo) {
        assertThat(transferencias.todas()).hasSize(1);
        Transferencia r = transferencias.todas().get(0);
        assertThat(r.estado()).isEqualTo(EstadoTransferencia.RECHAZADA);
        assertThat(r.motivo()).contains(fragmentoMotivo);
        assertThat(r.fecha()).isEqualTo(AHORA);
    }
}
