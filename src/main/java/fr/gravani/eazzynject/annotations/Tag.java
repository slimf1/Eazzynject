package fr.gravani.eazzynject.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to distinguish between two or more implementations of an abstraction
 */
@Target({ FIELD, METHOD, TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
public @interface Tag {
    String value() default "";
}
