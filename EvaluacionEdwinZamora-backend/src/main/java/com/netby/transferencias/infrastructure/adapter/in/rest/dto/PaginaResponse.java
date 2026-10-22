package com.netby.transferencias.infrastructure.adapter.in.rest.dto;

import java.util.List;

public record PaginaResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
