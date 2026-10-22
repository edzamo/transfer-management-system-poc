package com.netby.transferencias.adapter.in.rest.dto;

import java.util.List;

public record PaginaResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
