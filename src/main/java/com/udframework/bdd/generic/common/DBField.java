package com.udframework.bdd.generic.common;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import com.udframework.bdd.annotations.ManyToMany;
import com.udframework.bdd.annotations.ManyToOne;
import com.udframework.bdd.annotations.OneToMany;
import com.udframework.bdd.annotations.OneToOne;
import com.udframework.bdd.generic.relationTypes.RelationType;

public abstract class DBField extends FieldData{

    protected RelationType relationType;

    public DBField(Field field) {
        super(field);
    }

    @Override
    protected void init() {
        super.init();
        extractRelations();
    }

    protected abstract void extractRelations ();

    public RelationType getRelationType() {
        return relationType;
    }

    public boolean isInRelation () {
        return relationType != null;
    }


    protected boolean checkMTM () {
        ManyToMany manyToMany = getAnnotation(ManyToMany.class);
        if (isNotNull(manyToMany)) {
            this.relationType = RelationType.ManyToMany;
            this.relationType.setAnnotation(manyToMany);
            return true;
        }
        return false;
    }

    protected boolean check1TM () {
        OneToMany manyT1 = getAnnotation(OneToMany.class);
        if (isNotNull(manyT1)) {
            this.relationType = RelationType.OneToMany;
            this.relationType.setAnnotation(manyT1);
            return true;
        }
        return false;
    }

    protected boolean check1T1 () {
        OneToOne oneT1 = getAnnotation(OneToOne.class);
        if (isNotNull(oneT1)) {
            this.relationType = RelationType.OneToOne;
            this.relationType.setAnnotation(oneT1);
            return true;
        }
        return false;
    }

    protected boolean checkMT1 () {
        ManyToOne manyT1 = this.field.getAnnotation(ManyToOne.class);
        if (manyT1 != null) {
            this.relationType = RelationType.ManyToOne;
            this.relationType.setAnnotation(manyT1);
            return true;
        }
        return false;
    }

    private boolean isNotNull(Annotation annotation) {
        return annotation != null;
    }
}
