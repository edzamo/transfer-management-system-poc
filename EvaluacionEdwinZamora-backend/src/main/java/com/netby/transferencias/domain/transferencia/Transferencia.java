package com.netby.transferencias.domain.transferencia;

import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.DatosInvalidosException;
import java.time.Instant;

/** Registro inmutable de una transferencia en su estado final. Las invariantes viven en el constructor. */
public final class Transferencia {

    private final Long id;
    private final NumeroCuenta cuentaOrigen;
    private final NumeroCuenta cuentaDestino;
    private final Dinero monto;
    private final EstadoTransferencia estado;
    private final String motivo;
    private final Instant fecha;

    private Transferencia(Long id, NumeroCuenta origen, NumeroCuenta destino, Dinero monto,
                          EstadoTransferencia estado, String motivo, Instant fecha) {
        requerido(origen, "cuentaOrigen");
        requerido(destino, "cuentaDestino");
        requerido(monto, "monto");
        requerido(estado, "estado");
        requerido(fecha, "fecha");
        if (!monto.esPositivo()) {
            throw new DatosInvalidosException("monto", "El monto debe ser mayor a cero");
        }
        if (origen.equals(destino)) {
            throw new DatosInvalidosException("cuentaDestino", "La cuenta origen y destino deben ser distintas");
        }
        boolean rechazada = estado == EstadoTransferencia.RECHAZADA;
        if (rechazada && (motivo == null || motivo.isBlank())) {
            throw new DatosInvalidosException("motivo", "Una transferencia rechazada requiere motivo");
        }
        if (!rechazada && motivo != null) {
            throw new DatosInvalidosException("motivo", "Solo una transferencia rechazada lleva motivo");
        }
        this.id = id;
        this.cuentaOrigen = origen;
        this.cuentaDestino = destino;
        this.monto = monto;
        this.estado = estado;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    private static void requerido(Object valor, String campo) {
        if (valor == null) {
            throw new DatosInvalidosException(campo, "El campo " + campo + " es obligatorio");
        }
    }

    public static Transferencia completada(NumeroCuenta origen, NumeroCuenta destino, Dinero monto, Instant fecha) {
        return new Transferencia(null, origen, destino, monto, EstadoTransferencia.COMPLETADA, null, fecha);
    }

    public static Transferencia enRevision(NumeroCuenta origen, NumeroCuenta destino, Dinero monto, Instant fecha) {
        return new Transferencia(null, origen, destino, monto, EstadoTransferencia.EN_REVISION, null, fecha);
    }

    public static Transferencia rechazada(NumeroCuenta origen, NumeroCuenta destino, Dinero monto, Instant fecha,
                                          String motivo) {
        return new Transferencia(null, origen, destino, monto, EstadoTransferencia.RECHAZADA, motivo, fecha);
    }

    public static Transferencia reconstruir(Long id, NumeroCuenta origen, NumeroCuenta destino, Dinero monto,
                                            EstadoTransferencia estado, String motivo, Instant fecha) {
        requerido(id, "id");
        return new Transferencia(id, origen, destino, monto, estado, motivo, fecha);
    }

    public Long id() { return id; }

    public NumeroCuenta cuentaOrigen() { return cuentaOrigen; }

    public NumeroCuenta cuentaDestino() { return cuentaDestino; }

    public Dinero monto() { return monto; }

    public EstadoTransferencia estado() { return estado; }

    public String motivo() { return motivo; }

    public Instant fecha() { return fecha; }

    @Override
    public String toString() {
        return "Transferencia[id=" + id + ", origen=" + cuentaOrigen.enmascarado() + ", destino="
                + cuentaDestino.enmascarado() + ", estado=" + estado + "]";
    }
}
