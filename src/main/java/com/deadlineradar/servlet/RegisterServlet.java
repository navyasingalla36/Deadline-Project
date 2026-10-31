package com.deadlineradar.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;

import com.deadlineradar.dao.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {

        // Get form data
        String name =
                request.getParameter("name");

        String email =
                request.getParameter("email");

        String password =
                request.getParameter("password");

        // Basic validation
        if (name == null ||
            email == null ||
            password == null ||
            name.trim().isEmpty() ||
            email.trim().isEmpty() ||
            password.isEmpty()) {

            response.setContentType("text/html;charset=UTF-8");

            response.getWriter().println(
                "<h2>All fields are required.</h2>"
            );

            return;
        }

        // Hash password
        String hashedPassword =
                hashPassword(password);

        String sql =
                "INSERT INTO DR_USERS " +
                "(ID, NAME, EMAIL, PASSWORD) " +
                "VALUES (DR_USERS_SEQ.NEXTVAL, ?, ?, ?)";

        try (Connection con =
                     DBConnection.getConnection();

             PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            ps.setString(2, email.trim());
            ps.setString(3, hashedPassword);

            ps.executeUpdate();

            response.setContentType(
                "text/html;charset=UTF-8"
            );

            response.getWriter().println("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Registration Successful</title>

                    <style>

                        body {
                            font-family: Arial, sans-serif;
                            background: #081321;
                            color: white;
                            text-align: center;
                            padding-top: 100px;
                        }

                        .box {
                            width: 90%;
                            max-width: 500px;
                            margin: auto;
                            background: #0b1f36;
                            padding: 35px;
                            border-radius: 15px;
                            border: 1px solid #173452;
                        }

                        h1 {
                            color: #22d3ee;
                        }

                        p {
                            color: #a9bbcc;
                            margin: 20px 0;
                        }

                        a {
                            display: inline-block;
                            text-decoration: none;
                            background: #22d3ee;
                            color: #081321;
                            padding: 11px 20px;
                            border-radius: 8px;
                            font-weight: bold;
                        }

                    </style>
                </head>

                <body>

                    <div class="box">

                        <h1>Registration Successful! ✅</h1>

                        <p>
                            Your account has been created securely.
                        </p>

                        <a href="login.html">
                            Go to Login
                        </a>

                    </div>

                </body>
                </html>
                """);

        } catch (Exception e) {

            e.printStackTrace();

            response.setContentType(
                "text/html;charset=UTF-8"
            );

            // Handle duplicate email
            if (e.getMessage() != null &&
                e.getMessage().contains("ORA-00001")) {

                response.getWriter().println("""
                    <h2>Email already registered.</h2>
                    <p>Please use another email address.</p>
                    <a href="register.html">
                        Back to Registration
                    </a>
                    """);

            } else {

                response.getWriter().println(
                    "<h2>Registration failed.</h2>"
                );
            }
        }
    }


    // =========================================================
    // SHA-256 PASSWORD HASHING
    // =========================================================

    private String hashPassword(String password) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    md.digest(
                        password.getBytes(
                            StandardCharsets.UTF_8
                        )
                    );

            StringBuilder hexString =
                    new StringBuilder();

            for (byte b : hash) {

                String hex =
                        Integer.toHexString(
                            0xff & b
                        );

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {

            throw new RuntimeException(
                "Password hashing failed",
                e
            );
        }
    }
}