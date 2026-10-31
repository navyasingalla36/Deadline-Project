package com.deadlineradar.servlet;



import jakarta.servlet.ServletException;

import jakarta.servlet.annotation.WebServlet;

import jakarta.servlet.http.HttpServlet;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.HttpSession;



import com.deadlineradar.dao.DBConnection;



import java.io.IOException;

import java.io.PrintWriter;

import java.sql.Connection;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Date;

import java.util.List;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;



@WebServlet("/dashboard")

public class DashboardServlet extends HttpServlet {



    private static final long serialVersionUID = 1L;



    @Override

    protected void doGet(HttpServletRequest request,

                          HttpServletResponse response)

            throws ServletException, IOException {



        HttpSession session = request.getSession(false);



        if (session == null ||

            session.getAttribute("userId") == null) {



            response.sendRedirect("login.html");

            return;

        }



        int userId = (Integer) session.getAttribute("userId");

        String userName = (String) session.getAttribute("userName");



        int total = 0;

        int pending = 0;

        int completed = 0;

        int highPriority = 0;

        int upcoming = 0;

        int overdue = 0;



        List<Alert> alerts = new ArrayList<>();



        Connection con = null;

        PreparedStatement ps = null;

        ResultSet rs = null;



        try {



            con = DBConnection.getConnection();



            if (con == null) {

                throw new Exception("Database connection failed.");

            }



            /* ==============================

               DASHBOARD STATISTICS

               ============================== */



            String statsSql =

                    "SELECT " +

                    "COUNT(*) AS TOTAL, " +

                    "SUM(CASE WHEN STATUS = 'Pending' THEN 1 ELSE 0 END) AS PENDING, " +

                    "SUM(CASE WHEN STATUS = 'Completed' THEN 1 ELSE 0 END) AS COMPLETED, " +

                    "SUM(CASE WHEN PRIORITY = 'High' THEN 1 ELSE 0 END) AS HIGH_PRIORITY, " +

                    "SUM(CASE WHEN DUE_DATE >= TRUNC(SYSDATE) " +

                    "AND DUE_DATE <= TRUNC(SYSDATE) + 7 " +

                    "AND STATUS = 'Pending' THEN 1 ELSE 0 END) AS UPCOMING, " +

                    "SUM(CASE WHEN STATUS = 'Pending' " +

                    "AND DUE_DATE < TRUNC(SYSDATE) THEN 1 ELSE 0 END) AS OVERDUE " +

                    "FROM DR_DEADLINES " +

                    "WHERE USER_ID = ?";



            ps = con.prepareStatement(statsSql);

            ps.setInt(1, userId);



            rs = ps.executeQuery();



            if (rs.next()) {



                total = rs.getInt("TOTAL");

                pending = rs.getInt("PENDING");

                completed = rs.getInt("COMPLETED");

                highPriority = rs.getInt("HIGH_PRIORITY");

                upcoming = rs.getInt("UPCOMING");

                overdue = rs.getInt("OVERDUE");

            }



            rs.close();

            ps.close();



            /* ==============================

               SMART ALERTS

               ============================== */



            String alertSql =

                    "SELECT TITLE, DUE_DATE, PRIORITY " +

                    "FROM DR_DEADLINES " +

                    "WHERE USER_ID = ? " +

                    "AND STATUS = 'Pending' " +

                    "AND DUE_DATE <= TRUNC(SYSDATE) + 7 " +

                    "ORDER BY DUE_DATE ASC";



            ps = con.prepareStatement(alertSql);

            ps.setInt(1, userId);



            rs = ps.executeQuery();



            while (rs.next()) {



                String title = rs.getString("TITLE");

                Date dueDate = rs.getDate("DUE_DATE");

                String priority = rs.getString("PRIORITY");



                if (dueDate == null) {
                    continue;
                }

                LocalDate today = LocalDate.now();
                LocalDate deadlineDate = new java.sql.Date(dueDate.getTime()).toLocalDate();
                long days = ChronoUnit.DAYS.between(today, deadlineDate);

                String type;
                String icon;
                String message;
                String cssClass;

                if (days < 0) {
                    type = "OVERDUE";
                    icon = "🔴";
                    message = "This deadline is overdue.";
                    cssClass = "danger";
                } else if (days == 0) {
                    type = "DUE TODAY";
                    icon = "🚨";
                    message = "This deadline is due today.";
                    cssClass = "today";
                } else if (days == 1) {
                    type = "DUE TOMORROW";
                    icon = "⚠️";
                    message = "Only one day left to complete this deadline.";
                    cssClass = "warning";
                } else if (days <= 3) {
                    type = "DUE SOON";
                    icon = "⚠️";
                    message = "Due within the next 3 days.";
                    cssClass = "warning";
                } else {
                    type = "UPCOMING";
                    icon = "📅";
                    message = "Upcoming deadline.";
                    cssClass = "upcoming";
                }

                alerts.add(

                        new Alert(

                                title,

                                dueDate,

                                priority,

                                type,

                                icon,

                                message,

                                cssClass

                        )

                );

            }



        } catch (Exception e) {



            e.printStackTrace();



        } finally {



            try {

                if (rs != null) rs.close();

            } catch (Exception ignored) {}



            try {

                if (ps != null) ps.close();

            } catch (Exception ignored) {}



            try {

                if (con != null) con.close();

            } catch (Exception ignored) {}

        }



        /* ==============================

           CALCULATIONS

           ============================== */



        int completionPercentage = 0;



        if (total > 0) {

            completionPercentage =

                    (completed * 100) / total;

        }



        int attentionCount =

                overdue + highPriority;



        String today =

                new SimpleDateFormat(

                        "EEEE, dd MMMM yyyy")

                        .format(new Date());



        /* ==============================

           HTML RESPONSE

           ============================== */



        response.setContentType("text/html;charset=UTF-8");



        PrintWriter out = response.getWriter();



        out.println("<!DOCTYPE html>");

        out.println("<html lang='en'>");



        out.println("<head>");



        out.println("<meta charset='UTF-8'>");



        out.println(

                "<meta name='viewport' " +

                "content='width=device-width, initial-scale=1.0'>"

        );



        out.println(

                "<title>Deadline Radar | Dashboard</title>"

        );



        /* ==============================

           CSS

           ============================== */



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

                    rgba(0, 188, 212, 0.10),

                    transparent 30%

                ),

                radial-gradient(

                    circle at top right,

                    rgba(37, 99, 235, 0.12),

                    transparent 28%

                ),

                #07111f;



            color: #f4f8ff;

            min-height: 100vh;

        }



        a {

            text-decoration: none;

            color: inherit;

        }



        /* ==========================

           NAVBAR

           ========================== */



        .navbar {

            position: sticky;

            top: 0;

            z-index: 1000;



            background:

                rgba(7, 17, 31, 0.92);



            backdrop-filter: blur(18px);



            border-bottom:

                1px solid rgba(148, 163, 184, 0.14);



            box-shadow:

                0 8px 30px rgba(0, 0, 0, 0.25);

        }



        .nav-container {

            max-width: 1400px;

            margin: auto;



            min-height: 76px;



            padding: 0 28px;



            display: flex;

            align-items: center;

            justify-content: space-between;



            gap: 25px;

        }



        .brand {

            display: flex;

            align-items: center;

            gap: 12px;



            white-space: nowrap;

        }



        .brand-icon {

            width: 43px;

            height: 43px;



            border-radius: 13px;



            display: flex;

            align-items: center;

            justify-content: center;



            font-size: 21px;



            background:

                linear-gradient(

                    135deg,

                    #06b6d4,

                    #2563eb

                );



            box-shadow:

                0 0 24px rgba(6, 182, 212, 0.30);

        }



        .brand-text {

            font-size: 20px;

            font-weight: 800;

            letter-spacing: -0.3px;

        }



        .brand-text span {

            color: #22d3ee;

        }



        .nav-links {

            display: flex;

            align-items: center;

            gap: 7px;



            overflow-x: auto;

            scrollbar-width: none;

        }



        .nav-links::-webkit-scrollbar {

            display: none;

        }



        .nav-link {

            padding: 10px 15px;



            border-radius: 10px;



            color: #aebdce;



            font-size: 14px;

            font-weight: 600;



            white-space: nowrap;



            transition:

                background 0.2s,

                color 0.2s,

                transform 0.2s;

        }



        .nav-link:hover {

            color: #ffffff;



            background:

                rgba(34, 211, 238, 0.09);



            transform: translateY(-1px);

        }



        .nav-link.active {

            color: #ffffff;



            background:

                linear-gradient(

                    135deg,

                    rgba(6, 182, 212, 0.20),

                    rgba(37, 99, 235, 0.20)

                );



            border:

                1px solid rgba(34, 211, 238, 0.20);



            box-shadow:

                0 5px 18px rgba(6, 182, 212, 0.08);

        }



        .logout {

            color: #fca5a5;

        }



        .logout:hover {

            color: #fecaca;



            background:

                rgba(239, 68, 68, 0.10);

        }



        /* ==========================

           MAIN

           ========================== */



        .container {

            max-width: 1400px;



            margin: auto;



            padding:

                34px 28px 60px;

        }



        /* ==========================

           HERO

           ========================== */



        .hero {

            position: relative;



            overflow: hidden;



            padding: 32px;



            border-radius: 22px;



            background:

                linear-gradient(

                    135deg,

                    rgba(15, 34, 58, 0.98),

                    rgba(9, 28, 49, 0.95)

                );



            border:

                1px solid rgba(148, 163, 184, 0.15);



            box-shadow:

                0 18px 45px rgba(0, 0, 0, 0.24);



            margin-bottom: 26px;

        }



        .hero::before {

            content: "";



            position: absolute;



            width: 260px;

            height: 260px;



            border-radius: 50%;



            background:

                rgba(34, 211, 238, 0.07);



            filter: blur(5px);



            top: -150px;

            right: -80px;

        }



        .hero-content {

            position: relative;

            z-index: 1;



            display: flex;

            align-items: center;

            justify-content: space-between;



            gap: 25px;

        }



        .hero-label {

            display: inline-flex;



            align-items: center;

            gap: 7px;



            padding: 7px 12px;



            border-radius: 30px;



            background:

                rgba(34, 211, 238, 0.08);



            border:

                1px solid rgba(34, 211, 238, 0.17);



            color: #67e8f9;



            font-size: 12px;

            font-weight: 700;



            margin-bottom: 13px;

        }



        .hero h1 {

            font-size: clamp(26px, 3vw, 38px);



            line-height: 1.15;



            margin-bottom: 9px;



            letter-spacing: -1px;

        }



        .hero h1 span {

            color: #22d3ee;

        }



        .hero p {

            color: #9fb0c4;



            font-size: 14px;



            line-height: 1.6;

        }



        .date-card {

            min-width: 210px;



            padding: 17px 20px;



            border-radius: 15px;



            background:

                rgba(3, 12, 24, 0.42);



            border:

                1px solid rgba(148, 163, 184, 0.14);



            text-align: right;

        }



        .date-label {

            color: #8192a8;



            font-size: 11px;



            text-transform: uppercase;



            letter-spacing: 1px;



            margin-bottom: 5px;

        }



        .date-value {

            color: #e8f4ff;



            font-size: 14px;

            font-weight: 700;

        }



        /* ==========================

           STAT CARDS

           ========================== */



        .section-title {

            font-size: 17px;



            font-weight: 750;



            margin-bottom: 15px;



            color: #eaf3ff;

        }



        .stats-grid {

            display: grid;



            grid-template-columns:

                repeat(5, minmax(0, 1fr));



            gap: 15px;



            margin-bottom: 26px;

        }



        .stat-card {

            position: relative;



            overflow: hidden;



            padding: 20px;



            min-height: 145px;



            border-radius: 17px;



            background:

                rgba(13, 29, 49, 0.94);



            border:

                1px solid rgba(148, 163, 184, 0.13);



            box-shadow:

                0 10px 28px rgba(0, 0, 0, 0.15);



            transition:

                transform 0.2s,

                border-color 0.2s,

                box-shadow 0.2s;

        }



        .stat-card:hover {

            transform: translateY(-4px);



            border-color:

                rgba(34, 211, 238, 0.24);



            box-shadow:

                0 16px 35px rgba(0, 0, 0, 0.25);

        }



        .stat-card::before {

            content: "";



            position: absolute;



            top: 0;

            left: 0;



            width: 100%;

            height: 3px;

        }



        .stat-card.total::before {

            background: #38bdf8;

        }



        .stat-card.pending::before {

            background: #f59e0b;

        }



        .stat-card.completed::before {

            background: #22c55e;

        }



        .stat-card.high::before {

            background: #ef4444;

        }



        .stat-card.upcoming::before {

            background: #a78bfa;

        }



        .stat-top {

            display: flex;

            justify-content: space-between;

            align-items: center;



            margin-bottom: 15px;

        }



        .stat-icon {

            width: 38px;

            height: 38px;



            border-radius: 11px;



            display: flex;

            align-items: center;

            justify-content: center;



            font-size: 17px;

        }



        .total .stat-icon {

            background: rgba(56, 189, 248, 0.12);

        }



        .pending .stat-icon {

            background: rgba(245, 158, 11, 0.12);

        }



        .completed .stat-icon {

            background: rgba(34, 197, 94, 0.12);

        }



        .high .stat-icon {

            background: rgba(239, 68, 68, 0.12);

        }



        .upcoming .stat-icon {

            background: rgba(167, 139, 250, 0.12);

        }



        .stat-number {

            font-size: 31px;



            font-weight: 800;



            line-height: 1;



            margin-bottom: 8px;



            letter-spacing: -1px;

        }



        .stat-label {

            color: #8fa1b7;



            font-size: 12px;



            font-weight: 600;



            text-transform: uppercase;



            letter-spacing: 0.6px;

        }



        /* ==========================

           MAIN GRID

           ========================== */



        .main-grid {

            display: grid;



            grid-template-columns:

                minmax(0, 1.45fr)

                minmax(320px, 0.85fr);



            gap: 20px;



            margin-bottom: 26px;

        }



        .panel {

            background:

                rgba(13, 29, 49, 0.94);



            border:

                1px solid rgba(148, 163, 184, 0.13);



            border-radius: 19px;



            padding: 23px;



            box-shadow:

                0 10px 30px rgba(0, 0, 0, 0.16);

        }



        .panel-header {

            display: flex;



            align-items: center;

            justify-content: space-between;



            gap: 15px;



            margin-bottom: 20px;

        }



        .panel-title {

            font-size: 17px;



            font-weight: 750;



            color: #eef6ff;

        }



        .panel-subtitle {

            color: #7f91a7;



            font-size: 12px;



            margin-top: 4px;

        }



        .view-link {

            color: #67e8f9;



            font-size: 12px;



            font-weight: 700;



            white-space: nowrap;

        }



        .view-link:hover {

            color: #a5f3fc;

        }



        /* ==========================

           ALERTS

           ========================== */



        .alert-list {

            display: flex;



            flex-direction: column;



            gap: 11px;

        }



        .alert-item {

            display: flex;



            align-items: center;



            gap: 13px;



            padding: 14px;



            border-radius: 13px;



            background:

                rgba(5, 15, 27, 0.55);



            border:

                1px solid rgba(148, 163, 184, 0.10);



            transition:

                transform 0.2s,

                background 0.2s;

        }



        .alert-item:hover {

            transform: translateX(3px);



            background:

                rgba(18, 39, 63, 0.75);

        }



        .alert-icon {

            width: 39px;

            height: 39px;



            min-width: 39px;



            display: flex;

            align-items: center;

            justify-content: center;



            border-radius: 11px;



            font-size: 17px;

        }



        .alert-item.danger .alert-icon {

            background: rgba(239, 68, 68, 0.13);

        }



        .alert-item.today .alert-icon {

            background: rgba(249, 115, 22, 0.13);

        }



        .alert-item.warning .alert-icon {

            background: rgba(245, 158, 11, 0.13);

        }



        .alert-item.upcoming .alert-icon {

            background: rgba(59, 130, 246, 0.13);

        }



        .alert-content {

            flex: 1;



            min-width: 0;

        }



        .alert-title {

            font-size: 14px;



            font-weight: 700;



            color: #eaf3ff;



            white-space: nowrap;



            overflow: hidden;



            text-overflow: ellipsis;



            margin-bottom: 4px;

        }



        .alert-message {

            font-size: 12px;



            color: #8295aa;

        }



        .alert-right {

            text-align: right;



            min-width: 100px;

        }



        .alert-badge {

            display: inline-block;



            padding: 4px 8px;



            border-radius: 20px;



            font-size: 9px;



            font-weight: 800;



            letter-spacing: 0.5px;



            margin-bottom: 5px;

        }



        .danger .alert-badge {

            color: #fca5a5;

            background: rgba(239, 68, 68, 0.12);

        }



        .today .alert-badge {

            color: #fdba74;

            background: rgba(249, 115, 22, 0.12);

        }



        .warning .alert-badge {

            color: #fcd34d;

            background: rgba(245, 158, 11, 0.12);

        }



        .upcoming .alert-badge {

            color: #93c5fd;

            background: rgba(59, 130, 246, 0.12);

        }



        .alert-date {

            display: block;



            color: #8fa1b7;



            font-size: 11px;

        }



        .priority {

            display: inline-block;



            margin-top: 4px;



            font-size: 9px;



            font-weight: 700;

        }



        .priority-high {

            color: #fca5a5;

        }



        .priority-medium {

            color: #fcd34d;

        }



        .priority-low {

            color: #86efac;

        }



        .empty-alert {

            padding: 35px 20px;



            text-align: center;

        }



        .empty-icon {

            font-size: 35px;



            margin-bottom: 12px;

        }



        .empty-alert h3 {

            font-size: 15px;



            margin-bottom: 6px;

        }



        .empty-alert p {

            color: #7f91a7;



            font-size: 12px;

        }



        /* ==========================

           PROGRESS

           ========================== */



        .progress-content {

            display: flex;



            align-items: center;

            justify-content: center;



            gap: 28px;



            min-height: 210px;

        }



        .progress-circle {

            position: relative;



            width: 150px;

            height: 150px;



            border-radius: 50%;



            background:

                conic-gradient(

                    #22d3ee <?php?>,

                    rgba(51, 65, 85, 0.55) 0

                );



            display: flex;



            align-items: center;

            justify-content: center;



            box-shadow:

                0 0 35px rgba(34, 211, 238, 0.12);

        }



        .progress-circle::before {

            content: "";



            position: absolute;



            width: 112px;

            height: 112px;



            border-radius: 50%;



            background: #0d1d31;



            border:

                1px solid rgba(148, 163, 184, 0.10);

        }



        .progress-value {

            position: relative;



            z-index: 1;



            text-align: center;

        }



        .progress-number {

            display: block;



            font-size: 29px;



            font-weight: 800;



            color: #f3fbff;

        }



        .progress-label {

            color: #8295aa;



            font-size: 10px;



            text-transform: uppercase;



            letter-spacing: 0.7px;

        }



        .progress-info {

            display: flex;



            flex-direction: column;



            gap: 13px;



            flex: 1;

        }



        .progress-row {

            display: flex;



            align-items: center;

            justify-content: space-between;



            padding-bottom: 10px;



            border-bottom:

                1px solid rgba(148, 163, 184, 0.08);

        }



        .progress-row:last-child {

            border-bottom: none;

        }



        .progress-name {

            color: #9cafc3;



            font-size: 12px;

        }



        .progress-count {

            font-size: 14px;



            font-weight: 750;



            color: #f1f7ff;

        }



        /* ==========================

           PRODUCTIVITY

           ========================== */



        .productivity {

            display: grid;



            grid-template-columns:

                repeat(4, 1fr);



            gap: 14px;



            margin-bottom: 26px;

        }



        .product-card {

            padding: 18px;



            border-radius: 15px;



            background:

                rgba(5, 15, 27, 0.52);



            border:

                1px solid rgba(148, 163, 184, 0.10);

        }



        .product-label {

            color: #8295aa;



            font-size: 11px;



            margin-bottom: 9px;

        }



        .product-value {

            font-size: 23px;



            font-weight: 800;



            color: #edf7ff;

        }



        .product-small {

            color: #72859c;



            font-size: 10px;



            margin-top: 4px;

        }



        .green {

            color: #4ade80;

        }



        .amber {

            color: #fbbf24;

        }



        .red {

            color: #f87171;

        }



        .blue {

            color: #38bdf8;

        }



        /* ==========================

           QUICK ACTIONS

           ========================== */



        .quick-actions {

            display: grid;



            grid-template-columns:

                repeat(4, 1fr);



            gap: 14px;



            margin-bottom: 26px;

        }



        .action-card {

            position: relative;



            padding: 20px;



            border-radius: 16px;



            background:

                linear-gradient(

                    145deg,

                    rgba(17, 39, 65, 0.90),

                    rgba(9, 25, 43, 0.92)

                );



            border:

                1px solid rgba(148, 163, 184, 0.12);



            transition:

                transform 0.2s,

                border-color 0.2s,

                box-shadow 0.2s;

        }



        .action-card:hover {

            transform: translateY(-4px);



            border-color:

                rgba(34, 211, 238, 0.25);



            box-shadow:

                0 15px 30px rgba(0, 0, 0, 0.20);

        }



        .action-icon {

            width: 42px;

            height: 42px;



            border-radius: 12px;



            display: flex;

            align-items: center;

            justify-content: center;



            background:

                rgba(34, 211, 238, 0.10);



            font-size: 19px;



            margin-bottom: 14px;

        }



        .action-card h3 {

            font-size: 14px;



            margin-bottom: 5px;

        }



        .action-card p {

            color: #7f91a7;



            font-size: 11px;



            line-height: 1.5;

        }



        .action-arrow {

            position: absolute;



            right: 18px;

            bottom: 17px;



            color: #38bdf8;



            font-size: 17px;

        }



        /* ==========================

           NOTIFICATION BOX

           ========================== */



        .notification-box {

            display: flex;



            align-items: center;



            gap: 15px;



            padding: 17px 19px;



            border-radius: 15px;



            background:

                linear-gradient(

                    135deg,

                    rgba(6, 182, 212, 0.08),

                    rgba(37, 99, 235, 0.08)

                );



            border:

                1px solid rgba(34, 211, 238, 0.15);



            margin-bottom: 25px;

        }



        .notification-icon {

            width: 40px;

            height: 40px;



            min-width: 40px;



            display: flex;

            align-items: center;

            justify-content: center;



            border-radius: 11px;



            background:

                rgba(34, 211, 238, 0.10);



            font-size: 18px;

        }



        .notification-text {

            flex: 1;

        }



        .notification-text strong {

            display: block;



            font-size: 13px;



            margin-bottom: 3px;

        }



        .notification-text span {

            color: #8194aa;



            font-size: 11px;

        }



        .notification-status {

            color: #4ade80;



            font-size: 11px;



            font-weight: 700;



            white-space: nowrap;

        }



        /* ==========================

           FOOTER

           ========================== */



        .footer {

            text-align: center;



            padding-top: 5px;



            color: #52667d;



            font-size: 11px;

        }



        /* ==========================

           RESPONSIVE

           ========================== */



        @media (max-width: 1150px) {



            .stats-grid {

                grid-template-columns:

                    repeat(3, 1fr);

            }



            .quick-actions {

                grid-template-columns:

                    repeat(2, 1fr);

            }



            .productivity {

                grid-template-columns:

                    repeat(2, 1fr);

            }

        }



        @media (max-width: 900px) {



            .nav-container {

                padding: 0 18px;



                flex-wrap: wrap;



                padding-top: 12px;

                padding-bottom: 12px;

            }



            .nav-links {

                width: 100%;



                order: 3;



                padding-bottom: 2px;

            }



            .main-grid {

                grid-template-columns: 1fr;

            }



            .hero-content {

                align-items: flex-start;



                flex-direction: column;

            }



            .date-card {

                text-align: left;



                width: 100%;

            }

        }



        @media (max-width: 650px) {



            .container {

                padding:

                    22px 15px 40px;

            }



            .nav-container {

                padding-left: 15px;

                padding-right: 15px;

            }



            .brand-text {

                font-size: 17px;

            }



            .hero {

                padding: 23px 20px;

            }



            .hero h1 {

                font-size: 27px;

            }



            .stats-grid {

                grid-template-columns:

                    repeat(2, 1fr);

            }



            .stat-card {

                min-height: 130px;



                padding: 17px;

            }



            .stat-number {

                font-size: 27px;

            }



            .progress-content {

                flex-direction: column;



                padding: 10px 0 5px;

            }



            .progress-info {

                width: 100%;

            }



            .productivity,

            .quick-actions {

                grid-template-columns: 1fr 1fr;

            }



            .panel {

                padding: 18px;

            }



            .alert-item {

                align-items: flex-start;

            }



            .alert-right {

                min-width: 80px;

            }

        }



        @media (max-width: 430px) {



            .stats-grid {

                grid-template-columns: 1fr 1fr;

            }



            .productivity,

            .quick-actions {

                grid-template-columns: 1fr;

            }



            .alert-right {

                min-width: 75px;

            }



            .alert-date {

                font-size: 10px;

            }



            .notification-box {

                align-items: flex-start;



                flex-wrap: wrap;

            }



            .notification-status {

                margin-left: 55px;

            }

        }

        """);



        /*

         * The progress circle needs the percentage

         * generated by Java.

         */



        out.println(

                ".progress-circle {" +

                "background: conic-gradient(" +

                "#22d3ee " + completionPercentage + "%," +

                "rgba(51,65,85,0.55) 0);" +

                "}"

        );



        out.println("</style>");



        out.println("</head>");



        /* ==============================

           BODY

           ============================== */



        out.println("<body>");



        /* ==============================

           NAVBAR

           ============================== */



        out.println("<nav class='navbar'>");



        out.println("<div class='nav-container'>");



        out.println(

                "<a href='dashboard' class='brand'>"

        );



        out.println(

                "<div class='brand-icon'>⏰</div>"

        );



        out.println(

                "<div class='brand-text'>Deadline <span>Radar</span></div>"

        );



        out.println("</a>");



        out.println("<div class='nav-links'>");



        out.println(

                "<a href='dashboard' class='nav-link active'>"

                + "⌂ Dashboard</a>"

        );



        out.println(

                "<a href='viewDeadlines' class='nav-link'>"

                + "📋 Deadlines</a>"

        );



        out.println(

                "<a href='calendar.html' class='nav-link'>"

                + "📅 Calendar</a>"

        );



        out.println(

                "<a href='analytics.html' class='nav-link'>"

                + "📊 Analytics</a>"

        );



        out.println(

                "<a href='logout' class='nav-link logout'>"

                + "↪ Logout</a>"

        );



        out.println("</div>");



        out.println("</div>");



        out.println("</nav>");



        /* ==============================

           MAIN CONTAINER

           ============================== */



        out.println("<main class='container'>");



        /* ==============================

           HERO

           ============================== */



        out.println("<section class='hero'>");



        out.println("<div class='hero-content'>");



        out.println("<div>");



        out.println(

                "<div class='hero-label'>"

                + "✦ SMART DEADLINE MANAGEMENT"

                + "</div>"

        );



        out.println(

                "<h1>Welcome back, <span>"

                + escapeHtml(userName)

                + "</span> 👋</h1>"

        );



        out.println(

                "<p>Stay organized, track your deadlines, "

                + "and keep your academic work under control.</p>"

        );



        out.println("</div>");



        out.println("<div class='date-card'>");



        out.println(

                "<div class='date-label'>Today</div>"

        );



        out.println(

                "<div class='date-value'>"

                + today

                + "</div>"

        );



        out.println("</div>");



        out.println("</div>");



        out.println("</section>");



        /* ==============================

           STATISTICS

           ============================== */



        out.println(

                "<div class='section-title'>At a Glance</div>"

        );



        out.println("<section class='stats-grid'>");



        out.println(

                statCard(

                        "total",

                        "📋",

                        total,

                        "Total Deadlines"

                )

        );



        out.println(

                statCard(

                        "pending",

                        "⏳",

                        pending,

                        "Pending"

                )

        );



        out.println(

                statCard(

                        "completed",

                        "✓",

                        completed,

                        "Completed"

                )

        );



        out.println(

                statCard(

                        "high",

                        "🔥",

                        highPriority,

                        "High Priority"

                )

        );



        out.println(

                statCard(

                        "upcoming",

                        "📅",

                        upcoming,

                        "Upcoming"

                )

        );



        out.println("</section>");



        /* ==============================

           SMART WORKLOAD INSIGHTS

           ============================== */

        out.println("<section class='panel' style='margin-bottom:26px;'>");
        out.println("<div class='panel-header'>");
        out.println("<div>");
        out.println("<div class='panel-title'>🧠 Smart Workload Insights</div>");
        out.println("<div class='panel-subtitle'>A quick recommendation based on your current deadlines</div>");
        out.println("</div>");
        out.println("</div>");

        if (overdue > 0) {
            out.println("<div style='padding:15px 17px;border:1px solid rgba(248,113,113,.28);background:rgba(127,29,29,.16);border-radius:13px;'>");
            out.println("<strong style='color:#fca5a5;'>Priority recommendation: clear overdue work first</strong>");
            out.println("<p style='margin:8px 0 0;color:#cbd5e1;line-height:1.65;'>You have " + overdue + " overdue pending deadline(s). Review them, split large tasks into smaller steps, and complete the most urgent one before starting lower-priority work.</p>");
            out.println("</div>");
        } else if (highPriority > 0) {
            out.println("<div style='padding:15px 17px;border:1px solid rgba(251,191,36,.25);background:rgba(120,53,15,.13);border-radius:13px;'>");
            out.println("<strong style='color:#fcd34d;'>Priority recommendation: plan your high-priority tasks</strong>");
            out.println("<p style='margin:8px 0 0;color:#cbd5e1;line-height:1.65;'>There are " + highPriority + " high-priority pending deadline(s). Schedule these first and check their due dates so urgent work does not become overdue.</p>");
            out.println("</div>");
        } else if (pending == 0) {
            out.println("<div style='padding:15px 17px;border:1px solid rgba(52,211,153,.25);background:rgba(6,95,70,.14);border-radius:13px;'>");
            out.println("<strong style='color:#6ee7b7;'>Great work — no pending deadlines</strong>");
            out.println("<p style='margin:8px 0 0;color:#cbd5e1;line-height:1.65;'>You have no pending deadlines right now. Add upcoming assignments early and use realistic due dates to keep your workload manageable.</p>");
            out.println("</div>");
        } else if (pending >= 5) {
            out.println("<div style='padding:15px 17px;border:1px solid rgba(34,211,238,.24);background:rgba(8,145,178,.10);border-radius:13px;'>");
            out.println("<strong style='color:#67e8f9;'>Workload recommendation: break work into focused sessions</strong>");
            out.println("<p style='margin:8px 0 0;color:#cbd5e1;line-height:1.65;'>You have " + pending + " pending deadlines. Start with the nearest due date, choose one task for your next focused session, and review progress before moving to the next.</p>");
            out.println("</div>");
        } else {
            out.println("<div style='padding:15px 17px;border:1px solid rgba(34,211,238,.24);background:rgba(8,145,178,.10);border-radius:13px;'>");
            out.println("<strong style='color:#67e8f9;'>Your workload looks manageable</strong>");
            out.println("<p style='margin:8px 0 0;color:#cbd5e1;line-height:1.65;'>You have " + pending + " pending deadline(s). Keep them updated and work on the nearest due date first to stay on track.</p>");
            out.println("</div>");
        }

        out.println("<div style='margin-top:14px;'><a href='viewDeadlines' class='view-link'>Review my deadlines →</a></div>");
        out.println("</section>");

        /* ==============================

           MAIN GRID

           ============================== */



        out.println("<section class='main-grid'>");



        /* ALERT PANEL */



        out.println("<div class='panel'>");



        out.println("<div class='panel-header'>");



        out.println("<div>");



        out.println(

                "<div class='panel-title'>"

                + "Smart Deadline Alerts"

                + "</div>"

        );



        out.println(

                "<div class='panel-subtitle'>"

                + "Important deadlines that need your attention"

                + "</div>"

        );



        out.println("</div>");



        out.println(

                "<a href='viewDeadlines' class='view-link'>"

                + "View all →</a>"

        );



        out.println("</div>");



        if (alerts.isEmpty()) {



            out.println("<div class='empty-alert'>");



            out.println(

                    "<div class='empty-icon'>🎉</div>"

            );



            out.println(

                    "<h3>No urgent deadlines</h3>"

            );



            out.println(

                    "<p>You are all caught up for now.</p>"

            );



            out.println("</div>");



        } else {



            out.println("<div class='alert-list'>");



            int displayCount = 0;



            for (Alert alert : alerts) {



                if (displayCount >= 6) {

                    break;

                }



                out.println(

                        "<div class='alert-item "

                        + alert.cssClass

                        + "'>"

                );



                out.println(

                        "<div class='alert-icon'>"

                        + alert.icon

                        + "</div>"

                );



                out.println(

                        "<div class='alert-content'>"

                );



                out.println(

                        "<div class='alert-title'>"

                        + escapeHtml(alert.title)

                        + "</div>"

                );



                out.println(

                        "<div class='alert-message'>"

                        + alert.message

                        + "</div>"

                );



                out.println("</div>");



                out.println("<div class='alert-right'>");



                out.println(

                        "<span class='alert-badge'>"

                        + alert.type

                        + "</span>"

                );



                out.println(

                        "<span class='alert-date'>"

                        + new SimpleDateFormat("dd MMM yyyy")

                                .format(alert.dueDate)

                        + "</span>"

                );



                String priorityClass =

                        "High".equalsIgnoreCase(alert.priority)

                        ? "priority-high"

                        : "Medium".equalsIgnoreCase(alert.priority)

                        ? "priority-medium"

                        : "priority-low";



                out.println(

                        "<span class='priority "

                        + priorityClass

                        + "'>"

                        + escapeHtml(alert.priority)

                        + " Priority</span>"

                );



                out.println("</div>");



                out.println("</div>");



                displayCount++;

            }



            out.println("</div>");

        }



        out.println("</div>");



        /* PROGRESS PANEL */



        out.println("<div class='panel'>");



        out.println("<div class='panel-header'>");



        out.println("<div>");



        out.println(

                "<div class='panel-title'>"

                + "Progress Overview"

                + "</div>"

        );



        out.println(

                "<div class='panel-subtitle'>"

                + "Your overall completion progress"

                + "</div>"

        );



        out.println("</div>");



        out.println("</div>");



        out.println("<div class='progress-content'>");



        out.println("<div class='progress-circle'>");



        out.println("<div class='progress-value'>");



        out.println(

                "<span class='progress-number'>"

                + completionPercentage

                + "%</span>"

        );



        out.println(

                "<span class='progress-label'>Completed</span>"

        );



        out.println("</div>");



        out.println("</div>");



        out.println("<div class='progress-info'>");



        out.println(

                progressRow(

                        "Total deadlines",

                        total

                )

        );



        out.println(

                progressRow(

                        "Completed",

                        completed

                )

        );



        out.println(

                progressRow(

                        "Pending",

                        pending

                )

        );



        out.println(

                progressRow(

                        "Overdue",

                        overdue

                )

        );



        out.println("</div>");



        out.println("</div>");



        out.println("</div>");



        out.println("</section>");



        /* ==============================

           PRODUCTIVITY

           ============================== */



        out.println(

                "<div class='section-title'>Productivity Snapshot</div>"

        );



        out.println("<section class='productivity'>");



        out.println(

                productCard(

                        "Completion Rate",

                        completionPercentage + "%",

                        "Overall progress",

                        "green"

                )

        );



        out.println(

                productCard(

                        "Pending",

                        String.valueOf(pending),

                        "Need to be completed",

                        "amber"

                )

        );



        out.println(

                productCard(

                        "Upcoming",

                        String.valueOf(upcoming),

                        "Within next 7 days",

                        "blue"

                )

        );



        out.println(

                productCard(

                        "Attention Needed",

                        String.valueOf(attentionCount),

                        "High priority + overdue",

                        "red"

                )

        );



        out.println("</section>");



        /* ==============================

           QUICK ACTIONS

           ============================== */



        out.println(

                "<div class='section-title'>Quick Actions</div>"

        );



        out.println("<section class='quick-actions'>");



        out.println(

                actionCard(

                        "add-deadline.html",

                        "➕",

                        "Add Deadline",

                        "Create a new deadline and stay organized."

                )

        );



        out.println(

                actionCard(

                        "viewDeadlines",

                        "📋",

                        "View Deadlines",

                        "Search, update and manage your deadlines."

                )

        );



        out.println(

                actionCard(

                        "calendar.html",

                        "📅",

                        "Open Calendar",

                        "See your deadlines across the month."

                )

        );



        out.println(

                actionCard(

                        "analytics.html",

                        "📊",

                        "View Analytics",

                        "Understand your productivity and workload."

                )

        );



        out.println("</section>");



        /* ==============================

           NOTIFICATION INFO

           ============================== */



        out.println("<div class='notification-box'>");



        out.println(

                "<div class='notification-icon'>🔔</div>"

        );



        out.println("<div class='notification-text'>");



        out.println(

                "<strong>Browser Deadline Notifications</strong>"

        );



        out.println(

                "<span>"

                + "Deadline Radar checks for upcoming deadlines "

                + "while this dashboard is open."

                + "</span>"

        );



        out.println("</div>");



        out.println(

                "<div class='notification-status'>● Active</div>"

        );



        out.println("</div>");



        /* ==============================

           FOOTER

           ============================== */



        out.println(

                "<div class='footer'>"

                + "Deadline Radar • Smart Deadline Management System"

                + "</div>"

        );



        out.println("</main>");



        /* ==============================

           JAVASCRIPT

           ============================== */


        /* ==============================
           JAVASCRIPT
           ============================== */

        out.println("<script>");

        out.println("""
        const NOTIFICATION_INTERVAL = 60 * 1000;
        const NOTIFICATION_COOLDOWN = 60 * 1000;

        function requestNotificationPermission() {
            if (!("Notification" in window)) {
                console.log("Browser notifications are not supported.");
                return;
            }

            if (Notification.permission === "default") {
                Notification.requestPermission()
                    .then(permission => {
                        console.log("Notification permission:", permission);
                    })
                    .catch(error => {
                        console.error("Notification permission failed:", error);
                    });
            }
        }

        function showDeadlineNotification(data) {
            if (!("Notification" in window)) {
                return;
            }

            if (Notification.permission !== "granted") {
                return;
            }

            if (!data || data.id == null) {
                console.warn("Invalid deadline notification:", data);
                return;
            }

            const key = "deadlineRadarNotification_" + data.id;
            const now = Date.now();
            const lastShown = localStorage.getItem(key);

            if (
                lastShown &&
                now - parseInt(lastShown, 10) < NOTIFICATION_COOLDOWN
            ) {
                return;
            }

            // Use the actual deadline title as the notification heading.
            const deadlineTitle =
                typeof data.title === "string" && data.title.trim()
                    ? data.title.trim()
                    : "Deadline Reminder";

            const deadlineMessage =
                data.message || "You have an upcoming deadline.";

            try {
                const notification = new Notification(deadlineTitle, {
                    body: deadlineMessage,
                    tag: "deadline-" + data.id,
                    icon: "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>⏰</text></svg>"
                });

                notification.onclick = function () {
                    window.focus();
                    notification.close();
                };

                localStorage.setItem(key, now.toString());

                console.log(
                    "Notification displayed:",
                    deadlineTitle,
                    deadlineMessage
                );
            } catch (error) {
                console.error("Could not display notification:", error);
            }
        }

        async function checkDeadlines() {
            try {
                const response = await fetch("deadlineNotification", {
                    method: "GET",
                    cache: "no-store",
                    credentials: "same-origin"
                });

                if (!response.ok) {
                    throw new Error("HTTP " + response.status);
                }

                const data = await response.json();

                console.log("Deadline notification response:", data);

                if (Array.isArray(data)) {
                    data.forEach(showDeadlineNotification);
                } else if (data && typeof data === "object") {
                    showDeadlineNotification(data);
                } else {
                    console.error("Unexpected notification response:", data);
                }
            } catch (error) {
                console.error("Deadline notification check failed:", error);
            }
        }

        requestNotificationPermission();

        setTimeout(checkDeadlines, 2000);

        setInterval(checkDeadlines, NOTIFICATION_INTERVAL);
        """);

        out.println("</script>");



        out.println("</body>");



        out.println("</html>");

    }



    /* =========================================================

       STAT CARD

       ========================================================= */



    private String statCard(

            String type,

            String icon,

            int number,

            String label) {



        return

                "<div class='stat-card " + type + "'>" +



                "<div class='stat-top'>" +



                "<div class='stat-icon'>"

                + icon +

                "</div>" +



                "</div>" +



                "<div class='stat-number'>"

                + number +

                "</div>" +



                "<div class='stat-label'>"

                + label +

                "</div>" +



                "</div>";

    }



    /* =========================================================

       PROGRESS ROW

       ========================================================= */



    private String progressRow(

            String name,

            int count) {



        return

                "<div class='progress-row'>" +



                "<span class='progress-name'>"

                + name +

                "</span>" +



                "<span class='progress-count'>"

                + count +

                "</span>" +



                "</div>";

    }



    /* =========================================================

       PRODUCTIVITY CARD

       ========================================================= */



    private String productCard(

            String label,

            String value,

            String small,

            String color) {



        return

                "<div class='product-card'>" +



                "<div class='product-label'>"

                + label +

                "</div>" +



                "<div class='product-value "

                + color +

                "'>"

                + value +

                "</div>" +



                "<div class='product-small'>"

                + small +

                "</div>" +



                "</div>";

    }



    /* =========================================================

       QUICK ACTION CARD

       ========================================================= */



    private String actionCard(

            String link,

            String icon,

            String title,

            String description) {



        return

                "<a href='" + link + "' class='action-card'>" +



                "<div class='action-icon'>"

                + icon +

                "</div>" +



                "<h3>"

                + title +

                "</h3>" +



                "<p>"

                + description +

                "</p>" +



                "<span class='action-arrow'>→</span>" +



                "</a>";

    }



    /* =========================================================

       HTML ESCAPE

       ========================================================= */



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



    /* =========================================================

       ALERT CLASS

       ========================================================= */



    private static class Alert {



        String title;

        Date dueDate;

        String priority;

        String type;

        String icon;

        String message;

        String cssClass;



        Alert(

                String title,

                Date dueDate,

                String priority,

                String type,

                String icon,

                String message,

                String cssClass) {



            this.title = title;

            this.dueDate = dueDate;

            this.priority = priority;

            this.type = type;

            this.icon = icon;

            this.message = message;

            this.cssClass = cssClass;

        }

    }

}