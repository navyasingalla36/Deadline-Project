package com.deadlineradar.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.deadlineradar.dao.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/addDeadline")
public class AddDeadlineServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // Check whether the user is logged in
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.html");
            return;
        }

        int userId;

        try {
            userId = Integer.parseInt(
                    session.getAttribute("userId").toString());
        } catch (NumberFormatException e) {
            response.sendRedirect("login.html");
            return;
        }

        // Read form values
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String category = request.getParameter("category");
        String dueDate = request.getParameter("dueDate");
        String priority = request.getParameter("priority");
        String recurrence = request.getParameter("recurrence");

        // Validate required fields
        if (title == null || title.trim().isEmpty()
                || category == null || category.trim().isEmpty()
                || dueDate == null || dueDate.trim().isEmpty()
                || priority == null || priority.trim().isEmpty()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Please provide all required deadline details.");
            return;
        }

        title = title.trim();
        description = description == null ? "" : description.trim();
        category = category.trim();
        priority = priority.trim();

        // Validate field lengths
        if (title.length() > 150 || description.length() > 500
                || category.length() > 50 || priority.length() > 20) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "One or more fields exceed the allowed length.");
            return;
        }

        // Validate allowed categories
        if (!category.equals("Assignment")
                && !category.equals("Exam")
                && !category.equals("Project")
                && !category.equals("Meeting")
                && !category.equals("Personal")
                && !category.equals("Other")) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid category selected.");
            return;
        }

        // Validate priority
        if (!priority.equals("High")
                && !priority.equals("Medium")
                && !priority.equals("Low")) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid priority selected.");
            return;
        }

        // Validate recurrence
        if (recurrence == null || recurrence.trim().isEmpty()) {
            recurrence = "None";
        }

        if (!recurrence.equals("None")
                && !recurrence.equals("Daily")
                && !recurrence.equals("Weekly")
                && !recurrence.equals("Monthly")) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid recurrence option selected.");
            return;
        }

        // Validate the date format and reject past dates
        java.time.LocalDate parsedDueDate;

        try {
            parsedDueDate = java.time.LocalDate.parse(dueDate);
        } catch (java.time.format.DateTimeParseException e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid due date format.");
            return;
        }

        if (parsedDueDate.isBefore(java.time.LocalDate.now())) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Due date cannot be earlier than today.");
            return;
        }

        // Insert deadline into Oracle
        String sql =
                "INSERT INTO DR_DEADLINES "
                + "(USER_ID, TITLE, DESCRIPTION, CATEGORY, "
                + "DUE_DATE, PRIORITY, STATUS, RECURRENCE, "
                + "PARENT_DEADLINE_ID) "
                + "VALUES (?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), "
                + "?, 'Pending', ?, NULL)";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Unable to connect to the database.");
                return;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, userId);
                statement.setString(2, title);
                statement.setString(3, description);
                statement.setString(4, category);
                statement.setString(5, dueDate);
                statement.setString(6, priority);
                statement.setString(7, recurrence);

                int rowsInserted = statement.executeUpdate();

                if (rowsInserted > 0) {
                    response.sendRedirect("viewDeadlines?added=success");
                } else {
                    response.sendError(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "The deadline could not be added.");
                }
            }

        } catch (SQLException e) {
            log("Error adding deadline to Oracle database.", e);

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An error occurred while saving the deadline.");
        }
    }
}