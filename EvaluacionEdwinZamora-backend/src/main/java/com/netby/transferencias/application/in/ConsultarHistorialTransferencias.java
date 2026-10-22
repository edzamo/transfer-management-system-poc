package com.netby.transferencias.application.in;

import com.netby.transferencias.application.dto.Pagina;
import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import com.netby.transferencias.domain.transferencia.Transferencia;
import java.time.Instant;

public interface ConsultarHistorialTransferencias {

    Pagina<Transferencia> consultar(Query consulta);

    /** Filtros opcionales (nulos = sin filtrar); desde/hasta inclusivos. */
    record Query(String origen, String destino, Instant desde, Instant hasta, EstadoTransferencia estado,
                 int page, int size) {
    }
}
