package com.netby.transferencias.application.fake;

import com.netby.transferencias.application.in.FiltroTransferencias;
import com.netby.transferencias.domain.shared.Pagina;
import com.netby.transferencias.application.out.TransferenciaRepository;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import com.netby.transferencias.domain.transferencia.Periodo;
import com.netby.transferencias.domain.transferencia.Transferencia;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class InMemoryTransferenciaRepository implements TransferenciaRepository {

    private final List<Transferencia> almacen = new ArrayList<>();
    private final AtomicLong secuencia = new AtomicLong();
    private final List<String> eventos;

    public InMemoryTransferenciaRepository() {
        this(new ArrayList<>());
    }

    public InMemoryTransferenciaRepository(List<String> eventos) {
        this.eventos = eventos;
    }

    @Override
    public synchronized Transferencia guardar(Transferencia t) {
        Transferencia guardada = Transferencia.reconstruir(secuencia.incrementAndGet(), t.cuentaOrigen(),
                t.cuentaDestino(), t.monto(), t.estado(), t.motivo(), t.fecha());
        almacen.add(guardada);
        eventos.add("guardar:" + t.estado());
        return guardada;
    }

    @Override
    public synchronized long contar(NumeroCuenta origen, Set<EstadoTransferencia> estados, Periodo periodo) {
        eventos.add("contar");
        return filtrar(origen, estados, periodo).count();
    }

    @Override
    public synchronized Dinero sumar(NumeroCuenta origen, Set<EstadoTransferencia> estados, Periodo periodo) {
        eventos.add("sumar");
        return filtrar(origen, estados, periodo).map(Transferencia::monto).reduce(Dinero.CERO, Dinero::sumar);
    }

    @Override
    public synchronized Pagina<Transferencia> buscar(FiltroTransferencias f, int page, int size) {
        Predicate<Transferencia> cumple = t ->
                (f.origen() == null || t.cuentaOrigen().equals(f.origen()))
                && (f.destino() == null || t.cuentaDestino().equals(f.destino()))
                && (f.desde() == null || !t.fecha().isBefore(f.desde()))
                && (f.hasta() == null || !t.fecha().isAfter(f.hasta()))
                && (f.estado() == null || t.estado() == f.estado());
        List<Transferencia> todas = almacen.stream().filter(cumple)
                .sorted(Comparator.comparing(Transferencia::fecha).thenComparing(Transferencia::id).reversed())
                .toList();
        int desde = Math.min(page * size, todas.size());
        return new Pagina<>(todas.subList(desde, Math.min(desde + size, todas.size())), page, size, todas.size());
    }

    public synchronized List<Transferencia> todas() {
        return List.copyOf(almacen);
    }

    private java.util.stream.Stream<Transferencia> filtrar(NumeroCuenta origen, Set<EstadoTransferencia> estados,
                                                           Periodo periodo) {
        return almacen.stream().filter(t -> t.cuentaOrigen().equals(origen) && estados.contains(t.estado())
                && periodo.contiene(t.fecha()));
    }
}
