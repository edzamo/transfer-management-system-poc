package com.netby.transferencias.domain.shared;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> contenido, int page, int size, long totalElements) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }

    public <R> Pagina<R> mapear(Function<T, R> funcion) {
        return new Pagina<>(contenido.stream().map(funcion).toList(), page, size, totalElements);
    }
}
