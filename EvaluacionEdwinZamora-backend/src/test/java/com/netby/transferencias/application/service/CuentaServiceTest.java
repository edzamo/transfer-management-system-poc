package com.netby.transferencias.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netby.transferencias.application.fake.InMemoryCuentaRepository;
import com.netby.transferencias.application.fake.InMemoryTransferenciaRepository;
import com.netby.transferencias.application.in.ListarCuentas;
import com.netby.transferencias.application.in.ObtenerResumenCuenta;
import com.netby.transferencias.application.in.RegistrarCuenta;
import com.netby.transferencias.application.out.CuentaRepository;
import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.EstadoCuenta;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.cuenta.TipoCuenta;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.CuentaDuplicadaException;
import com.netby.transferencias.domain.error.CuentaNoEncontradaException;
import com.netby.transferencias.domain.error.DatosInvalidosException;
import com.netby.transferencias.domain.transferencia.Transferencia;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CuentaServiceTest {

    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-10-22T15:00:00Z"), ZoneId.of("America/Guayaquil"));

    private InMemoryCuentaRepository cuentas;
    private InMemoryTransferenciaRepository transferencias;
    private CuentaService servicio;

    @BeforeEach
    void preparar() {
        cuentas = new InMemoryCuentaRepository();
        transferencias = new InMemoryTransferenciaRepository();
        servicio = new CuentaService(cuentas, transferencias, RELOJ);
    }

    private static RegistrarCuenta.Command comando(String numero, String saldo) {
        return new RegistrarCuenta.Command(numero, "Ana Pérez", TipoCuenta.AHORRO, new BigDecimal(saldo),
                EstadoCuenta.ACTIVA);
    }

    private static Dinero d(String v) {
        return Dinero.de(new BigDecimal(v));
    }

    @Test
    void debe_registrar_cuenta_y_devolverla_con_id() {
        Cuenta c = servicio.registrar(comando("0000000001", "100"));
        assertThat(c.id()).isNotNull();
        assertThat(c.saldo()).isEqualTo(d("100"));
        assertThat(cuentas.existePorNumero(NumeroCuenta.de("0000000001"))).isTrue();
    }

    @Test
    void debe_rechazar_numero_duplicado_sin_guardar() {
        CuentaRepository repo = mock(CuentaRepository.class);
        when(repo.existePorNumero(NumeroCuenta.de("0000000001"))).thenReturn(true);
        CuentaService s = new CuentaService(repo, transferencias, RELOJ);

        assertThatThrownBy(() -> s.registrar(comando("0000000001", "1"))).isInstanceOf(CuentaDuplicadaException.class);
        verify(repo, never()).guardar(any());
    }

    @Test
    void debe_rechazar_datos_invalidos_con_error_de_validacion() {
        assertThatThrownBy(() -> servicio.registrar(comando("123", "1"))).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.registrar(comando("0000000001", "-1"))).isInstanceOf(DatosInvalidosException.class);
        assertThat(cuentas.listar(0, 10).contenido()).isEmpty();
    }

    @Test
    void debe_listar_paginado() {
        for (int i = 1; i <= 3; i++) {
            servicio.registrar(comando("000000000" + i, "10"));
        }
        var pagina = servicio.listar(new ListarCuentas.Query(1, 2));
        assertThat(pagina.contenido()).hasSize(1);
        assertThat(pagina.totalElements()).isEqualTo(3);
        assertThat(pagina.totalPages()).isEqualTo(2);
    }

    @Test
    void debe_rechazar_paginacion_invalida() {
        assertThatThrownBy(() -> servicio.listar(new ListarCuentas.Query(-1, 10))).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.listar(new ListarCuentas.Query(0, 0))).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.listar(new ListarCuentas.Query(0, 201))).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_aceptar_tamano_maximo_200_y_rechazar_201() {
        assertThat(servicio.listar(new ListarCuentas.Query(0, 200)).size()).isEqualTo(200);
        assertThatThrownBy(() -> servicio.listar(new ListarCuentas.Query(0, 201))).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_resumir_solo_el_dia_calendario_local_con_estados_que_consumen_cupo() {
        Cuenta o = servicio.registrar(comando("0000000001", "1000"));
        NumeroCuenta origen = o.numero();
        NumeroCuenta destino = NumeroCuenta.de("0000000002");
        Instant hoy = Instant.parse("2026-10-22T10:00:00Z");     // 05:00 local, hoy
        Instant ayer = Instant.parse("2026-10-22T04:00:00Z");    // 23:00 local de ayer
        transferencias.guardar(Transferencia.completada(origen, destino, d("100"), hoy));
        transferencias.guardar(Transferencia.enRevision(origen, destino, d("6000"), hoy));
        transferencias.guardar(Transferencia.rechazada(origen, destino, d("50"), hoy, "x"));
        transferencias.guardar(Transferencia.completada(origen, destino, d("30"), ayer));

        var resumen = servicio.obtener(new ObtenerResumenCuenta.Query(o.id()));

        assertThat(resumen.saldoActual()).isEqualTo(d("1000"));
        assertThat(resumen.totalTransferidoHoy()).isEqualTo(d("6100"));
        assertThat(resumen.cantidadTransferenciasHoy()).isEqualTo(2);
        assertThat(resumen.cantidadSospechosasHoy()).isEqualTo(1);
    }

    @Test
    void debe_dar_404_al_resumir_cuenta_inexistente() {
        assertThatThrownBy(() -> servicio.obtener(new ObtenerResumenCuenta.Query(99L)))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }
}
