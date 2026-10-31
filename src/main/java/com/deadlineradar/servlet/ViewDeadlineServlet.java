
package com.deadlineradar.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.deadlineradar.dao.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/viewDeadlines")
public class ViewDeadlineServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(
                    request.getContextPath() + "/login.html");
            return;
        }

        int userId = Integer.parseInt(
                session.getAttribute("userId").toString());

        String added = request.getParameter("added");
        String updated = request.getParameter("updated");
        String deleted = request.getParameter("deleted");

        String successMessage = null;

        if ("success".equals(added)) {
            successMessage = "Deadline added successfully!";
        } else if ("success".equals(updated)) {
            successMessage = "Deadline updated successfully!";
        } else if ("success".equals(deleted)) {
            successMessage = "Deadline deleted successfully!";
        }

        String search = request.getParameter("search");
        String category = request.getParameter("category");
        String priority = request.getParameter("priority");
        String status = request.getParameter("status");

        if (search == null) search = "";
        if (category == null) category = "";
        if (priority == null) priority = "";
        if (status == null) status = "";

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {

            out.println("<!DOCTYPE html>");
            out.println("<html lang='en'>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            out.println("<title>My Deadlines | Deadline Radar</title>");

            out.println("<style>");

            out.println("*{box-sizing:border-box;margin:0;padding:0;font-family:Arial,sans-serif;}");
            out.println("body{background:#081321;color:#e8f0fa;min-height:100vh;}");
            out.println(".navbar{background:#0b1f36;padding:18px 5%;display:flex;justify-content:space-between;align-items:center;gap:15px;flex-wrap:wrap;border-bottom:1px solid #1b3855;}");
            out.println(".brand{font-size:22px;font-weight:bold;color:#53d8fb;}");
            out.println(".navlinks{display:flex;gap:18px;flex-wrap:wrap;}");
            out.println(".navlinks a{color:#c7d5e7;text-decoration:none;font-size:14px;}");
            out.println(".navlinks a:hover{color:#53d8fb;}");
            out.println(".container{max-width:1200px;margin:35px auto;padding:0 20px;}");
            out.println(".heading{display:flex;justify-content:space-between;align-items:center;gap:15px;flex-wrap:wrap;margin-bottom:25px;}");
            out.println("h1{font-size:29px;margin-bottom:8px;}");
            out.println(".muted{color:#91a7bf;font-size:14px;}");
            out.println(".btn{display:inline-block;padding:11px 16px;border-radius:8px;text-decoration:none;border:none;cursor:pointer;font-weight:bold;font-size:13px;}");
            out.println(".primary{background:#35c8f0;color:#061321;}");
            out.println(".secondary{background:#193653;color:#e8f0fa;}");
            out.println(".filterbox,.tablebox{background:#0b1f36;border:1px solid #1b3855;border-radius:14px;padding:20px;margin-bottom:22px;}");
            out.println(".filters{display:grid;grid-template-columns:2fr 1fr 1fr 1fr auto;gap:12px;}");
            out.println("input,select{width:100%;padding:11px;background:#081321;border:1px solid #294766;border-radius:7px;color:#e8f0fa;outline:none;}");
            out.println("input:focus,select:focus{border-color:#35c8f0;}");
            out.println(".tablewrap{overflow-x:auto;}");
            out.println("table{width:100%;border-collapse:collapse;min-width:850px;}");
            out.println("th,td{text-align:left;padding:15px 12px;border-bottom:1px solid #1b3855;font-size:13px;}");
            out.println("th{color:#91a7bf;font-size:12px;text-transform:uppercase;}");
            out.println("td{color:#dce8f5;}");
            out.println(".badge{display:inline-block;padding:5px 9px;border-radius:20px;font-size:11px;background:#193653;color:#cde8fa;}");
            out.println(".high{background:#512b35;color:#ffb5bd;}");
            out.println(".medium{background:#514322;color:#ffdf8c;}");
            out.println(".low{background:#17483f;color:#9bf2d6;}");
            out.println(".actions{display:flex;gap:8px;align-items:center;}");
            out.println(".edit{color:#53d8fb;text-decoration:none;}");
            out.println(".delete{color:#ff929c;text-decoration:none;}");
            out.println(".empty{text-align:center;padding:35px;color:#91a7bf;}");
            out.println(".toast{position:fixed;top:22px;right:22px;z-index:9999;background:#123f35;color:#b8ffe3;border:1px solid #2cc995;padding:15px 22px;border-radius:10px;box-shadow:0 10px 30px #0005;font-size:14px;font-weight:bold;animation:slideIn .3s ease;}");
            out.println("@keyframes slideIn{from{transform:translateY(-12px);opacity:0;}to{transform:translateY(0);opacity:1;}}");
            out.println("@media(max-width:850px){.filters{grid-template-columns:1fr 1fr;}.navbar{align-items:flex-start;}.heading{align-items:flex-start;}}");
            out.println("@media(max-width:520px){.filters{grid-template-columns:1fr;}h1{font-size:24px;}.container{margin:24px auto;}.toast{left:15px;right:15px;top:15px;text-align:center;}}");

            out.println("</style>");
            out.println("</head>");
            out.println("<body>");

            if (successMessage != null) {
                out.println("<div class='toast' id='successToast'>");
                out.println("&#10003; " + successMessage);
                out.println("</div>");
            }

            out.println("<header class='navbar'>");
            out.println("<div class='brand'>Deadline Radar</div>");
            out.println("<nav class='navlinks'>");
            out.println("<a href='dashboard'>Dashboard</a>");
            out.println("<a href='viewDeadlines'>Deadlines</a>");
            out.println("<a href='calendar.html'>Calendar</a>");
            out.println("<a href='analytics.html'>Analytics</a>");
            out.println("<a href='logout'>Logout</a>");
            out.println("</nav>");
            out.println("</header>");

            out.println("<main class='container'>");

            out.println("<div class='heading'>");
            out.println("<div><h1>My Deadlines</h1>");
            out.println("<p class='muted'>Search, filter, and manage your deadlines.</p></div>");
            out.println("<a class='btn primary' href='addDeadline.html'>+ Add Deadline</a>");
            out.println("</div>");

            out.println("<section class='filterbox'>");
            out.println("<form method='get' action='viewDeadlines' class='filters'>");

            out.println("<input type='text' name='search' placeholder='Search deadlines...' value='" +
                    escapeHtml(search) + "'>");

            out.println("<select name='category'>");
            out.println("<option value=''>All Categories</option>");
            printOption(out, "Assignment", category);
            printOption(out, "Exam", category);
            printOption(out, "Project", category);
            printOption(out, "Personal", category);
            printOption(out, "Other", category);
            out.println("</select>");

            out.println("<select name='priority'>");
            out.println("<option value=''>All Priorities</option>");
            printOption(out, "High", priority);
            printOption(out, "Medium", priority);
            printOption(out, "Low", priority);
            out.println("</select>");

            out.println("<select name='status'>");
            out.println("<option value=''>All Statuses</option>");
            printOption(out, "Pending", status);
            printOption(out, "Completed", status);
            out.println("</select>");

            out.println("<button class='btn primary' type='submit'>Apply</button>");
            out.println("</form>");
            out.println("</section>");

            StringBuilder sql = new StringBuilder(
                    "SELECT ID, TITLE, DESCRIPTION, CATEGORY, DUE_DATE, PRIORITY, STATUS " +
                    "FROM DR_DEADLINES WHERE USER_ID = ?");

            if (!search.trim().isEmpty()) {
                sql.append(" AND (LOWER(TITLE) LIKE ? OR LOWER(DESCRIPTION) LIKE ?)");
            }

            if (!category.isEmpty()) {
                sql.append(" AND CATEGORY = ?");
            }

            if (!priority.isEmpty()) {
                sql.append(" AND PRIORITY = ?");
            }

            if (!status.isEmpty()) {
                sql.append(" AND STATUS = ?");
            }

            sql.append(" ORDER BY DUE_DATE ASC NULLS LAST");

            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(sql.toString())) {

                int index = 1;
                statement.setInt(index++, userId);

                if (!search.trim().isEmpty()) {
                    String pattern = "%" + search.toLowerCase() + "%";
                    statement.setString(index++, pattern);
                    statement.setString(index++, pattern);
                }

                if (!category.isEmpty()) {
                    statement.setString(index++, category);
                }

                if (!priority.isEmpty()) {
                    statement.setString(index++, priority);
                }

                if (!status.isEmpty()) {
                    statement.setString(index++, status);
                }

                try (ResultSet rs = statement.executeQuery()) {

                    out.println("<section class='tablebox'>");
                    out.println("<div class='tablewrap'>");
                    out.println("<table>");
                    out.println("<thead><tr>");
                    out.println("<th>Title</th><th>Category</th><th>Due Date</th>");
                    out.println("<th>Priority</th><th>Status</th><th>Actions</th>");
                    out.println("</tr></thead><tbody>");

                    boolean hasRows = false;

                    while (rs.next()) {
                        hasRows = true;

                        int id = rs.getInt("ID");
                        String title = rs.getString("TITLE");
                        String description = rs.getString("DESCRIPTION");
                        String rowCategory = rs.getString("CATEGORY");
                        java.sql.Date dueDate = rs.getDate("DUE_DATE");
                        String rowPriority = rs.getString("PRIORITY");
                        String rowStatus = rs.getString("STATUS");

                        out.println("<tr>");

                        out.println("<td><strong>" +
                                escapeHtml(title) + "</strong>");

                        if (description != null && !description.trim().isEmpty()) {
                            out.println("<br><span class='muted'>" +
                                    escapeHtml(description) + "</span>");
                        }

                        out.println("</td>");

                        out.println("<td>" + escapeHtml(rowCategory) + "</td>");

                        out.println("<td>" +
                                (dueDate == null ? "—" : dueDate.toString()) +
                                "</td>");

                        String priorityClass = "";
                        if ("High".equalsIgnoreCase(rowPriority)) {
                            priorityClass = "high";
                        } else if ("Medium".equalsIgnoreCase(rowPriority)) {
                            priorityClass = "medium";
                        } else if ("Low".equalsIgnoreCase(rowPriority)) {
                            priorityClass = "low";
                        }

                        out.println("<td><span class='badge " +
                                priorityClass + "'>" +
                                escapeHtml(rowPriority) + "</span></td>");

                        out.println("<td><span class='badge'>" +
                                escapeHtml(rowStatus) + "</span></td>");

                        out.println("<td><div class='actions'>");
                        out.println("<a class='edit' href='editDeadline?id=" +
                                id + "'>Edit</a>");

                        out.println("<a class='delete' href='deleteDeadline?id=" +
                                id +
                                "' onclick=\"return confirm('Are you sure you want to delete this deadline?');\">Delete</a>");

                        out.println("</div></td>");
                        out.println("</tr>");
                    }

                    if (!hasRows) {
                        out.println("<tr><td colspan='6' class='empty'>");
                        out.println("No deadlines found. Add a deadline or change your filters.");
                        out.println("</td></tr>");
                    }

                    out.println("</tbody></table>");
                    out.println("</div>");
                    out.println("</section>");
                }

            } catch (Exception e) {
                log("Error retrieving deadlines.", e);
                out.println("<section class='tablebox'>");
                out.println("<p class='empty'>Unable to load deadlines right now. Please try again.</p>");
                out.println("</section>");
            }

            out.println("</main>");

            out.println("<script>");
            out.println("const toast = document.getElementById('successToast');");
            out.println("if (toast) {");
            out.println("  setTimeout(() => {");
            out.println("    toast.style.opacity = '0';");
            out.println("    toast.style.transition = 'opacity .4s ease';");
            out.println("    setTimeout(() => toast.remove(), 400);");
            out.println("  }, 3000);");
            out.println("  const url = new URL(window.location.href);");
            out.println("  url.searchParams.delete('added');");
            out.println("  url.searchParams.delete('updated');");
            out.println("  url.searchParams.delete('deleted');");
            out.println("  window.history.replaceState({}, document.title, url.pathname + url.search);");
            out.println("}");
            out.println("</script>");

            out.println("</body>");
            out.println("</html>");
        }
    }

    private void printOption(PrintWriter out, String value, String selected) {
        out.println("<option value='" + escapeHtml(value) + "'" +
                (value.equals(selected) ? " selected" : "") + ">" +
                escapeHtml(value) + "</option>");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
