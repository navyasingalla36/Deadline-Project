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

@WebServlet("/editDeadline")
public class EditDeadlineServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        // =====================================================
        // SESSION CHECK
        // =====================================================

        HttpSession session = request.getSession(false);

        if (session == null ||
            session.getAttribute("userId") == null) {

            response.sendRedirect("login.html");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        // =====================================================
        // GET DEADLINE ID
        // =====================================================

        String idParameter = request.getParameter("id");

        if (idParameter == null ||
            idParameter.trim().isEmpty()) {

            response.sendRedirect("viewDeadlines");
            return;
        }

        int deadlineId;

        try {

            deadlineId = Integer.parseInt(idParameter);

        } catch (NumberFormatException e) {

            response.sendRedirect("viewDeadlines");
            return;
        }

        // =====================================================
        // DATABASE QUERY
        // =====================================================

        String sql =
                "SELECT ID, TITLE, DESCRIPTION, CATEGORY, " +
                "DUE_DATE, PRIORITY, STATUS, RECURRENCE " +
                "FROM DR_DEADLINES " +
                "WHERE ID = ? AND USER_ID = ?";

        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, deadlineId);
            statement.setInt(2, userId);

            ResultSet result = statement.executeQuery();

            // =================================================
            // DEADLINE NOT FOUND
            // =================================================

            if (!result.next()) {

                showNotFoundPage(out);

                return;
            }

            // =================================================
            // READ DATABASE VALUES
            // =================================================

            int id = result.getInt("ID");

            String title = result.getString("TITLE");

            String description =
                    result.getString("DESCRIPTION");

            String category =
                    result.getString("CATEGORY");

            String priority =
                    result.getString("PRIORITY");

            String status =
                    result.getString("STATUS");

            String recurrence =
                    result.getString("RECURRENCE");

            if (recurrence == null ||
                recurrence.trim().isEmpty()) {

                recurrence = "None";
            }

            java.sql.Date dueDate =
                    result.getDate("DUE_DATE");

            String dueDateValue = "";

            if (dueDate != null) {

                dueDateValue =
                        dueDate.toLocalDate().toString();
            }

            // =================================================
            // HTML START
            // =================================================

            out.println("<!DOCTYPE html>");

            out.println("<html lang='en'>");

            out.println("<head>");

            out.println(
                    "<meta charset='UTF-8'>"
            );

            out.println(
                    "<meta name='viewport' " +
                    "content='width=device-width, initial-scale=1.0'>"
            );

            out.println(
                    "<title>Deadline Radar | Edit Deadline</title>"
            );

            // =================================================
            // CSS
            // =================================================

            out.println("<style>");

            out.println("""
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            html {
                scroll-behavior: smooth;
            }

            body {
                font-family:
                    Inter,
                    -apple-system,
                    BlinkMacSystemFont,
                    "Segoe UI",
                    sans-serif;

                background:
                    radial-gradient(
                        circle at top left,
                        rgba(6, 182, 212, 0.10),
                        transparent 30%
                    ),
                    radial-gradient(
                        circle at top right,
                        rgba(37, 99, 235, 0.12),
                        transparent 30%
                    ),
                    #07111f;

                color: #f4f8ff;
                min-height: 100vh;
            }

            a {
                text-decoration: none;
            }

            /* ===============================================
               NAVBAR
               =============================================== */

            .navbar {
                position: sticky;
                top: 0;
                z-index: 1000;

                background:
                    rgba(7, 17, 31, 0.94);

                backdrop-filter: blur(18px);

                border-bottom:
                    1px solid rgba(148, 163, 184, 0.14);

                box-shadow:
                    0 8px 30px rgba(0, 0, 0, 0.25);
            }

            .nav-container {
                max-width: 1400px;
                min-height: 74px;

                margin: auto;
                padding: 0 28px;

                display: flex;
                align-items: center;
                justify-content: space-between;

                gap: 25px;
            }

            .brand {
                display: flex;
                align-items: center;

                gap: 11px;

                color: white;

                white-space: nowrap;
            }

            .brand-icon {
                width: 43px;
                height: 43px;

                border-radius: 13px;

                display: flex;
                align-items: center;
                justify-content: center;

                font-size: 20px;

                background:
                    linear-gradient(
                        135deg,
                        #06b6d4,
                        #2563eb
                    );

                box-shadow:
                    0 0 24px rgba(6, 182, 212, 0.28);
            }

            .brand-name {
                font-size: 20px;
                font-weight: 800;
            }

            .brand-name span {
                color: #22d3ee;
            }

            .nav-links {
                display: flex;
                align-items: center;

                gap: 6px;

                overflow-x: auto;

                scrollbar-width: none;
            }

            .nav-links::-webkit-scrollbar {
                display: none;
            }

            .nav-link {
                padding: 10px 14px;

                border-radius: 10px;

                color: #aab9cb;

                font-size: 13px;
                font-weight: 650;

                white-space: nowrap;

                transition:
                    background 0.2s,
                    color 0.2s,
                    transform 0.2s;
            }

            .nav-link:hover {
                color: white;

                background:
                    rgba(34, 211, 238, 0.08);

                transform: translateY(-1px);
            }

            .nav-link.active {
                color: white;

                background:
                    linear-gradient(
                        135deg,
                        rgba(6, 182, 212, 0.18),
                        rgba(37, 99, 235, 0.18)
                    );

                border:
                    1px solid rgba(34, 211, 238, 0.18);
            }

            .nav-link.logout {
                color: #fca5a5;
            }

            .nav-link.logout:hover {
                background:
                    rgba(239, 68, 68, 0.10);
            }

            /* ===============================================
               MAIN
               =============================================== */

            .container {
                width: min(1000px, calc(100% - 56px));

                margin: auto;

                padding:
                    34px 0 55px;
            }

            /* ===============================================
               HERO
               =============================================== */

            .page-hero {
                position: relative;

                overflow: hidden;

                padding: 28px 30px;

                border-radius: 21px;

                background:
                    linear-gradient(
                        135deg,
                        rgba(15, 34, 58, 0.98),
                        rgba(9, 27, 47, 0.95)
                    );

                border:
                    1px solid rgba(148, 163, 184, 0.14);

                box-shadow:
                    0 18px 45px rgba(0, 0, 0, 0.22);

                margin-bottom: 22px;
            }

            .page-hero::after {
                content: "";

                position: absolute;

                width: 230px;
                height: 230px;

                border-radius: 50%;

                right: -90px;
                top: -130px;

                background:
                    rgba(34, 211, 238, 0.07);
            }

            .hero-content {
                position: relative;
                z-index: 1;

                display: flex;

                align-items: center;

                gap: 17px;
            }

            .hero-icon {
                width: 55px;
                height: 55px;

                flex-shrink: 0;

                border-radius: 16px;

                display: flex;
                align-items: center;
                justify-content: center;

                font-size: 25px;

                background:
                    linear-gradient(
                        135deg,
                        rgba(6, 182, 212, 0.18),
                        rgba(37, 99, 235, 0.20)
                    );

                border:
                    1px solid rgba(34, 211, 238, 0.16);
            }

            .hero-label {
                display: inline-flex;

                padding: 5px 10px;

                border-radius: 20px;

                color: #67e8f9;

                background:
                    rgba(34, 211, 238, 0.08);

                border:
                    1px solid rgba(34, 211, 238, 0.16);

                font-size: 9px;

                font-weight: 800;

                letter-spacing: 0.8px;

                margin-bottom: 7px;
            }

            .page-title {
                font-size: 30px;

                font-weight: 800;

                letter-spacing: -0.8px;

                margin-bottom: 5px;
            }

            .page-title span {
                color: #22d3ee;
            }

            .page-description {
                color: #91a4b9;

                font-size: 12px;

                line-height: 1.6;
            }

            /* ===============================================
               FORM CARD
               =============================================== */

            .form-card {
                padding: 27px;

                border-radius: 18px;

                background:
                    rgba(13, 29, 49, 0.96);

                border:
                    1px solid rgba(148, 163, 184, 0.13);

                box-shadow:
                    0 12px 32px rgba(0, 0, 0, 0.18);
            }

            .form-heading {
                margin-bottom: 21px;

                padding-bottom: 16px;

                border-bottom:
                    1px solid rgba(148, 163, 184, 0.09);
            }

            .form-heading h3 {
                font-size: 15px;

                margin-bottom: 5px;
            }

            .form-heading p {
                color: #71849b;

                font-size: 11px;
            }

            .form-grid {
                display: grid;

                grid-template-columns:
                    1fr 1fr;

                gap: 19px;
            }

            .form-group {
                display: flex;

                flex-direction: column;
            }

            .full-width {
                grid-column: 1 / -1;
            }

            label {
                color: #d6e4f1;

                font-size: 11px;

                font-weight: 750;

                margin-bottom: 7px;
            }

            .required {
                color: #f87171;
            }

            input,
            select,
            textarea {
                width: 100%;

                padding: 11px 12px;

                border:
                    1px solid rgba(148, 163, 184, 0.16);

                border-radius: 9px;

                background:
                    rgba(5, 15, 27, 0.70);

                color: #eaf3ff;

                font-family: inherit;

                font-size: 12px;

                outline: none;

                transition:
                    border-color 0.2s,
                    box-shadow 0.2s;
            }

            input:focus,
            select:focus,
            textarea:focus {
                border-color:
                    rgba(34, 211, 238, 0.55);

                box-shadow:
                    0 0 0 3px rgba(34, 211, 238, 0.07);
            }

            input::placeholder,
            textarea::placeholder {
                color: #657990;
            }

            select option {
                background: #0d1d31;
                color: #f4f8ff;
            }

            textarea {
                min-height: 115px;

                resize: vertical;

                line-height: 1.5;
            }

            .help-text {
                margin-top: 6px;

                color: #71849b;

                font-size: 10px;

                line-height: 1.5;
            }

            /* ===============================================
               RECURRENCE INFO
               =============================================== */

            .recurrence-info {
                margin-top: 9px;

                padding: 11px 13px;

                border-radius: 9px;

                color: #9edee8;

                background:
                    rgba(6, 182, 212, 0.07);

                border:
                    1px solid rgba(6, 182, 212, 0.12);

                font-size: 10px;

                line-height: 1.55;
            }

            /* ===============================================
               BUTTONS
               =============================================== */

            .button-row {
                display: flex;

                align-items: center;

                gap: 10px;

                margin-top: 25px;

                padding-top: 20px;

                border-top:
                    1px solid rgba(148, 163, 184, 0.09);
            }

            .update-button,
            .cancel-button {
                display: inline-flex;

                align-items: center;
                justify-content: center;

                min-height: 42px;

                padding: 0 19px;

                border-radius: 9px;

                font-family: inherit;

                font-size: 12px;

                font-weight: 750;

                cursor: pointer;

                transition:
                    transform 0.2s,
                    box-shadow 0.2s,
                    background 0.2s;
            }

            .update-button {
                border: none;

                color: white;

                background:
                    linear-gradient(
                        135deg,
                        #06b6d4,
                        #2563eb
                    );

                box-shadow:
                    0 8px 20px rgba(6, 182, 212, 0.16);
            }

            .update-button:hover {
                transform: translateY(-2px);

                box-shadow:
                    0 12px 26px rgba(6, 182, 212, 0.25);
            }

            .cancel-button {
                color: #aebed0;

                background:
                    rgba(148, 163, 184, 0.07);

                border:
                    1px solid rgba(148, 163, 184, 0.14);
            }

            .cancel-button:hover {
                color: white;

                background:
                    rgba(148, 163, 184, 0.11);

                transform: translateY(-1px);
            }

            /* ===============================================
               NAVIGATION
               =============================================== */

            .navigation {
                display: flex;

                flex-wrap: wrap;

                gap: 9px;

                margin-top: 18px;
            }

            .navigation a {
                display: inline-flex;

                align-items: center;

                padding: 9px 13px;

                border-radius: 9px;

                color: #9fb2c7;

                background:
                    rgba(148, 163, 184, 0.06);

                border:
                    1px solid rgba(148, 163, 184, 0.10);

                font-size: 11px;

                font-weight: 650;
            }

            .navigation a:hover {
                color: #67e8f9;

                border-color:
                    rgba(34, 211, 238, 0.18);
            }

            /* ===============================================
               NOT FOUND / ERROR
               =============================================== */

            .message-card {
                text-align: center;

                padding: 50px 25px;

                border-radius: 18px;

                background:
                    rgba(13, 29, 49, 0.96);

                border:
                    1px solid rgba(148, 163, 184, 0.13);
            }

            .message-icon {
                width: 58px;
                height: 58px;

                margin: 0 auto 15px;

                border-radius: 16px;

                display: flex;
                align-items: center;
                justify-content: center;

                font-size: 24px;

                background:
                    rgba(239, 68, 68, 0.09);
            }

            .message-card h2 {
                font-size: 19px;

                margin-bottom: 7px;
            }

            .message-card p {
                color: #71849b;

                font-size: 12px;

                margin-bottom: 18px;
            }

            .back-button {
                display: inline-flex;

                padding: 10px 15px;

                border-radius: 9px;

                color: white;

                background:
                    linear-gradient(
                        135deg,
                        #06b6d4,
                        #2563eb
                    );

                font-size: 11px;

                font-weight: 750;
            }

            /* ===============================================
               FOOTER
               =============================================== */

            .footer {
                text-align: center;

                color: #52667d;

                font-size: 10px;

                margin-top: 27px;
            }

            /* ===============================================
               RESPONSIVE
               =============================================== */

            @media (max-width: 850px) {

                .nav-container {
                    flex-wrap: wrap;

                    padding:
                        12px 18px;
                }

                .nav-links {
                    width: 100%;

                    order: 3;
                }

                .container {
                    width:
                        calc(100% - 36px);
                }
            }

            @media (max-width: 650px) {

                .container {
                    width:
                        calc(100% - 28px);

                    padding-top: 20px;
                }

                .form-card {
                    padding: 20px;
                }

                .page-hero {
                    padding: 21px;
                }

                .hero-content {
                    align-items: flex-start;
                }

                .page-title {
                    font-size: 25px;
                }

                .form-grid {
                    grid-template-columns: 1fr;
                }

                .full-width {
                    grid-column: auto;
                }

                .button-row {
                    flex-direction: column;
                }

                .update-button,
                .cancel-button {
                    width: 100%;
                }

                .navigation {
                    flex-direction: column;
                }

                .navigation a {
                    justify-content: center;
                }
            }

            @media (max-width: 450px) {

                .brand-name {
                    font-size: 17px;
                }

                .hero-icon {
                    width: 48px;
                    height: 48px;

                    font-size: 21px;
                }

                .hero-content {
                    gap: 12px;
                }

                .form-card {
                    padding: 17px;
                }
            }
            """);

            out.println("</style>");

            out.println("</head>");

            // =================================================
            // BODY
            // =================================================

            out.println("<body>");

            // =================================================
            // NAVBAR
            // =================================================

            out.println("<nav class='navbar'>");

            out.println("<div class='nav-container'>");

            out.println(
                    "<a href='dashboard' class='brand'>"
            );

            out.println(
                    "<div class='brand-icon'>⏰</div>"
            );

            out.println(
                    "<div class='brand-name'>"
                    + "Deadline <span>Radar</span>"
                    + "</div>"
            );

            out.println("</a>");

            out.println("<div class='nav-links'>");

            out.println(
                    "<a href='dashboard' class='nav-link'>"
                    + "⌂ Dashboard"
                    + "</a>"
            );

            out.println(
                    "<a href='viewDeadlines' "
                    + "class='nav-link active'>"
                    + "📋 Deadlines"
                    + "</a>"
            );

            out.println(
                    "<a href='calendar.html' class='nav-link'>"
                    + "📅 Calendar"
                    + "</a>"
            );

            out.println(
                    "<a href='analytics.html' class='nav-link'>"
                    + "📊 Analytics"
                    + "</a>"
            );

            out.println(
                    "<a href='logout' class='nav-link logout'>"
                    + "↪ Logout"
                    + "</a>"
            );

            out.println("</div>");

            out.println("</div>");

            out.println("</nav>");

            // =================================================
            // MAIN CONTAINER
            // =================================================

            out.println("<main class='container'>");

            // =================================================
            // HERO
            // =================================================

            out.println("<section class='page-hero'>");

            out.println("<div class='hero-content'>");

            out.println(
                    "<div class='hero-icon'>✎</div>"
            );

            out.println("<div>");

            out.println(
                    "<div class='hero-label'>"
                    + "DEADLINE MANAGEMENT"
                    + "</div>"
            );

            out.println(
                    "<h1 class='page-title'>"
                    + "Edit <span>Deadline</span>"
                    + "</h1>"
            );

            out.println(
                    "<p class='page-description'>"
                    + "Update the details of your existing deadline."
                    + "</p>"
            );

            out.println("</div>");

            out.println("</div>");

            out.println("</section>");

            // =================================================
            // FORM CARD
            // =================================================

            out.println("<section class='form-card'>");

            out.println("<div class='form-heading'>");

            out.println(
                    "<h3>📝 Deadline Details</h3>"
            );

            out.println(
                    "<p>"
                    + "Modify the information below and save your changes."
                    + "</p>"
            );

            out.println("</div>");

            out.println(
                    "<form action='updateDeadline' "
                    + "method='post' "
                    + "onsubmit='return validateForm()'>"
            );

            // Hidden ID

            out.println(
                    "<input type='hidden' "
                    + "name='id' "
                    + "value='" + id + "'>"
            );

            out.println("<div class='form-grid'>");

            // =================================================
            // TITLE
            // =================================================

            out.println(
                    "<div class='form-group full-width'>"
            );

            out.println(
                    "<label for='title'>"
                    + "Deadline Title "
                    + "<span class='required'>*</span>"
                    + "</label>"
            );

            out.println(
                    "<input type='text' "
                    + "id='title' "
                    + "name='title' "
                    + "maxlength='150' "
                    + "required "
                    + "placeholder='Enter deadline title' "
                    + "value='"
                    + escapeHtml(title)
                    + "'>"
            );

            out.println("</div>");

            // =================================================
            // CATEGORY
            // =================================================

            out.println("<div class='form-group'>");

            out.println(
                    "<label for='category'>"
                    + "Category "
                    + "<span class='required'>*</span>"
                    + "</label>"
            );

            out.println(
                    "<select id='category' "
                    + "name='category' required>"
            );

            out.println(
                    "<option value=''>"
                    + "Select Category"
                    + "</option>"
            );

            printOption(out, "Assignment", category);
            printOption(out, "Exam", category);
            printOption(out, "Project", category);
            printOption(out, "Application", category);
            printOption(out, "Certification", category);
            printOption(out, "Other", category);

            out.println("</select>");

            out.println("</div>");

            // =================================================
            // PRIORITY
            // =================================================

            out.println("<div class='form-group'>");

            out.println(
                    "<label for='priority'>"
                    + "Priority "
                    + "<span class='required'>*</span>"
                    + "</label>"
            );

            out.println(
                    "<select id='priority' "
                    + "name='priority' required>"
            );

            out.println(
                    "<option value=''>"
                    + "Select Priority"
                    + "</option>"
            );

            printOption(out, "High", priority);
            printOption(out, "Medium", priority);
            printOption(out, "Low", priority);

            out.println("</select>");

            out.println("</div>");

            // =================================================
            // DUE DATE
            // =================================================

            out.println("<div class='form-group'>");

            out.println(
                    "<label for='dueDate'>"
                    + "Due Date "
                    + "<span class='required'>*</span>"
                    + "</label>"
            );

            out.println(
                    "<input type='date' "
                    + "id='dueDate' "
                    + "name='dueDate' "
                    + "required "
                    + "value='"
                    + dueDateValue
                    + "'>"
            );

            out.println(
                    "<span class='help-text'>"
                    + "Set the date by which this task should be completed."
                    + "</span>"
            );

            out.println("</div>");

            // =================================================
            // STATUS
            // =================================================

            out.println("<div class='form-group'>");

            out.println(
                    "<label for='status'>"
                    + "Status "
                    + "<span class='required'>*</span>"
                    + "</label>"
            );

            out.println(
                    "<select id='status' "
                    + "name='status' required>"
            );

            out.println(
                    "<option value='Pending' "
                    + selected(status, "Pending")
                    + ">Pending</option>"
            );

            out.println(
                    "<option value='Completed' "
                    + selected(status, "Completed")
                    + ">Completed</option>"
            );

            out.println("</select>");

            out.println(
                    "<span class='help-text'>"
                    + "Mark it Completed after finishing the deadline."
                    + "</span>"
            );

            out.println("</div>");

            // =================================================
            // RECURRENCE
            // =================================================

            out.println("<div class='form-group'>");

            out.println(
                    "<label for='recurrence'>"
                    + "Recurrence"
                    + "</label>"
            );

            out.println(
                    "<select id='recurrence' "
                    + "name='recurrence'>"
            );

            out.println(
                    "<option value='None' "
                    + selected(recurrence, "None")
                    + ">None</option>"
            );

            out.println(
                    "<option value='Daily' "
                    + selected(recurrence, "Daily")
                    + ">Daily</option>"
            );

            out.println(
                    "<option value='Weekly' "
                    + selected(recurrence, "Weekly")
                    + ">Weekly</option>"
            );

            out.println(
                    "<option value='Monthly' "
                    + selected(recurrence, "Monthly")
                    + ">Monthly</option>"
            );

            out.println("</select>");

            out.println(
                    "<span class='help-text'>"
                    + "Choose how often this deadline repeats."
                    + "</span>"
            );

            out.println(
                    "<div class='recurrence-info'>"
                    + "↻ When a recurring deadline is marked "
                    + "<strong>Completed</strong>, Deadline Radar "
                    + "automatically creates the next occurrence."
                    + "</div>"
            );

            out.println("</div>");

            // =================================================
            // DESCRIPTION
            // =================================================

            out.println(
                    "<div class='form-group full-width'>"
            );

            out.println(
                    "<label for='description'>"
                    + "Description"
                    + "</label>"
            );

            out.println(
                    "<textarea id='description' "
                    + "name='description' "
                    + "maxlength='500' "
                    + "placeholder='Add additional details...'>"
            );

            out.println(
                    escapeHtml(description)
            );

            out.println("</textarea>");

            out.println(
                    "<span class='help-text'>"
                    + "Maximum 500 characters."
                    + "</span>"
            );

            out.println("</div>");

            out.println("</div>");

            // =================================================
            // BUTTONS
            // =================================================

            out.println("<div class='button-row'>");

            out.println(
                    "<button type='submit' "
                    + "class='update-button'>"
                    + "✓ Save Changes"
                    + "</button>"
            );

            out.println(
                    "<a href='viewDeadlines' "
                    + "class='cancel-button'>"
                    + "Cancel"
                    + "</a>"
            );

            out.println("</div>");

            out.println("</form>");

            out.println("</section>");

            // =================================================
            // NAVIGATION
            // =================================================

            out.println("<div class='navigation'>");

            out.println(
                    "<a href='dashboard'>"
                    + "← Dashboard"
                    + "</a>"
            );

            out.println(
                    "<a href='viewDeadlines'>"
                    + "📋 View Deadlines"
                    + "</a>"
            );

            out.println(
                    "<a href='add-deadline.html'>"
                    + "＋ Add Deadline"
                    + "</a>"
            );

            out.println("</div>");

            // =================================================
            // FOOTER
            // =================================================

            out.println(
                    "<div class='footer'>"
                    + "Deadline Radar • Smart Deadline Management System"
                    + "</div>"
            );

            out.println("</main>");

            // =================================================
            // JAVASCRIPT
            // =================================================

            out.println("<script>");

            out.println("""
            function validateForm() {

                const title =
                    document.getElementById("title")
                    .value.trim();

                const category =
                    document.getElementById("category")
                    .value;

                const priority =
                    document.getElementById("priority")
                    .value;

                const dueDate =
                    document.getElementById("dueDate")
                    .value;

                const status =
                    document.getElementById("status")
                    .value;

                if (title === "") {

                    alert("Please enter a deadline title.");

                    return false;
                }

                if (category === "") {

                    alert("Please select a category.");

                    return false;
                }

                if (priority === "") {

                    alert("Please select a priority.");

                    return false;
                }

                if (dueDate === "") {

                    alert("Please select a due date.");

                    return false;
                }

                if (status === "") {

                    alert("Please select a status.");

                    return false;
                }

                return true;
            }
            """);

            out.println("</script>");

            out.println("</body>");

            out.println("</html>");

        } catch (Exception e) {

            e.printStackTrace();

            showErrorPage(out);
        }
    }

    // ==========================================================
    // NOT FOUND PAGE
    // ==========================================================

    private void showNotFoundPage(PrintWriter out) {

        out.println("""
        <!DOCTYPE html>
        <html lang="en">

        <head>

            <meta charset="UTF-8">

            <meta name="viewport"
                  content="width=device-width, initial-scale=1.0">

            <title>Deadline Radar | Not Found</title>

            <style>

                * {
                    box-sizing: border-box;
                }

                body {
                    margin: 0;

                    min-height: 100vh;

                    display: flex;
                    align-items: center;
                    justify-content: center;

                    font-family:
                        Arial,
                        sans-serif;

                    background:
                        #07111f;

                    color: white;
                }

                .card {
                    width: min(500px, calc(100% - 30px));

                    padding: 40px;

                    text-align: center;

                    border-radius: 18px;

                    background: #0d1d31;

                    border:
                        1px solid rgba(148,163,184,0.14);
                }

                .icon {
                    font-size: 40px;

                    margin-bottom: 15px;
                }

                h2 {
                    margin-bottom: 8px;
                }

                p {
                    color: #8295aa;

                    font-size: 13px;

                    margin-bottom: 22px;
                }

                a {
                    display: inline-block;

                    padding: 11px 17px;

                    border-radius: 9px;

                    color: white;

                    text-decoration: none;

                    background:
                        linear-gradient(
                            135deg,
                            #06b6d4,
                            #2563eb
                        );

                    font-size: 12px;

                    font-weight: bold;
                }

            </style>

        </head>

        <body>

            <div class="card">

                <div class="icon">🔍</div>

                <h2>Deadline Not Found</h2>

                <p>
                    The requested deadline does not exist
                    or you do not have permission to access it.
                </p>

                <a href="viewDeadlines">
                    ← Back to Deadlines
                </a>

            </div>

        </body>

        </html>
        """);
    }

    // ==========================================================
    // ERROR PAGE
    // ==========================================================

    private void showErrorPage(PrintWriter out) {

        out.println("""
        <!DOCTYPE html>
        <html lang="en">

        <head>

            <meta charset="UTF-8">

            <meta name="viewport"
                  content="width=device-width, initial-scale=1.0">

            <title>Deadline Radar | Error</title>

            <style>

                body {
                    margin: 0;

                    min-height: 100vh;

                    display: flex;
                    align-items: center;
                    justify-content: center;

                    font-family: Arial, sans-serif;

                    background: #07111f;

                    color: white;
                }

                .card {
                    width: min(500px, calc(100% - 30px));

                    padding: 40px;

                    text-align: center;

                    border-radius: 18px;

                    background: #0d1d31;

                    border:
                        1px solid rgba(148,163,184,0.14);
                }

                .icon {
                    font-size: 40px;

                    margin-bottom: 15px;
                }

                h2 {
                    margin-bottom: 8px;
                }

                p {
                    color: #8295aa;

                    font-size: 13px;

                    margin-bottom: 22px;
                }

                a {
                    display: inline-block;

                    padding: 11px 17px;

                    border-radius: 9px;

                    color: white;

                    text-decoration: none;

                    background:
                        linear-gradient(
                            135deg,
                            #06b6d4,
                            #2563eb
                        );

                    font-size: 12px;

                    font-weight: bold;
                }

            </style>

        </head>

        <body>

            <div class="card">

                <div class="icon">⚠️</div>

                <h2>Unable to Load Deadline</h2>

                <p>
                    Something went wrong while loading
                    the deadline information.
                </p>

                <a href="viewDeadlines">
                    ← Back to Deadlines
                </a>

            </div>

        </body>

        </html>
        """);
    }

    // ==========================================================
    // PRINT SELECT OPTION
    // ==========================================================

    private void printOption(
            PrintWriter out,
            String value,
            String current) {

        out.println(
                "<option value='"
                + escapeHtml(value)
                + "' "
                + selected(current, value)
                + ">"
                + escapeHtml(value)
                + "</option>"
        );
    }

    // ==========================================================
    // SELECTED OPTION
    // ==========================================================

    private String selected(
            String current,
            String value) {

        if (current != null &&
            current.equals(value)) {

            return "selected";
        }

        return "";
    }

    // ==========================================================
    // HTML ESCAPE
    // ==========================================================

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