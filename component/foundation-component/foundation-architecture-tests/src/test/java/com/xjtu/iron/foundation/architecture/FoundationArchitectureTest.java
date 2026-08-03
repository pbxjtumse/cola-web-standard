package com.xjtu.iron.foundation.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 防止 Foundation 反向依赖上层技术组件或把框架实现泄漏到纯基础模块。
 */
class FoundationArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.xjtu.iron.foundation");

    @Test
    void foundationMustNotDependOnUpperComponents() {
        noClasses().that().resideInAPackage("com.xjtu.iron.foundation..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.xjtu.iron.message..",
                        "com.xjtu.iron.cache..",
                        "com.xjtu.iron.retry..",
                        "com.xjtu.iron.distributed.lock..",
                        "com.xjtu.iron.transaction.."
                )
                .check(classes);
    }

    @Test
    void foundationIdMustRemainFrameworkIndependent() {
        noClasses().that().resideInAPackage("com.xjtu.iron.foundation.id..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "io.micrometer..",
                        "com.fasterxml.jackson.."
                )
                .check(classes);
    }

    @Test
    void serializationApiMustNotDependOnJackson() {
        noClasses().that().resideInAPackage("com.xjtu.iron.foundation.serialization..")
                .and().resideOutsideOfPackage(
                        "com.xjtu.iron.foundation.serialization.jackson.."
                )
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.fasterxml.jackson.."
                )
                .check(classes);
    }

    @Test
    void executionContextMustNotOwnThreadLocalStorage() {
        noClasses().that().resideInAPackage("com.xjtu.iron.foundation.context..")
                .should().dependOnClassesThat().areAssignableTo(ThreadLocal.class)
                .check(classes);
    }
}
