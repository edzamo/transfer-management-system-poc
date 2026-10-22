package com.netby.transferencias.adapter.out.persistence;

import com.netby.transferencias.adapter.out.persistence.entity.CuentaEntity;
import com.netby.transferencias.adapter.out.persistence.mapper.PersistenceMapper;
import com.netby.transferencias.application.dto.Pagina;
import com.netby.transferencias.application.out.CuentaRepository;
import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.error.CuentaBloqueadaException;
import com.netby.transferencias.domain.error.CuentaDuplicadaException;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CuentaPersistenceAdapter implements CuentaRepository {

    private static final Logger LOG = Logger.getLogger(CuentaPersistenceAdapter.class);

    private final CuentaPanacheRepository repositorio;
    private final PersistenceMapper mapper;

    @Inject
    public CuentaPersistenceAdapter(CuentaPanacheRepository repositorio, PersistenceMapper mapper) {
        this.repositorio = repositorio;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Cuenta guardar(Cuenta cuenta) {
        if (cuenta.id() != null) {
            CuentaEntity existente = repositorio.findByIdOptional(cuenta.id()).orElseThrow(
                    () -> new IllegalStateException("La cuenta " + cuenta.id() + " no existe en persistencia"));
            mapper.actualizar(cuenta, existente);
            return mapper.toDomain(existente);
        }
        CuentaEntity entity = mapper.toEntity(cuenta);
        try {
            repositorio.persistAndFlush(entity);
        } catch (PersistenceException e) {
            if (causadoPor(e, ConstraintViolationException.class)) {
                throw new CuentaDuplicadaException("Ya existe una cuenta con el número " + cuenta.numero().enmascarado());
            }
            throw e;
        }
        LOG.infof("Cuenta registrada id=%d numero=%s", entity.id, cuenta.numero().enmascarado());
        return mapper.toDomain(entity);
    }

    @Override
    public boolean existePorNumero(NumeroCuenta numero) {
        return repositorio.count("numero", numero.valor()) > 0;
    }

    @Override
    public Optional<Cuenta> buscarPorNumero(NumeroCuenta numero) {
        return repositorio.find("numero", numero.valor()).firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public Optional<Cuenta> buscarPorId(Long id) {
        return repositorio.findByIdOptional(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    public Optional<Cuenta> buscarPorNumeroParaActualizar(NumeroCuenta numero) {
        try {
            return repositorio.find("numero", numero.valor()).withLock(LockModeType.PESSIMISTIC_WRITE)
                    .firstResultOptional().map(mapper::toDomain);
        } catch (PersistenceException e) {
            if (esFalloDeBloqueo(e)) {
                LOG.warnf("No se obtuvo el bloqueo de la cuenta %s", numero.enmascarado());
                throw new CuentaBloqueadaException(
                        "La cuenta " + numero.enmascarado() + " está siendo modificada por otra operación; reintente");
            }
            throw e;
        }
    }

    @Override
    public Pagina<Cuenta> listar(int page, int size) {
        var consulta = repositorio.findAll(Sort.by("id")).page(Page.of(page, size));
        return new Pagina<>(consulta.list().stream().map(mapper::toDomain).toList(), page, size, consulta.count());
    }

    private static boolean esFalloDeBloqueo(PersistenceException e) {
        return causadoPor(e, LockTimeoutException.class) || causadoPor(e, LockAcquisitionException.class)
                || e instanceof PessimisticLockException
                || e instanceof jakarta.persistence.LockTimeoutException;
    }

    private static boolean causadoPor(Throwable error, Class<? extends Throwable> tipo) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (tipo.isInstance(t)) {
                return true;
            }
        }
        return false;
    }
}
