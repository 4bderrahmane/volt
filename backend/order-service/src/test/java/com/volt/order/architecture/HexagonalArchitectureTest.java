package com.volt.order.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Enforces the service's hexagonal architecture boundaries. */
@AnalyzeClasses(
        packages = "com.volt.order",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "..order.domain..";
    private static final String APPLICATION = "..order.application..";
    private static final String INFRASTRUCTURE = "..order.infrastructure..";

    // Lombok restrictions live in domain/lombok.config because SOURCE-retained
    // annotations are absent from the bytecode ArchUnit inspects.
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
            .because("the domain must compile and be tested with no framework present");

    // @Service and @Transactional are deliberate application-layer exceptions.
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

    @ArchTest
    static final ArchRule dependencies_point_inward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Infrastructure").definedBy(INFRASTRUCTURE)
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
            .because("dependencies point inward: infrastructure -> application -> domain");

    @ArchTest
    static final ArchRule domain_depends_on_nothing_internal = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE)
            .because("the domain is the innermost layer and knows about nothing else");

    @ArchTest
    static final ArchRule ports_are_interfaces = classes()
            .that().resideInAPackage("..application.port..")
            .and().areTopLevelClasses()
            .should().beInterfaces()
            .because("a port is a contract, not an implementation");

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

    @ArchTest
    static final ArchRule jpa_lives_only_in_the_persistence_adapter = noClasses()
            .that().resideOutsideOfPackage("..infrastructure.adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..")
            .because("JPA entities are separate from domain models");

    @ArchTest
    static final ArchRule jpa_entities_are_named_consistently = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().haveSimpleNameEndingWith("JpaEntity")
            .andShould().resideInAPackage("..infrastructure.adapter.out.persistence.entity..")
            .because("the *JpaEntity suffix keeps the distinction from domain models visible at a glance");

    @ArchTest
    static final ArchRule controllers_live_in_the_web_adapter = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().resideInAPackage("..infrastructure.adapter.in.web.controller..")
            .because("a controller is a driving adapter and belongs nowhere else");

    @ArchTest
    static final ArchRule spring_data_repositories_live_in_the_persistence_adapter = classes()
            .that().haveSimpleNameEndingWith("JpaRepository")
            .should().resideInAPackage("..infrastructure.adapter.out.persistence.repository..");

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
            .because("entity/domain translation keeps persistence and domain models separate");

    @ArchTest
    static final ArchRule web_adapter_does_not_touch_jpa_entities = noClasses()
            .that().resideInAPackage("..infrastructure.adapter.in.web..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.adapter.out.persistence..")
            .because("controllers return response DTOs built from domain models, never persistence entities");

    @ArchTest
    static final ArchRule web_adapter_does_not_reach_into_usecases = noClasses()
            .that().resideInAPackage("..infrastructure.adapter.in.web..")
            .should().dependOnClassesThat().resideInAPackage("..application.usecase..")
            .because("controllers depend on port.in interfaces, not on their implementations");

    @ArchTest
    static final ArchRule no_field_injection = noClasses()
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .orShould().dependOnClassesThat().haveFullyQualifiedName(
                    "org.springframework.beans.factory.annotation.Autowired")
            .because("constructor injection keeps use cases instantiable in a plain unit test");

    @ArchTest
    static final ArchRule no_cycles_between_layers = com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
            .slices().matching("com.volt.order.(*)..").should().beFreeOfCycles();
}
