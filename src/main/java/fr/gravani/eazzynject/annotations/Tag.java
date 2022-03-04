package fr.gravani.eazzynject.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ FIELD, METHOD, TYPE, CONSTRUCTOR })
@Retention(RUNTIME)
public @interface Tag {
    String value() default "";
}
