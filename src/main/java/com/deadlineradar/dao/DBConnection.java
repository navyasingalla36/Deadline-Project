package com.deadlineradar.dao;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL =
            "jdbc:oracle:thin:@localhost:1521:xe";

    private static final String USER =
            "DEADLINE_USER";

    private static final String PASSWORD =
            "Deadline123";

    public static Connection getConnection() {

        try {
            Class.forName("oracle.jdbc.OracleDriver");

            Connection connection =
                    DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("Oracle Database Connected Successfully!");

            return connection;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}