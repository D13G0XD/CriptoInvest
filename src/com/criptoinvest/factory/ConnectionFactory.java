package com.criptoinvest.factory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {

    private static final String url = "inserirurljdbc.com";
    private static final String user = "usuario";
    private static final String password = "senha";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }


}
