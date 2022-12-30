package com.udframework.database.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Sequence {

    String pgPattern = "select {method}('{seq_name}')";

    String name () default "";

    /**
     * you need to define how your database interacts with sequences
     * example:
     * - postgresql uses nextval('seq_name')
     * - oracle seq_name.nextval
     *
     * the dbPatternForm will contain 2 key word:
     * - seq_name
     * - method
     *
     * these keys will be changed depending on the type of values
     * the user wants {nextval, currval, etc...}
     *
     * example:
     * - for postgres: select {method}('{seq_name}')
     */

    String dbPattern() default pgPattern;
}
