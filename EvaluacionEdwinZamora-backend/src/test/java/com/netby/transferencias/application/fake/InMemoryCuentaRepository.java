package com.netby.transferencias.application.fake;

import com.netby.transferencias.domain.shared.Pagina;
import com.netby.transferencias.application.out.CuentaRepository;
import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.error.CuentaDuplicadaException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/** Fake en memoria. Guarda y entrega copias, como lo haría una base de datos. */
public class InMemoryCuentaRepository implements CuentaRepository {

    private final Map<Long, Cuenta> almacen = new TreeMap<>();
    private final AtomicLong secuencia = new AtomicLong();
    private final List<NumeroCuenta> ordenDeBloqueos = new ArrayList<>();
    private final List<String> eventos;

    public InMemoryCuentaRepository() {
        this(new ArrayList<>());
    }

    public InMemoryCuentaRepository(List<String> eventos) {
        this.eventos = eventos;
    }

    @Override
    public synchronized Cuenta guardar(Cuenta cuenta) {
        boolean duplicada = almacen.values().stream()
                .anyMatch(c -> c.numero().equals(cuenta.numero()) && !c.id().equals(cuenta.id()));
        if (duplicada) {
            throw new CuentaDuplicadaException("Ya existe una cuenta con ese número");
        }
        Long id = cuenta.id() != null ? cuenta.id() : secuencia.incrementAndGet();
        almacen.put(id, copia(id, cuenta));
        return copia(id, cuenta);
    }

    @Override
    public synchronized boolean existePorNumero(NumeroCuenta numero) {
        return almacen.values().stream().anyMatch(c -> c.numero().equals(numero));
    }

    @Override
    public synchronized Optional<Cuenta> buscarPorNumero(NumeroCuenta numero) {
        return almacen.values().stream().filter(c -> c.numero().equals(numero)).findFirst().map(c -> copia(c.id(), c));
    }

    @Override
    public synchronized Optional<Cuenta> buscarPorId(Long id) {
        return Optional.ofNullable(almacen.get(id)).map(c -> copia(id, c));
    }

    @Override
    public synchronized Optional<Cuenta> buscarPorNumeroParaActualizar(NumeroCuenta numero) {
        ordenDeBloqueos.add(numero);
        eventos.add("bloquear:" + numero.valor());
        return buscarPorNumero(numero);
    }

    @Override
    public synchronized Pagina<Cuenta> listar(int page, int size) {
        List<Cuenta> todas = new ArrayList<>(almacen.values());
        int desde = Math.min(page * size, todas.size());
        int hasta = Math.min(desde + size, todas.size());
        return new Pagina<>(todas.subList(desde, hasta).stream().map(c -> copia(c.id(), c)).toList(),
                page, size, todas.size());
    }

    public List<NumeroCuenta> ordenDeBloqueos() {
        return List.copyOf(ordenDeBloqueos);
    }

    private static Cuenta copia(Long id, Cuenta c) {
        return Cuenta.reconstruir(id, c.numero(), c.titular(), c.tipo(), c.saldo(), c.estado());
    }
}
