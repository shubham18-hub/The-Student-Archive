package com.example.ui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// This class manages the connection between our Swing desktop app and PostgreSQL.
public class DatabaseConnection {

    // Database settings — must match docker-compose.yml and application.properties
    private static final String HOST = "localhost";
    private static final int PORT = 5433;
    private static final String DATABASE = "resource_engine";
    private static final String USERNAME = "Javadb";
    private static final String PASSWORD = "123456789";

    private static final String URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE;

    
    // Call  SearchService every time it needs to run a query
    public static Connection getConnection() throws SQLException {
        try {
            // Load the PostgreSQL JDBC driver 
            Class.forName("org.postgresql.Driver");

            // Create and return the connection
            Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            return conn;

        } catch (ClassNotFoundException e) {
            // This happens if the postgresql JAR is missing from the classpath
            throw new SQLException("PostgreSQL driver not found. Check pom.xml.", e);
        }
    }
}
