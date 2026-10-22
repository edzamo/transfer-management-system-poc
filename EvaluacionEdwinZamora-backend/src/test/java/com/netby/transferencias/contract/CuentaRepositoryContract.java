package com.netby.transferencias.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netby.transferencias.application.out.CuentaRepository;
import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.EstadoCuenta;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.cuenta.TipoCuenta;
import com.netby.transferencias.domain.cuenta.Titular;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.CuentaDuplicadaException;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Contrato que cumplen tanto el fake en memoria como el adaptador de persistencia real. */
public abstract class CuentaRepositoryContract {

    protected abstract CuentaRepository repositorio();

    /** El bloqueo pesimista solo es válido dentro de una transacción. */
    protected <T> T enTransaccion(Supplier<T> accion) {
        return accion.get();
    }

    protected static NumeroCuenta numeroUnico() {
        return NumeroCuenta.de(String.format("%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L)));
    }

    protected static Dinero d(String v) {
        return Dinero.de(new BigDecimal(v));
    }

    protected Cuenta nueva(NumeroCuenta numero, String saldo, TipoCuenta tipo, EstadoCuenta estado) {
        return repositorio().guardar(Cuenta.abrir(numero, Titular.de("Ana Pérez"), tipo, d(saldo), estado));
    }

    @Test
    void debe_guardar_asignando_id_y_recuperar_por_numero_e_id() {
        NumeroCuenta numero = numeroUnico();
        Cuenta guardada = nueva(numero, "123.45", TipoCuenta.AHORRO, EstadoCuenta.ACTIVA);

        assertThat(guardada.id()).isNotNull();
        assertThat(repositorio().buscarPorNumero(numero)).hasValueSatisfying(c -> {
            assertThat(c.id()).isEqualTo(guardada.id());
            assertThat(c.titular().valor()).isEqualTo("Ana Pérez");
            assertThat(c.saldo()).isEqualTo(d("123.45"));
        });
        assertThat(repositorio().buscarPorId(guardada.id())).isPresent();
    }

    @ParameterizedTest
    @EnumSource(TipoCuenta.class)
    void debe_conservar_cada_tipo_en_ida_y_vuelta(TipoCuenta tipo) {
        NumeroCuenta numero = numeroUnico();
        nueva(numero, "1", tipo, EstadoCuenta.ACTIVA);
        assertThat(repositorio().buscarPorNumero(numero).orElseThrow().tipo()).isEqualTo(tipo);
    }

    @ParameterizedTest
    @EnumSource(EstadoCuenta.class)
    void debe_conservar_cada_estado_en_ida_y_vuelta(EstadoCuenta estado) {
        NumeroCuenta numero = numeroUnico();
        nueva(numero, "1", TipoCuenta.CORRIENTE, estado);
        assertThat(repositorio().buscarPorNumero(numero).orElseThrow().estado()).isEqualTo(estado);
    }

    @Test
    void debe_reportar_existencia_y_ausencia() {
        NumeroCuenta numero = numeroUnico();
        assertThat(repositorio().existePorNumero(numero)).isFalse();
        assertThat(repositorio().buscarPorNumero(numero)).isEmpty();
        assertThat(repositorio().buscarPorId(-1L)).isEmpty();
        nueva(numero, "1", TipoCuenta.AHORRO, EstadoCuenta.ACTIVA);
        assertThat(repositorio().existePorNumero(numero)).isTrue();
    }

    @Test
    void debe_actualizar_el_saldo_de_una_cuenta_existente() {
        NumeroCuenta numero = numeroUnico();
        Cuenta c = nueva(numero, "100", TipoCuenta.AHORRO, EstadoCuenta.ACTIVA);
        c.debitar(d("30.10"));

        repositorio().guardar(c);

        assertThat(repositorio().buscarPorNumero(numero).orElseThrow().saldo()).isEqualTo(d("69.90"));
    }

    @Test
    void debe_traducir_numero_duplicado_a_cuenta_duplicada() {
        NumeroCuenta numero = numeroUnico();
        nueva(numero, "1", TipoCuenta.AHORRO, EstadoCuenta.ACTIVA);

        assertThatThrownBy(() -> nueva(numero, "2", TipoCuenta.CORRIENTE, EstadoCuenta.ACTIVA))
                .isInstanceOf(CuentaDuplicadaException.class);
    }

    @Test
    void debe_devolver_la_cuenta_bloqueada_para_actualizar_dentro_de_una_transaccion() {
        NumeroCuenta numero = numeroUnico();
        nueva(numero, "5", TipoCuenta.AHORRO, EstadoCuenta.ACTIVA);

        var bloqueada = enTransaccion(() -> repositorio().buscarPorNumeroParaActualizar(numero));

        assertThat(bloqueada).hasValueSatisfying(c -> assertThat(c.saldo()).isEqualTo(d("5")));
        assertThat(enTransaccion(() -> repositorio().buscarPorNumeroParaActualizar(numeroUnico()))).isEmpty();
    }

    @Test
    void debe_listar_paginado_con_total() {
        for (int i = 0; i < 3; i++) {
            nueva(numeroUnico(), "1", TipoCuenta.AHORRO, EstadoCuenta.ACTIVA);
        }

        var pagina = repositorio().listar(0, 2);

        assertThat(pagina.contenido()).hasSize(2);
        assertThat(pagina.page()).isZero();
        assertThat(pagina.size()).isEqualTo(2);
        assertThat(pagina.totalElements()).isGreaterThanOrEqualTo(3);
    }
}
