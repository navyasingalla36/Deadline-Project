
package com.deadlineradar.servlet;

import com.deadlineradar.dao.DBConnection;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/analytics")
public class AnalyticsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String[] CATEGORY_NAMES = {
        "Assignment", "Exam", "Project",
        "Meeting", "Personal", "Other"
    };

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate");

        HttpSession session = request.getSession(false);
        Object userValue =
                session == null ? null : session.getAttribute("userId");

        if (!(userValue instanceof Number)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(
                    "{\"error\":\"Session expired\"}");
            return;
        }

        long userId = ((Number) userValue).longValue();

        long total = 0;
        long pending = 0;
        long completed = 0;
        long overdue = 0;
        long highPriority = 0;
        long upcoming = 0;

        Map<String, Long> categories = new LinkedHashMap<>();

        for (String category : CATEGORY_NAMES) {
            categories.put(category, 0L);
        }

        String statsSql =
                "SELECT COUNT(*) AS TOTAL, "
                + "NVL(SUM(CASE WHEN STATUS = 'Pending' "
                + "THEN 1 ELSE 0 END), 0) AS PENDING, "
                + "NVL(SUM(CASE WHEN STATUS = 'Completed' "
                + "THEN 1 ELSE 0 END), 0) AS COMPLETED, "
                + "NVL(SUM(CASE WHEN STATUS = 'Pending' "
                + "AND DUE_DATE < TRUNC(SYSDATE) "
                + "THEN 1 ELSE 0 END), 0) AS OVERDUE, "
                + "NVL(SUM(CASE WHEN PRIORITY = 'High' "
                + "AND STATUS = 'Pending' "
                + "THEN 1 ELSE 0 END), 0) AS HIGH_PRIORITY, "
                + "NVL(SUM(CASE WHEN STATUS = 'Pending' "
                + "AND DUE_DATE >= TRUNC(SYSDATE) "
                + "AND DUE_DATE <= TRUNC(SYSDATE) + 7 "
                + "THEN 1 ELSE 0 END), 0) AS UPCOMING "
                + "FROM DR_DEADLINES WHERE USER_ID = ?";

        String categorySql =
                "SELECT NVL(CATEGORY, 'Other') AS CATEGORY, "
                + "COUNT(*) AS AMOUNT "
                + "FROM DR_DEADLINES "
                + "WHERE USER_ID = ? "
                + "GROUP BY NVL(CATEGORY, 'Other')";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                throw new SQLException(
                        "Database connection failed.");
            }

            // Retrieve the user's deadline statistics.
            try (PreparedStatement statement =
                    connection.prepareStatement(statsSql)) {

                statement.setLong(1, userId);

                try (ResultSet result = statement.executeQuery()) {

                    if (result.next()) {
                        total = result.getLong("TOTAL");
                        pending = result.getLong("PENDING");
                        completed = result.getLong("COMPLETED");
                        overdue = result.getLong("OVERDUE");
                        highPriority =
                                result.getLong("HIGH_PRIORITY");
                        upcoming = result.getLong("UPCOMING");
                    }
                }
            }

            // Retrieve category-wise distribution.
            try (PreparedStatement statement =
                    connection.prepareStatement(categorySql)) {

                statement.setLong(1, userId);

                try (ResultSet result = statement.executeQuery()) {

                    while (result.next()) {
                        String category =
                                result.getString("CATEGORY");

                        long amount = result.getLong("AMOUNT");

                        if (category == null
                                || !categories.containsKey(category)) {
                            category = "Other";
                        }

                        categories.put(
                                category,
                                categories.get(category) + amount);
                    }
                }
            }

        } catch (SQLException e) {

            getServletContext().log(
                    "Unable to load analytics data.", e);

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            response.getWriter().write(
                    "{\"error\":\"Unable to load analytics data\"}");
            return;
        }

        double completionPercentage = total == 0
                ? 0.0
                : (completed * 100.0) / total;

        // Return JSON for analytics.html.
        StringBuilder json = new StringBuilder();

        json.append("{")
            .append("\"total\":").append(total).append(",")
            .append("\"pending\":").append(pending).append(",")
            .append("\"completed\":").append(completed).append(",")
            .append("\"overdue\":").append(overdue).append(",")
            .append("\"highPriority\":").append(highPriority).append(",")
            .append("\"upcoming\":").append(upcoming).append(",")
            .append("\"completionPercentage\":")
            .append(String.format(
                    java.util.Locale.US, "%.2f",
                    completionPercentage))
            .append(",")
            .append("\"categories\":{");

        boolean first = true;

        for (Map.Entry<String, Long> entry : categories.entrySet()) {

            if (!first) {
                json.append(",");
            }

            first = false;

            json.append("\"")
                .append(jsonEscape(entry.getKey()))
                .append("\":")
                .append(entry.getValue());
        }

        json.append("}}");

        response.getWriter().write(json.toString());
    }

    private String jsonEscape(String value) {

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
                        escaped.append(String.format(
                                "\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
            }
        }

        return escaped.toString();
    }
}
