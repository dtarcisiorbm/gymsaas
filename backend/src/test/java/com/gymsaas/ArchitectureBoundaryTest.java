package com.gymsaas;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the module boundaries of the modular monolith: domain code has no
 * knowledge of Spring/JPA, application services do not know the HTTP layer and
 * persistence adapters do not leak into the application layer.
 */
@AnalyzeClasses(packages = "com.gymsaas", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureBoundaryTest {

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..application..", "..infrastructure..", "..presentation..");

    @ArchTest
    static final ArchRule application_does_not_depend_on_presentation =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..presentation..");

    @ArchTest
    static final ArchRule persistence_does_not_depend_on_presentation =
            noClasses().that().resideInAPackage("..infrastructure.persistence..")
                    .should().dependOnClassesThat().resideInAPackage("..presentation..");

    @ArchTest
    static final ArchRule controllers_live_in_presentation_packages =
            classes().that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage("..presentation..");

    @ArchTest
    static final ArchRule domain_is_not_annotated_with_spring_or_jpa =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..");
}
