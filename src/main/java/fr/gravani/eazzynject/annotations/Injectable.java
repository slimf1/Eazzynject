package fr.gravani.eazzynject.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks that a type is subject to be injected or see its dependencies being injected
 */
@Target(ElementType.TYPE)
@Retention(RUNTIME)
public @interface Injectable {
}
