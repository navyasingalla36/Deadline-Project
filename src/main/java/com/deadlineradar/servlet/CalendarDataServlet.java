package com.deadlineradar.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

import com.deadlineradar.dao.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/calendarData")
public class CalendarDataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        response.setHeader(
                "Cache-Control",
                "no-cache, no-store, must-revalidate"
        );

        /*
         * Check login session
         */
        HttpSession session =
                request.getSession(false);

        if (session == null ||
            session.getAttribute("userId") == null) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.getWriter().print(
                    "{\"error\":\"Not logged in\"}"
            );

            return;
        }

        /*
         * Get logged-in user ID
         */
        int userId =
                (Integer) session.getAttribute("userId");


        /*
         * Get all deadlines belonging
         * to the logged-in user
         */
        String sql =
                "SELECT ID, TITLE, DUE_DATE, PRIORITY, STATUS " +
                "FROM DR_DEADLINES " +
                "WHERE USER_ID = ? " +
                "ORDER BY DUE_DATE ASC";


        StringBuilder json =
                new StringBuilder();

        json.append("[");

        boolean first = true;


        try (Connection con =
                     DBConnection.getConnection();

             PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    userId
            );


            try (ResultSet rs =
                         ps.executeQuery()) {

                SimpleDateFormat dateFormat =
                        new SimpleDateFormat(
                                "yyyy-MM-dd"
                        );


                while (rs.next()) {

                    int id =
                            rs.getInt("ID");


                    String title =
                            rs.getString("TITLE");


                    java.sql.Date dueDate =
                            rs.getDate("DUE_DATE");


                    String priority =
                            rs.getString("PRIORITY");


                    String status =
                            rs.getString("STATUS");


                    if (!first) {

                        json.append(",");

                    }

                    first = false;


                    json.append("{");


                    json.append("\"id\":");
                    json.append(id);
                    json.append(",");


                    json.append("\"title\":\"");
                    json.append(
                            escapeJson(title)
                    );
                    json.append("\",");


                    json.append("\"dueDate\":\"");
                    json.append(
                            dateFormat.format(
                                    dueDate
                            )
                    );
                    json.append("\",");


                    json.append("\"priority\":\"");
                    json.append(
                            escapeJson(priority)
                    );
                    json.append("\",");


                    json.append("\"status\":\"");
                    json.append(
                            escapeJson(status)
                    );
                    json.append("\"");


                    json.append("}");

                }

            }


        } catch (Exception e) {

            e.printStackTrace();


            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );


            response.getWriter().print(
                    "{\"error\":\"Unable to load calendar data\"}"
            );

            return;
        }


        json.append("]");


        response.getWriter().print(
                json.toString()
        );
    }


    /*
     * Escape characters that could
     * break JSON
     */
    private String escapeJson(String value) {

        if (value == null) {

            return "";

        }


        return value

                .replace("\\", "\\\\")

                .replace("\"", "\\\"")

                .replace("\n", "\\n")

                .replace("\r", "\\r")

                .replace("\t", "\\t");
    }
}