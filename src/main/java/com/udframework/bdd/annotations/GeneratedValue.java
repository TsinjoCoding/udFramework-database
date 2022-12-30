package com.udframework.bdd.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface GeneratedValue {

    String seqName() default "";

    /**
     * sometimes you want the generated value to be a specific string
     * like 'STR0001'
     * completedStr will define the 'STR'
     */
    String completedStr() default "";

    /**
     * the pad char is the char which is inserted between the completedStr
     * and the sequence value
     */
    char padChar() default '0';

    /**
     * if the generated value will be a string you need to define
     * its length
     */
    int len() default Integer.MAX_VALUE;

    /**
     * lpad defines the concatenated-form of the value.
     * by default, lpad will be true otherwise the right pad method will be used
     */
    boolean lPad() default true;
}
