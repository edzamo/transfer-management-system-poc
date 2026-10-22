package com.netby.transferencias.application.out;

import com.netby.transferencias.application.dto.FiltroTransferencias;
import com.netby.transferencias.application.dto.Pagina;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import com.netby.transferencias.domain.transferencia.Periodo;
import com.netby.transferencias.domain.transferencia.Transferencia;
import java.util.Set;

public interface TransferenciaRepository {

    Transferencia guardar(Transferencia transferencia);

    long contar(NumeroCuenta origen, Set<EstadoTransferencia> estados, Periodo periodo);

    Dinero sumar(NumeroCuenta origen, Set<EstadoTransferencia> estados, Periodo periodo);

    /** Orden: fecha descendente. */
    Pagina<Transferencia> buscar(FiltroTransferencias filtro, int page, int size);
}
