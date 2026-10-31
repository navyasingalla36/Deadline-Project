
package com.deadlineradar.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;
import java.time.LocalDate;

import com.deadlineradar.dao.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/updateDeadline")
public class UpdateDeadlineServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
                session.getAttribute("userId") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/login.html");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        String idParameter = request.getParameter("id");
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String category = request.getParameter("category");
        String dueDate = request.getParameter("dueDate");
        String priority = request.getParameter("priority");
        String status = request.getParameter("status");
        String recurrence = request.getParameter("recurrence");

        if (idParameter == null || idParameter.trim().isEmpty()) {
            showMessage(response, "Invalid deadline ID.");
            return;
        }

        int deadlineId;

        try {
            deadlineId = Integer.parseInt(idParameter);
        } catch (NumberFormatException e) {
            showMessage(response, "Invalid deadline ID.");
            return;
        }

        if (title == null || title.trim().isEmpty()) {
            showMessage(response, "Deadline title cannot be empty.");
            return;
        }

        if (category == null || category.trim().isEmpty()) {
            showMessage(response, "Please select a category.");
            return;
        }

        if (dueDate == null || dueDate.trim().isEmpty()) {
            showMessage(response, "Please select a due date.");
            return;
        }

        if (priority == null || priority.trim().isEmpty()) {
            showMessage(response, "Please select a priority.");
            return;
        }

        // Validate the date format before using it in SQL.
        try {
            LocalDate.parse(dueDate);
        } catch (Exception e) {
            showMessage(response, "Please enter a valid due date.");
            return;
        }

        if (status == null ||
                (!status.equals("Pending") &&
                 !status.equals("Completed"))) {
            status = "Pending";
        }

        if (recurrence == null ||
                (!recurrence.equals("None") &&
                 !recurrence.equals("Daily") &&
                 !recurrence.equals("Weekly") &&
                 !recurrence.equals("Monthly"))) {
            recurrence = "None";
        }

        String oldStatus;
        String existingTitle;
        String existingDescription;
        String existingCategory;
        String existingPriority;
        LocalDate existingDueDate;

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {
                // STEP 1: Retrieve the existing deadline
                String selectSql =
                        "SELECT TITLE, DESCRIPTION, CATEGORY, " +
                        "DUE_DATE, PRIORITY, STATUS " +
                        "FROM DR_DEADLINES " +
                        "WHERE ID = ? AND USER_ID = ?";

                try (PreparedStatement selectPs =
                             con.prepareStatement(selectSql)) {

                    selectPs.setInt(1, deadlineId);
                    selectPs.setInt(2, userId);

                    try (ResultSet rs = selectPs.executeQuery()) {

                        if (!rs.next()) {
                            con.rollback();
                            showMessage(response, "Deadline not found.");
                            return;
                        }

                        existingTitle = rs.getString("TITLE");
                        existingDescription = rs.getString("DESCRIPTION");
                        existingCategory = rs.getString("CATEGORY");
                        existingPriority = rs.getString("PRIORITY");
                        oldStatus = rs.getString("STATUS");

                        Date databaseDueDate = rs.getDate("DUE_DATE");

                        existingDueDate = databaseDueDate == null
                                ? null
                                : databaseDueDate.toLocalDate();
                    }
                }

                // STEP 2: Update the deadline
                String updateSql =
                        "UPDATE DR_DEADLINES SET " +
                        "TITLE = ?, DESCRIPTION = ?, CATEGORY = ?, " +
                        "DUE_DATE = TO_DATE(?, 'YYYY-MM-DD'), " +
                        "PRIORITY = ?, STATUS = ?, RECURRENCE = ? " +
                        "WHERE ID = ? AND USER_ID = ?";

                try (PreparedStatement updatePs =
                             con.prepareStatement(updateSql)) {

                    updatePs.setString(1, title.trim());

                    if (description == null ||
                            description.trim().isEmpty()) {
                        updatePs.setNull(
                                2, java.sql.Types.VARCHAR);
                    } else {
                        updatePs.setString(2, description.trim());
                    }

                    updatePs.setString(3, category);
                    updatePs.setString(4, dueDate);
                    updatePs.setString(5, priority);
                    updatePs.setString(6, status);
                    updatePs.setString(7, recurrence);
                    updatePs.setInt(8, deadlineId);
                    updatePs.setInt(9, userId);

                    int updated = updatePs.executeUpdate();

                    if (updated == 0) {
                        con.rollback();
                        showMessage(
                                response,
                                "Unable to update deadline.");
                        return;
                    }
                }

                // STEP 3: Detect Pending -> Completed
                boolean newlyCompleted =
                        "Completed".equals(status) &&
                        !"Completed".equals(oldStatus);

                // STEP 4: Create the next recurring occurrence
                if (newlyCompleted &&
                        !"None".equals(recurrence) &&
                        existingDueDate != null) {

                    LocalDate nextDueDate =
                            calculateNextDueDate(
                                    existingDueDate, recurrence);

                    String insertSql =
                            "INSERT INTO DR_DEADLINES " +
                            "(USER_ID, TITLE, DESCRIPTION, CATEGORY, " +
                            "DUE_DATE, PRIORITY, STATUS, RECURRENCE, " +
                            "PARENT_DEADLINE_ID) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 'Pending', ?, ?)";

                    try (PreparedStatement insertPs =
                                 con.prepareStatement(insertSql)) {

                        insertPs.setInt(1, userId);
                        insertPs.setString(2, existingTitle);

                        if (existingDescription == null ||
                                existingDescription.trim().isEmpty()) {
                            insertPs.setNull(
                                    3, java.sql.Types.VARCHAR);
                        } else {
                            insertPs.setString(
                                    3, existingDescription);
                        }

                        insertPs.setString(4, existingCategory);
                        insertPs.setDate(
                                5, Date.valueOf(nextDueDate));
                        insertPs.setString(6, existingPriority);
                        insertPs.setString(7, recurrence);
                        insertPs.setInt(8, deadlineId);

                        insertPs.executeUpdate();
                    }
                }

                // STEP 5: Commit both operations together
                con.commit();

                // STEP 6: Redirect to the list with a success flag
                response.sendRedirect(
                        request.getContextPath()
                        + "/viewDeadlines?updated=success");

            } catch (Exception e) {
                try {
                    con.rollback();
                } catch (Exception rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;
            }

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    response,
                    "Error while updating deadline. Please try again.");
        }
    }

    private LocalDate calculateNextDueDate(
            LocalDate currentDate,
            String recurrence) {

        switch (recurrence) {
            case "Daily":
                return currentDate.plusDays(1);

            case "Weekly":
                return currentDate.plusWeeks(1);

            case "Monthly":
                return currentDate.plusMonths(1);

            default:
                return currentDate;
        }
    }

    private void showMessage(
            HttpServletResponse response,
            String message) throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        String safeMessage = escapeHtml(message);
        String contextPath = requestContextPath(response);

        response.getWriter().println(
                "<!DOCTYPE html><html><head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
                "<title>Deadline Radar</title>" +
                "<style>" +
                "body{font-family:Arial,sans-serif;background:#081321;color:white;" +
                "display:flex;justify-content:center;align-items:center;" +
                "min-height:100vh;margin:0}" +
                ".box{background:#0b1f36;padding:35px;border-radius:15px;" +
                "text-align:center;max-width:550px;margin:20px}" +
                "a{display:inline-block;margin-top:20px;padding:10px 18px;" +
                "background:#00b8e6;color:white;text-decoration:none;border-radius:8px}" +
                "</style></head><body><div class='box'>" +
                "<h2>Deadline Radar</h2><p>" + safeMessage + "</p>" +
                "<a href='" + contextPath + "/viewDeadlines'>" +
                "Back to Deadlines</a></div></body></html>");
    }

    private String requestContextPath(
            HttpServletResponse response) {
        // Relative navigation works for this servlet's error page.
        return "";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
