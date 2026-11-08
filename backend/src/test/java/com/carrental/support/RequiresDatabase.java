package com.carrental.support;

import org.junit.jupiter.api.condition.EnabledIf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skips a test class when neither Docker nor TEST_DB_URL is available. {@code @Inherited} matters:
 * JUnit's {@code @EnabledIf} alone on an abstract base class is not seen by its subclasses.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@EnabledIf("com.carrental.support.AbstractIntegrationTest#databaseAvailable")
public @interface RequiresDatabase {
}
