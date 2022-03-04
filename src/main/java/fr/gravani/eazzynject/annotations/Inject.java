package fr.gravani.eazzynject.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ CONSTRUCTOR, FIELD, METHOD })
@Retention(RUNTIME)
public @interface Inject {
}
