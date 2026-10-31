package com.deadlineradar.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;

import com.deadlineradar.dao.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/dbtest")
public class DBTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        Connection connection = DBConnection.getConnection();

        if (connection != null) {
            out.println("<h1>Database Connection Successful!</h1>");
            out.println("<p>Deadline Radar is connected to Oracle.</p>");
        } else {
            out.println("<h1>Database Connection Failed!</h1>");
            out.println("<p>Check the Eclipse Console for the error.</p>");
        }
    }
}