package fr.gravani.eazzynject.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks that the dependency container only creates one instance of the type. Whenever the type is injected into a
 * dependency, the same instance gets used.
 */
@Target(ElementType.TYPE)
@Retention(RUNTIME)
public @interface Singleton {
}
