package com.atm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() { }

    public static Connection getConnection() throws SQLException {
        String url = envOrDefault("ATM_DB_URL",
            "jdbc:mysql://localhost:3306/atm_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = envOrDefault("ATM_DB_USER", "root");
        String password = System.getenv("ATM_DB_PASSWORD");

        if (password == null) {
            password = "";
        }

        return DriverManager.getConnection(url, user, password);
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
