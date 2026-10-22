package com.netby.transferencias.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.netby.transferencias.application.service.TransferenciaService;
import com.netby.transferencias.domain.error.ReglaNegocioException;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Guardarrailes ejecutables de la arquitectura hexagonal (INV-01..INV-18 verificables por máquina). */
class ArchitectureTest {

    private static final String RAIZ = "com.netby.transferencias";
    private static final String[] FRAMEWORKS = {
        "jakarta..", "javax..", "io.quarkus..", "org.hibernate..", "org.mapstruct..", "org.jboss..",
        "com.fasterxml..", "lombok..", "org.eclipse.microprofile.."};

    private static JavaClasses clases;

    @BeforeAll
    static void importar() {
        clases = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests()).importPackages(RAIZ);
        assertThat(clases.size()).as("clases importadas").isGreaterThan(50);
    }

    private static void verificar(ArchRule regla) {
        regla.check(clases);
    }

    // --- Regla de dependencia -------------------------------------------------------------------------------

    @Test
    void domain_solo_depende_de_java_y_de_si_mismo() {
        verificar(classes().that().resideInAPackage("..domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("..domain..", "java.."));
    }

    @Test
    void domain_no_usa_frameworks_ni_logging_ni_otras_capas() {
        verificar(noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat()
                .resideInAnyPackage(FRAMEWORKS));
        verificar(noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..", "org.slf4j..", "java.util.logging.."));
    }

    @Test
    void application_no_depende_de_infrastructure_y_solo_usa_cdi_y_transaccion_permitidos() {
        verificar(noClasses().that().resideInAPackage("..application..").should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure.."));
        DescribedPredicate<JavaClass> permitido = resideInAnyPackage("..domain..", "..application..", "java..")
                .or(equivalentTo(ApplicationScoped.class))
                .or(equivalentTo(Inject.class))
                .or(equivalentTo(Transactional.class))
                .or(equivalentTo(Transactional.TxType.class));
        verificar(classes().that().resideInAPackage("..application..")
                .should().onlyDependOnClassesThat(permitido));
    }

    @Test
    void application_y_domain_no_hacen_logging() {
        verificar(noClasses().that().resideInAnyPackage("..domain..", "..application..").should().dependOnClassesThat()
                .resideInAnyPackage("org.jboss.logging..", "org.slf4j..", "java.util.logging..", "java.lang.System.Logger"));
    }

    @Test
    void adapters_de_entrada_y_salida_no_se_referencian_entre_si() {
        verificar(slices().matching("..infrastructure.adapter.(*)..").should().notDependOnEachOther());
        verificar(noClasses().that().resideInAPackage("..infrastructure.adapter..").should().dependOnClassesThat()
                .resideInAPackage("..infrastructure.config.."));
    }

    @Test
    void adapters_de_entrada_dependen_de_puertos_no_de_la_implementacion() {
        verificar(noClasses().that().resideInAPackage("..infrastructure.adapter..").should().dependOnClassesThat()
                .resideInAPackage("..application.service.."));
    }

    @Test
    void no_hay_ciclos_entre_capas_ni_entre_subpaquetes_del_dominio() {
        verificar(slices().matching(RAIZ + ".(*)..").should().beFreeOfCycles());
        verificar(slices().matching(RAIZ + ".domain.(*)..").should().beFreeOfCycles());
        verificar(slices().matching(RAIZ + ".application.(*)..").should().beFreeOfCycles());
    }

    // --- Estructura raíz ------------------------------------------------------------------------------------

    @Test
    void las_unicas_raices_bajo_el_paquete_base_son_domain_application_e_infrastructure() {
        java.util.Set<String> raices = new java.util.TreeSet<>();
        for (JavaClass c : clases) {
            String resto = c.getPackageName().substring(Math.min(c.getPackageName().length(), RAIZ.length() + 1));
            raices.add(resto.isEmpty() ? "(raiz)" : resto.split("\\.")[0]);
        }
        assertThat(raices).containsExactlyInAnyOrder("domain", "application", "infrastructure");
        verificar(noClasses().should().resideInAnyPackage(RAIZ + ".adapter..", RAIZ + ".bootstrap.."));
    }

    @Test
    void infrastructure_config_solo_contiene_configuracion_de_beans() {
        verificar(classes().that().resideInAPackage("..infrastructure.config..")
                .should().haveSimpleNameEndingWith("Beans")
                .andShould().beAnnotatedWith(ApplicationScoped.class));
        verificar(noClasses().that().resideInAPackage("..infrastructure.config..").should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure.adapter.."));
    }

    @Test
    void application_y_domain_no_dependen_de_infrastructure() {
        verificar(noClasses().that().resideInAnyPackage("..domain..", "..application..").should()
                .dependOnClassesThat().resideInAPackage("..infrastructure.."));
    }

    // --- Ubicación de anotaciones ---------------------------------------------------------------------------

    @Test
    void entity_solo_en_infrastructure_adapter_out_persistence_entity() {
        verificar(classes().that().areAnnotatedWith(Entity.class).should().resideInAPackage("..infrastructure.adapter.out.persistence.entity.."));
        verificar(classes().that().resideInAPackage("..infrastructure.adapter.out.persistence.entity..")
                .should().beAnnotatedWith(Entity.class));
    }

    @Test
    void path_solo_en_infrastructure_adapter_in_rest() {
        verificar(classes().that().areAnnotatedWith(Path.class).should().resideInAPackage("..infrastructure.adapter.in.rest.."));
    }

    @Test
    void transactional_solo_en_metodos_de_application_service_o_infrastructure_adapter_out() {
        verificar(methods().that().areAnnotatedWith(Transactional.class).should().beDeclaredInClassesThat()
                .resideInAnyPackage("..application.service..", "..infrastructure.adapter.out.."));
        verificar(noClasses().should().beAnnotatedWith(Transactional.class));
    }

    @Test
    void transferir_no_revierte_ante_reglas_de_negocio_para_poder_registrar_el_rechazo() throws Exception {
        Method realizar = TransferenciaService.class.getMethod("realizar",
                com.netby.transferencias.application.in.RealizarTransferencia.Command.class);
        Transactional tx = realizar.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.dontRollbackOn()).containsExactly(ReglaNegocioException.class);
    }

    @Test
    void existe_un_unico_exception_mapper() {
        verificar(classes().that().implement(ExceptionMapper.class).should().haveSimpleName("GlobalExceptionMapper"));
        assertThat(clases.that(DescribedPredicate.describe("implementan ExceptionMapper",
                c -> c.isAssignableTo(ExceptionMapper.class) && !c.isInterface()))).hasSize(1);
        verificar(methods().that().areAnnotatedWith(org.jboss.resteasy.reactive.server.ServerExceptionMapper.class)
                .should().beDeclaredInClassesThat().haveSimpleName("GlobalExceptionMapper"));
    }

    @Test
    void los_componentes_cdi_con_constructor_unico_lo_marcan_con_inject() {
        verificar(classes().that().areAnnotatedWith(ApplicationScoped.class).or().areAnnotatedWith(Path.class)
                .and().resideInAnyPackage("..application.service..", "..infrastructure..")
                .should(new ArchCondition<JavaClass>("tener su constructor público anotado con @Inject") {
                    @Override
                    public void check(JavaClass clase, ConditionEvents eventos) {
                        boolean ok = clase.getConstructors().stream()
                                .filter(c -> !c.getRawParameterTypes().isEmpty())
                                .allMatch(c -> c.isAnnotatedWith(Inject.class));
                        if (!ok) {
                            eventos.add(SimpleConditionEvent.violated(clase, clase.getName() + " sin @Inject"));
                        }
                    }
                }));
    }

    @Test
    void domain_sigue_sin_depender_de_jakarta() {
        verificar(noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat()
                .resideInAnyPackage("jakarta..", "javax.."));
    }

    @Test
    void no_hay_inyeccion_en_campos() {
        verificar(noFields().should().beAnnotatedWith(Inject.class));
    }

    // --- Puertos --------------------------------------------------------------------------------------------

    @Test
    void puertos_tienen_como_maximo_7_metodos() {
        ArchCondition<JavaClass> maximo7 = new ArchCondition<>("tener como máximo 7 métodos") {
            @Override
            public void check(JavaClass puerto, ConditionEvents eventos) {
                int cantidad = puerto.getMethods().size();
                eventos.add(new SimpleConditionEvent(puerto, cantidad <= 7,
                        puerto.getSimpleName() + " tiene " + cantidad + " métodos"));
            }
        };
        verificar(classes().that().areInterfaces().and().resideInAnyPackage("..application.in..", "..application.out..")
                .should(maximo7));
    }

    @Test
    void puertos_no_usan_nombres_tecnicos() {
        verificar(noClasses().that().resideInAnyPackage("..application.in..", "..application.out..")
                .should().haveNameMatching(".*(Jpa|Panache|Rest|Http|Sql).*"));
    }

    @Test
    void puertos_de_salida_no_exponen_tipos_de_framework() {
        verificar(noClasses().that().resideInAnyPackage("..application.in..", "..application.out..", "..application.dto..")
                .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS));
    }

    // --- Tiempo ---------------------------------------------------------------------------------------------

    @Test
    void el_nucleo_no_lee_el_reloj_del_sistema_sin_clock() {
        verificar(noClasses().that().resideInAnyPackage("..domain..", "..application..").should()
                .callMethod(Instant.class, "now")
                .orShould().callMethod(LocalDate.class, "now")
                .orShould().callMethod(LocalDateTime.class, "now")
                .orShould().callMethod(ZonedDateTime.class, "now")
                .orShould().callMethod(OffsetDateTime.class, "now")
                .orShould().callMethod(System.class, "currentTimeMillis"));
    }

    // --- Dominio rico, sin reglas en DTO ni entidades ---------------------------------------------------------

    @Test
    void value_objects_son_records_inmutables() {
        verificar(classes().that().haveSimpleNameEndingWith("Dinero").or().haveSimpleName("NumeroCuenta")
                .or().haveSimpleName("Titular").or().haveSimpleName("Periodo").should().beRecords());
    }

    @Test
    void entidades_de_dominio_no_tienen_setters_publicos() {
        verificar(noMethods().that().areDeclaredIn(com.netby.transferencias.domain.cuenta.Cuenta.class)
                .or().areDeclaredIn(com.netby.transferencias.domain.transferencia.Transferencia.class)
                .should().haveNameMatching("set[A-Z].*").andShould().bePublic());
    }

    @Test
    void los_dto_rest_solo_usan_not_null_y_not_blank() {
        verificar(noClasses().that().resideInAPackage("..infrastructure.adapter.in.rest.dto..").should().dependOnClassesThat(
                resideInAPackage("jakarta.validation.constraints..")
                        .and(not(belongToAnyOf(NotNull.class, NotBlank.class)))));
    }

    @Test
    void las_entidades_jpa_no_declaran_reglas_con_check() {
        verificar(noClasses().that().resideInAPackage("..infrastructure.adapter.out.persistence.entity..").should()
                .dependOnClassesThat().resideInAnyPackage("org.hibernate.annotations..", "jakarta.validation.."));
    }

    @Test
    void solo_el_exception_mapper_conoce_los_errores_de_dominio_en_infrastructure_adapter_in() {
        verificar(noClasses().that().resideInAPackage("..infrastructure.adapter.in..").and().doNotHaveSimpleName("GlobalExceptionMapper")
                .should().dependOnClassesThat().resideInAPackage("..domain.error.."));
    }

    @Test
    void los_campos_de_las_entidades_de_dominio_son_privados() {
        verificar(fields().that().areDeclaredInClassesThat().resideInAPackage("..domain..")
                .and().areNotStatic().and().areDeclaredInClassesThat().areNotRecords()
                .should().bePrivate());
    }
}
