package com.example.ui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Simple JDBC connection helper for the Swing UI — one connection per query
public class DatabaseConnection {

    private static final String URL = "jdbc:postgresql://localhost:5433/resource_engine";
    private static final String USERNAME = "Javadb";
    private static final String PASSWORD = "123456789";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL driver not found — check pom.xml", e);
        }
    }
}
