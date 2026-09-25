package com.invoiceflow;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

/** Project conventions that Spring Modulith's boundary check does not cover. */
@AnalyzeClasses(packages = "com.invoiceflow", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

    private static final String[] BUSINESS_MODULES = {
        "com.invoiceflow.tenant..", "com.invoiceflow.customer..", "com.invoiceflow.catalog..",
        "com.invoiceflow.invoicing..", "com.invoiceflow.payment..", "com.invoiceflow.ledger.."
    };

    @ArchTest
    static final ArchRule sharedKernelDoesNotDependOnBusinessModules = noClasses()
            .that().resideInAPackage("com.invoiceflow.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(BUSINESS_MODULES)
            .because("the shared kernel is used by every module and must not know about any of them");

    @ArchTest
    static final ArchRule mongoIsOnlyReachedThroughTheTenantScopedGateway = noClasses()
            .that().resideOutsideOfPackage("com.invoiceflow.shared.tenancy..")
            .should().dependOnClassesThat().areAssignableTo(MongoOperations.class)
            .orShould().dependOnClassesThat().areAssignableTo(Repository.class)
            .because("all MongoDB access must go through TenantScopedMongoOperations so queries are tenant-scoped");

    @ArchTest
    static final ArchRule noFloatingPointFields = noFields()
            .that().areDeclaredInClassesThat().resideInAPackage("com.invoiceflow..")
            .should().haveRawType(double.class)
            .orShould().haveRawType(Double.class)
            .orShould().haveRawType(float.class)
            .orShould().haveRawType(Float.class)
            .because("money and quantities must use BigDecimal or Money, never binary floating point");

    @ArchTest
    static final ArchRule noFieldInjection = fields()
            .should().notBeAnnotatedWith(Autowired.class)
            .because("constructor injection keeps dependencies explicit and testable");

    @ArchTest
    static final ArchRule controllersDoNotRepeatTheApiPrefix = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should(RequestMappings.startingWith("/api"))
            .because("the /api/v1 prefix is added centrally by ApiWebConfiguration");
}
