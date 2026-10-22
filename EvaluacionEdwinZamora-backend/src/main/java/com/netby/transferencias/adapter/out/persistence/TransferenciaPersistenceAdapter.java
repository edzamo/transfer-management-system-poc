package com.netby.transferencias.adapter.out.persistence;

import com.netby.transferencias.adapter.out.persistence.entity.TransferenciaEntity;
import com.netby.transferencias.adapter.out.persistence.mapper.PersistenceMapper;
import com.netby.transferencias.application.dto.FiltroTransferencias;
import com.netby.transferencias.application.dto.Pagina;
import com.netby.transferencias.application.out.TransferenciaRepository;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import com.netby.transferencias.domain.transferencia.Periodo;
import com.netby.transferencias.domain.transferencia.Transferencia;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TransferenciaPersistenceAdapter implements TransferenciaRepository {

    private static final Logger LOG = Logger.getLogger(TransferenciaPersistenceAdapter.class);
    private static final String CRITERIO_PERIODO =
            "cuentaOrigen = ?1 and estado in ?2 and fecha >= ?3 and fecha < ?4";

    private final TransferenciaPanacheRepository repositorio;
    private final PersistenceMapper mapper;

    @Inject
    public TransferenciaPersistenceAdapter(TransferenciaPanacheRepository repositorio, PersistenceMapper mapper) {
        this.repositorio = repositorio;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Transferencia guardar(Transferencia transferencia) {
        TransferenciaEntity entity = mapper.toEntity(transferencia);
        repositorio.persist(entity);
        LOG.infof("Transferencia id=%d estado=%s origen=%s", entity.id, entity.estado,
                transferencia.cuentaOrigen().enmascarado());
        return mapper.toDomain(entity);
    }

    @Override
    public long contar(NumeroCuenta origen, Set<EstadoTransferencia> estados, Periodo periodo) {
        return repositorio.count(CRITERIO_PERIODO, origen.valor(), estados, periodo.desde(), periodo.hasta());
    }

    @Override
    public Dinero sumar(NumeroCuenta origen, Set<EstadoTransferencia> estados, Periodo periodo) {
        BigDecimal total = repositorio.getEntityManager().createQuery(
                        "select coalesce(sum(t.monto), 0) from TransferenciaEntity t where t.cuentaOrigen = ?1 "
                                + "and t.estado in ?2 and t.fecha >= ?3 and t.fecha < ?4", BigDecimal.class)
                .setParameter(1, origen.valor())
                .setParameter(2, estados)
                .setParameter(3, periodo.desde())
                .setParameter(4, periodo.hasta())
                .getSingleResult();
        return Dinero.de(total);
    }

    @Override
    public Pagina<Transferencia> buscar(FiltroTransferencias filtro, int page, int size) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        agregar(condiciones, parametros, "cuentaOrigen = ?%d", filtro.origen() == null ? null : filtro.origen().valor());
        agregar(condiciones, parametros, "cuentaDestino = ?%d", filtro.destino() == null ? null : filtro.destino().valor());
        agregar(condiciones, parametros, "fecha >= ?%d", filtro.desde());
        agregar(condiciones, parametros, "fecha <= ?%d", filtro.hasta());
        agregar(condiciones, parametros, "estado = ?%d", filtro.estado());

        String where = condiciones.isEmpty() ? "1 = 1" : String.join(" and ", condiciones);
        var consulta = repositorio.find(where + " order by fecha desc, id desc", parametros.toArray())
                .page(Page.of(page, size));
        return new Pagina<>(consulta.list().stream().map(mapper::toDomain).toList(), page, size, consulta.count());
    }

    private static void agregar(List<String> condiciones, List<Object> parametros, String plantilla, Object valor) {
        if (valor != null) {
            parametros.add(valor);
            condiciones.add(plantilla.formatted(parametros.size()));
        }
    }
}
