package fr.gravani.eazzynject.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Target({ FIELD, METHOD, PARAMETER, TYPE, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Tag {
    String value() default "";
}
