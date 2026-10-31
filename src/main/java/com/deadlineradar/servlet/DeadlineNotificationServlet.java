

package com.deadlineradar.servlet;

import com.deadlineradar.dao.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@WebServlet("/deadlineNotification")
public class DeadlineNotificationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("[]");
            return;
        }

        int userId = ((Number) session.getAttribute("userId")).intValue();

        String sql =
            "SELECT ID, TITLE, DUE_DATE, PRIORITY " +
            "FROM DR_DEADLINES " +
            "WHERE USER_ID = ? AND STATUS = 'Pending' " +
            "AND DUE_DATE IS NOT NULL " +
            "ORDER BY DUE_DATE ASC";

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int id = rs.getInt("ID");
                    String title = rs.getString("TITLE");
                    Date dueDate = rs.getDate("DUE_DATE");
                    String priority = rs.getString("PRIORITY");

                    if (title == null || title.trim().isEmpty()) {
                        title = "Untitled Deadline";
                    }

                    if (dueDate == null) {
                        continue;
                    }

                    LocalDate today = LocalDate.now();
                    LocalDate deadlineDate = dueDate.toLocalDate();

                    long daysLeft = ChronoUnit.DAYS.between(
                        today, deadlineDate
                    );

                    String message;

                    if (daysLeft < 0) {
                        long overdueDays = Math.abs(daysLeft);

                        message = title + " is overdue by "
                                + overdueDays
                                + (overdueDays == 1 ? " day." : " days.");
                    } else if (daysLeft == 0) {
                        message = title + " is due today!";
                    } else if (daysLeft == 1) {
                        message = title + " is due tomorrow.";
                    } else {
                        message = title + " is due in "
                                + daysLeft + " days.";
                    }

                    if (!first) {
                        json.append(",");
                    }

                    json.append("{")
                        .append("\"id\":").append(id).append(",")
                        .append("\"title\":\"")
                        .append(escapeJson(title)).append("\",")
                        .append("\"message\":\"")
                        .append(escapeJson(message)).append("\",")
                        .append("\"priority\":\"")
                        .append(escapeJson(priority == null
                                ? "Normal" : priority))
                        .append("\"")
                        .append("}");

                    first = false;
                }
            }

            json.append("]");
            response.getWriter().write(json.toString());

        } catch (Exception e) {
            getServletContext().log(
                "Error fetching deadline notifications", e
            );

            response.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );
            response.getWriter().write("[]");
        }
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {

            char ch = value.charAt(i);

            switch (ch) {
                case '"':
                    escaped.append("\\\"");
                    break;

                case '\\':
                    escaped.append("\\\\");
                    break;

                case '\b':
                    escaped.append("\\b");
                    break;

                case '\f':
                    escaped.append("\\f");
                    break;

                case '\n':
                    escaped.append("\\n");
                    break;

                case '\r':
                    escaped.append("\\r");
                    break;

                case '\t':
                    escaped.append("\\t");
                    break;

                default:
                    if (ch < 0x20) {
                        escaped.append(
                            String.format("\\u%04x", (int) ch)
                        );
                    } else {
                        escaped.append(ch);
                    }
            }
        }

        return escaped.toString();
    }
}
