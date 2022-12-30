package com.udframework.database.generic.relationTypes;

import java.lang.annotation.Annotation;

import com.udframework.database.annotations.ManyToMany;
import com.udframework.database.annotations.ManyToOne;
import com.udframework.database.annotations.OneToMany;
import com.udframework.database.annotations.OneToOne;

public enum RelationType {

    OneToMany(OneToMany.class),
    ManyToOne(ManyToOne.class),
    OneToOne(OneToOne.class),
    ManyToMany(ManyToMany.class);

    public final Class<?> clazz;
    private Annotation annotation;
    RelationType(Class<?> clazz) {
        this.clazz = clazz;
    }


    public Annotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }

    public boolean isOne() {
        return this == OneToOne;
    }
}
