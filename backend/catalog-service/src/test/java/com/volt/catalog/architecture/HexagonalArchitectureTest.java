package com.volt.catalog.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Executable form of technical specification §4 and ADR-0001.
 *
 * <p>§9 makes a passing architecture test an acceptance criterion, and §13
 * repeats it. The rules below are what "compliance with hexagonal architecture"
 * actually means in code — a layout that is only a folder convention decays
 * within a fortnight, because every violation individually looks reasonable at
 * the moment you write it.
 *
 * <p>Rules 1 and 5 of §4 are mandatory. The rest are enforced here because each
 * one is a mistake that is easy to make and expensive to unwind later.
 */
@AnalyzeClasses(
        packages = "com.volt.catalog",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "..catalog.domain..";
    private static final String APPLICATION = "..catalog.application..";
    private static final String INFRASTRUCTURE = "..catalog.infrastructure..";

    // ------------------------------------------------------------------ §4.1

    /**
     * §4 rule 1, and §13's acceptance criterion: the ArchUnit test verifying
     * the absence of framework dependencies in domain passes.
     *
     * <p>The point is not purity for its own sake: a domain with no framework on
     * its classpath can be unit-tested without starting a Spring context, which
     * is what makes §9's 70% coverage target reachable at all. A domain that
     * needs {@code @SpringBootTest} to test gets tested rarely.
     *
     * <p>{@code jakarta..} is banned here and nowhere else. It is required in
     * {@code adapter.out.persistence} (JPA entities) and on the request DTOs in
     * {@code adapter.in.web}, where §6.3 explicitly mandates it. Adding it to a
     * domain model turns this test red, which turns a §13 checkbox red.
     *
     * <p>It also buys nothing: a {@code @NotNull} on a domain field does exactly
     * nothing unless something invokes a {@code Validator}, and nothing does for
     * domain objects. The constructor guards already present — {@code quantity
     * < 1} throws — fire unconditionally. Bean validation belongs at the
     * boundary, where a framework is actually running.
     */
    @ArchTest
    static final ArchRule domain_is_free_of_frameworks = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "javax..",
                    "com.fasterxml.jackson..",
                    "org.hibernate..",
                    "io.swagger..",
                    "org.flywaydb..")
            .because("specification §4 rule 1: the domain must compile and be tested with no framework present");

    /*
     * Deliberately absent: a rule banning "lombok..".
     *
     * Lombok's annotations are RetentionPolicy.SOURCE — erased by the compiler,
     * never written to a .class file. ArchUnit reads bytecode, so such a rule
     * compiles, runs, passes, and guards nothing whatsoever. It is worse than no
     * rule at all, because it reads like protection.
     *
     * The domain's Lombok policy (@Getter and @ToString permitted; @Data,
     * @Value, @Setter, @EqualsAndHashCode, @AllArgsConstructor and @Builder
     * rejected) is enforced instead by domain/lombok.config, which runs inside
     * the compiler where the annotations still exist. See ADR-0008.
     */

    /**
     * The same rule for the application layer, minus the two Spring annotations
     * it genuinely needs. Allowing {@code @Service} and {@code @Transactional}
     * is a pragmatic concession, not an oversight: transaction demarcation is
     * genuinely an application-layer concern, and the alternative — a
     * hand-rolled transaction port — buys nothing on a project this size.
     * Anything else from Spring in this layer is infrastructure that has crept
     * inward.
     */
    @ArchTest
    static final ArchRule application_uses_only_transactional_spring = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..",
                    "org.springframework.http..",
                    "org.springframework.data..",
                    "org.springframework.security..",
                    "jakarta.persistence..",
                    "jakarta.servlet..")
            .because("web, HTTP, persistence and security are adapter concerns; "
                    + "returning a Spring Data Page from an out-port is the usual way this breaks");

    // ------------------------------------------------------------------ §4.5

    /**
     * §4 rule 5: dependencies point inward only. This is the rule that makes
     * every other one enforceable — once infrastructure can be referenced from
     * application, nothing else holds.
     */
    @ArchTest
    static final ArchRule dependencies_point_inward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Infrastructure").definedBy(INFRASTRUCTURE)
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
            .because("specification §4 rule 5: infrastructure -> application -> domain");

    @ArchTest
    static final ArchRule domain_depends_on_nothing_internal = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE)
            .because("the domain is the innermost layer and knows about nothing else");

    // ------------------------------------------------------------------ §4.3

    /**
     * §4 rule 3: use cases depend on interfaces, never on implementations. A
     * port that is accidentally declared as a class still compiles and still
     * looks like hexagonal architecture in the folder tree, which is why this
     * is worth checking mechanically.
     */
    @ArchTest
    static final ArchRule ports_are_interfaces = classes()
            .that().resideInAPackage("..application.port..")
            .and().areTopLevelClasses()
            .should().beInterfaces()
            .because("specification §4 rule 3: a port is a contract, not an implementation");

    @ArchTest
    static final ArchRule in_ports_are_named_use_cases = classes()
            .that().resideInAPackage("..application.port.in..")
            .and().areTopLevelClasses()
            .and().doNotHaveSimpleName("package-info")
            .should().haveSimpleNameEndingWith("UseCase")
            .because("a driving port is named for the business intent it serves");

    @ArchTest
    static final ArchRule out_ports_are_named_ports = classes()
            .that().resideInAPackage("..application.port.out..")
            .and().areTopLevelClasses()
            .and().doNotHaveSimpleName("package-info")
            .should().haveSimpleNameEndingWith("Port")
            .because("a driven port declares a need; the name should not mention the technology");

    // ------------------------------------------------------------------ §4.2

    /**
     * §4 rule 2: JPA entities are distinct from domain models. Confining
     * {@code jakarta.persistence} to one package is what forces that split to
     * stay real — the moment an entity can be annotated anywhere, the domain
     * model and the table schema quietly become the same object and every JPA
     * constraint becomes a business rule by accident.
     */
    @ArchTest
    static final ArchRule jpa_lives_only_in_the_persistence_adapter = noClasses()
            .that().resideOutsideOfPackage("..infrastructure.adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..")
            .because("specification §4 rule 2: JPA entities are separate from domain models");

    @ArchTest
    static final ArchRule jpa_entities_are_named_consistently = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().haveSimpleNameEndingWith("JpaEntity")
            .andShould().resideInAPackage("..infrastructure.adapter.out.persistence.entity..")
            .because("the *JpaEntity suffix keeps the distinction from domain models visible at a glance");

    // ------------------------------------------------------- adapter placement

    @ArchTest
    static final ArchRule controllers_live_in_the_web_adapter = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().resideInAPackage("..infrastructure.adapter.in.web.controller..")
            .because("a controller is a driving adapter and belongs nowhere else");

    @ArchTest
    static final ArchRule spring_data_repositories_live_in_the_persistence_adapter = classes()
            .that().haveSimpleNameEndingWith("JpaRepository")
            .should().resideInAPackage("..infrastructure.adapter.out.persistence.repository..");

    // --------------------------------------------------- adapter package layout

    /*
     * The rules below pin the internal shape of the two adapters. Both packages
     * were once a single flat directory holding controllers, request DTOs,
     * response DTOs and the exception handler side by side, which is readable at
     * six files and unreadable at sixteen. Grouping alone would drift back within
     * a month, because every individual file is easier to drop next to its
     * neighbour than to file correctly — so the grouping is asserted here rather
     * than described in a wiki page.
     *
     * Note that all of these packages remain inside `adapter.in.web` and
     * `adapter.out.persistence`. That is deliberate: the hexagonal rules above
     * are written against those prefixes, so organising within them buys
     * readability without loosening a single boundary.
     */

    @ArchTest
    static final ArchRule request_dtos_live_in_the_request_package = classes()
            .that().resideInAPackage("..infrastructure.adapter.in.web..")
            .and().areTopLevelClasses()
            .and().haveSimpleNameEndingWith("Request")
            .should().resideInAPackage("..infrastructure.adapter.in.web.dto.request..")
            .because("an inbound payload is validated at the boundary and mapped to a command; "
                    + "keeping the set together is what makes the API surface reviewable");

    @ArchTest
    static final ArchRule response_dtos_live_in_the_response_package = classes()
            .that().resideInAPackage("..infrastructure.adapter.in.web..")
            .and().areTopLevelClasses()
            .and().haveSimpleNameEndingWith("Response")
            .should().resideInAPackage("..infrastructure.adapter.in.web.dto.response..")
            .because("responses are the published contract and must not be mistaken for domain models");

    @ArchTest
    static final ArchRule exception_handling_lives_in_the_advice_package = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
            .should().resideInAPackage("..infrastructure.adapter.in.web.advice..")
            .because("error translation applies across every controller and is changed independently of them");

    @ArchTest
    static final ArchRule persistence_mappers_live_in_the_mapper_package = classes()
            .that().haveSimpleNameEndingWith("PersistenceMapper")
            .should().resideInAPackage("..infrastructure.adapter.out.persistence.mapper..")
            .because("entity/domain translation is the seam specification §4 rule 2 rests on");

    /**
     * The web adapter must not touch JPA entities.
     *
     * <p>Serialising an entity straight out of a controller is the single most
     * common way the DTO layer stops meaning anything: it works, it is shorter,
     * and it silently republishes the table schema as the API contract. The
     * split into {@code dto.response} and {@code persistence.entity} only holds
     * while nothing bridges the two.
     */
    @ArchTest
    static final ArchRule web_adapter_does_not_touch_jpa_entities = noClasses()
            .that().resideInAPackage("..infrastructure.adapter.in.web..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.adapter.out.persistence..")
            .because("controllers return response DTOs built from domain models, never persistence entities");

    /**
     * The web layer talks to in-ports, never to use case implementations.
     * Without this the interfaces still exist but nothing uses them, and the
     * indirection becomes decoration.
     */
    @ArchTest
    static final ArchRule web_adapter_does_not_reach_into_usecases = noClasses()
            .that().resideInAPackage("..infrastructure.adapter.in.web..")
            .should().dependOnClassesThat().resideInAPackage("..application.usecase..")
            .because("controllers depend on port.in interfaces, not on their implementations");

    // ------------------------------------------------------------------ hygiene

    @ArchTest
    static final ArchRule no_field_injection = noClasses()
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .orShould().dependOnClassesThat().haveFullyQualifiedName(
                    "org.springframework.beans.factory.annotation.Autowired")
            .because("constructor injection keeps use cases instantiable in a plain unit test");

    @ArchTest
    static final ArchRule no_cycles_between_layers = com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
            .slices().matching("com.volt.catalog.(*)..").should().beFreeOfCycles();
}
