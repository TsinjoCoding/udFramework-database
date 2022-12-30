package com.udframework.bdd;

import java.sql.Connection;

public interface DBValidation {

    void validateCreate(Connection connection) throws Exception;
    void validateUpdate (Connection connection) throws Exception;
    void validateDelete (Connection connection) throws Exception;

}
