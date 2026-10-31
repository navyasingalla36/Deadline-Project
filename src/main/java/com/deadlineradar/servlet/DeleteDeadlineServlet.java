
package com.deadlineradar.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import com.deadlineradar.dao.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/deleteDeadline")
public class DeleteDeadlineServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        // CHECK LOGIN SESSION
        HttpSession session = request.getSession(false);

        if (session == null ||
                session.getAttribute("userId") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/login.html");
            return;
        }

        int userId;

        try {
            userId = Integer.parseInt(
                    session.getAttribute("userId").toString());
        } catch (NumberFormatException e) {
            session.invalidate();
            response.sendRedirect(
                    request.getContextPath() + "/login.html");
            return;
        }

        // GET DEADLINE ID
        String idParameter = request.getParameter("id");

        if (idParameter == null || idParameter.trim().isEmpty()) {
            response.sendRedirect(
                    request.getContextPath() + "/viewDeadlines");
            return;
        }

        int deadlineId;

        try {
            deadlineId = Integer.parseInt(idParameter);
        } catch (NumberFormatException e) {
            response.sendRedirect(
                    request.getContextPath() + "/viewDeadlines");
            return;
        }

        // DELETE ONLY THE LOGGED-IN USER'S DEADLINE
        String sql =
                "DELETE FROM DR_DEADLINES " +
                "WHERE ID = ? AND USER_ID = ?";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                throw new ServletException(
                        "Database connection is unavailable.");
            }

            try (PreparedStatement statement =
                    connection.prepareStatement(sql)) {

                statement.setInt(1, deadlineId);
                statement.setInt(2, userId);

                int result = statement.executeUpdate();

                if (result > 0) {
                    // SUCCESS: SHOW TOAST ON DEADLINE LIST
                    response.sendRedirect(
                            request.getContextPath()
                            + "/viewDeadlines?deleted=success");
                } else {
                    // DEADLINE NOT FOUND OR NOT OWNED BY THIS USER
                    response.sendRedirect(
                            request.getContextPath()
                            + "/viewDeadlines?deleteError=notfound");
                }
            }

        } catch (Exception e) {
            log("Error deleting deadline.", e);

            response.sendRedirect(
                    request.getContextPath()
                    + "/viewDeadlines?deleteError=failed");
        }
    }
}
