package com.udframework.bdd.generic;

import com.udframework.bdd.DBUtils;
import com.udframework.bdd.annotations.Sequence;
import com.udframework.bdd.exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;

public class DBSequence {

    private static final HashMap<String, DBSequence> sequences = new HashMap<>();
    private long currval;
    private String nextPattern, currvalPattern;
    private String pattern, name;

    Sequence seq;

    private DBSequence(Sequence seq) {
        this(seq.name(), seq.dbPattern());
    }

    public DBSequence(String name, String dbPattern) {
        this.name = name;
        this.pattern = dbPattern;
        setPattern();
    }

    private void setPattern() {
        this.nextPattern = pattern
                .replace("{method}", "nextval")
                .replace("{seq_name}", getName());
        this.currvalPattern = pattern
                .replace("{method}", "currval")
                .replace("{seq_name}", getName());
    }

    public static DBSequence register (Sequence sequence) {
        return register(sequence.name(), sequence.dbPattern());
    }

    public static DBSequence register (String name, String pattern) {
        DBSequence sq = sequences.get(name);
        if (sq == null) {
            sq = new DBSequence(name, pattern);
            sequences.put(name, sq);
            return sq;
        }
        return sq;
    }

    public static DBSequence register (Class<?> clazz) throws DatabaseException, NoSuchMethodException {
        Sequence seq = clazz.getAnnotation(Sequence.class);
        if(seq == null) return null;
        String name = seq.name();
        ClassMetadata classData = ClassMetadata.getMetadataOf(clazz);
        if (Objects.equals(name, "")) name = classData.getTableName()+"_"+classData.getPrimaryKey().getColumnName()+"_seq";
        return register(name, seq.dbPattern());
    }

    public static DBSequence getDBSequence (String name) {
        return sequences.get(name);
    }

    public String getName () {
        return name;
    }

    public Long nextVal (Connection connection) throws SQLException {
        Long next = DBUtils.oneValue(this.nextPattern, Long.class, connection);
        currval = next;
        return next;
    }

    public Long currval (Connection connection) throws SQLException {
        if (currval != 0L) return currval;
        return DBUtils.oneValue(this.currvalPattern, Long.class, connection);
    }

}
