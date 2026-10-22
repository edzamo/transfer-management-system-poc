package com.netby.transferencias.domain.cuenta;

import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.CuentaInactivaException;
import com.netby.transferencias.domain.error.DatosInvalidosException;
import com.netby.transferencias.domain.error.SaldoInsuficienteException;
import java.util.Objects;

/** Cuenta bancaria. Invariante: saldo >= 0 (garantizado por Dinero y por debitar). */
public final class Cuenta {

    private final Long id;
    private final NumeroCuenta numero;
    private final Titular titular;
    private final TipoCuenta tipo;
    private final EstadoCuenta estado;
    private Dinero saldo;

    private Cuenta(Long id, NumeroCuenta numero, Titular titular, TipoCuenta tipo, Dinero saldo, EstadoCuenta estado) {
        this.id = id;
        this.numero = requerido(numero, "numero");
        this.titular = requerido(titular, "titular");
        this.tipo = requerido(tipo, "tipo");
        this.saldo = requerido(saldo, "saldo");
        this.estado = requerido(estado, "estado");
    }

    public static Cuenta abrir(NumeroCuenta numero, Titular titular, TipoCuenta tipo, Dinero saldoInicial,
                               EstadoCuenta estado) {
        return new Cuenta(null, numero, titular, tipo, saldoInicial, estado);
    }

    public static Cuenta reconstruir(Long id, NumeroCuenta numero, Titular titular, TipoCuenta tipo, Dinero saldo,
                                     EstadoCuenta estado) {
        return new Cuenta(Objects.requireNonNull(id, "id"), numero, titular, tipo, saldo, estado);
    }

    private static <T> T requerido(T valor, String campo) {
        if (valor == null) {
            throw new DatosInvalidosException(campo, "El campo " + campo + " es obligatorio");
        }
        return valor;
    }

    public boolean estaActiva() {
        return estado == EstadoCuenta.ACTIVA;
    }

    public void verificarActiva() {
        if (!estaActiva()) {
            throw new CuentaInactivaException("La cuenta " + numero.enmascarado() + " está inactiva");
        }
    }

    public boolean tieneSaldoPara(Dinero monto) {
        return !monto.esMayorQue(saldo);
    }

    public void verificarSaldoPara(Dinero monto) {
        if (!tieneSaldoPara(monto)) {
            throw new SaldoInsuficienteException("Saldo insuficiente en la cuenta " + numero.enmascarado());
        }
    }

    public void debitar(Dinero monto) {
        verificarSaldoPara(monto);
        saldo = saldo.restar(monto);
    }

    public void acreditar(Dinero monto) {
        saldo = saldo.sumar(monto);
    }

    public Long id() { return id; }

    public NumeroCuenta numero() { return numero; }

    public Titular titular() { return titular; }

    public TipoCuenta tipo() { return tipo; }

    public Dinero saldo() { return saldo; }

    public EstadoCuenta estado() { return estado; }

    @Override
    public String toString() {
        return "Cuenta[id=" + id + ", numero=" + numero.enmascarado() + ", estado=" + estado + "]";
    }
}
