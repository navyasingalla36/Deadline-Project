
package com.deadlineradar.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("userId") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        String logoutMessage = "";
        if ("success".equals(request.getParameter("logout"))) {
            logoutMessage = """
                    <div class="toast success" role="status">
                        <span class="toast-icon">&#10003;</span>
                        <div>
                            <strong>Logged out successfully!</strong>
                            <p>You have been logged out safely. See you again!</p>
                        </div>
                        <button class="toast-close"
                                onclick="this.parentElement.remove()"
                                aria-label="Close">&times;</button>
                    </div>
                    """;
        }

        response.getWriter().println("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport"
                      content="width=device-width, initial-scale=1.0">
                <title>Login | Deadline Radar</title>
                <style>
                    * { box-sizing: border-box; }

                    body {
                        margin: 0;
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                        font-family: Arial, sans-serif;
                        background: #081321;
                        color: #eaf4ff;
                    }

                    .login-box {
                        width: 100%;
                        max-width: 420px;
                        padding: 32px;
                        background: #0b1f36;
                        border: 1px solid #1c3a57;
                        border-radius: 16px;
                        box-shadow: 0 15px 40px #0004;
                    }

                    h1 {
                        margin: 0 0 8px;
                        color: #38bdf8;
                        font-size: 27px;
                    }

                    .subtitle {
                        color: #9bb4cb;
                        margin-bottom: 28px;
                        font-size: 14px;
                        line-height: 1.5;
                    }

                    label {
                        display: block;
                        margin: 17px 0 8px;
                        font-size: 13px;
                        font-weight: bold;
                    }

                    input {
                        width: 100%;
                        padding: 12px;
                        border-radius: 8px;
                        border: 1px solid #31516e;
                        background: #081321;
                        color: white;
                        outline: none;
                    }

                    input:focus { border-color: #38bdf8; }

                    button[type="submit"] {
                        width: 100%;
                        margin-top: 24px;
                        padding: 13px;
                        border: 0;
                        border-radius: 8px;
                        background: #38bdf8;
                        color: #081321;
                        font-weight: bold;
                        cursor: pointer;
                        font-size: 15px;
                    }

                    button[type="submit"]:hover {
                        background: #7dd3fc;
                    }

                    .register {
                        margin-top: 22px;
                        text-align: center;
                        color: #9bb4cb;
                        font-size: 13px;
                    }

                    a {
                        color: #38bdf8;
                        text-decoration: none;
                    }

                    a:hover { text-decoration: underline; }

                    .toast {
                        position: fixed;
                        top: 22px;
                        right: 22px;
                        z-index: 1000;
                        width: min(390px, calc(100% - 32px));
                        display: flex;
                        align-items: flex-start;
                        gap: 12px;
                        padding: 16px;
                        border-radius: 12px;
                        background: #102b3a;
                        border: 1px solid #28516a;
                        box-shadow: 0 12px 35px #0006;
                        animation: slideIn .35s ease-out;
                    }

                    .toast.success { border-left: 4px solid #34d399; }
                    .toast.error { border-left: 4px solid #fb7185; }

                    .toast-icon {
                        display: grid;
                        place-items: center;
                        width: 30px;
                        height: 30px;
                        flex-shrink: 0;
                        border-radius: 50%;
                        background: #14533f;
                        color: #6ee7b7;
                        font-weight: bold;
                    }

                    .toast.error .toast-icon {
                        background: #542637;
                        color: #fda4af;
                    }

                    .toast strong { display: block; margin-top: 2px; }

                    .toast p {
                        margin: 5px 0 0;
                        color: #b5c7d9;
                        font-size: 13px;
                        line-height: 1.5;
                    }

                    .toast-close {
                        margin-left: auto;
                        border: 0;
                        background: transparent;
                        color: #b5c7d9;
                        font-size: 22px;
                        cursor: pointer;
                    }

                    @keyframes slideIn {
                        from { opacity: 0; transform: translateY(-10px); }
                        to { opacity: 1; transform: translateY(0); }
                    }

                    @media (max-width: 480px) {
                        .login-box { padding: 25px; }
                        .toast { top: 12px; right: 12px; }
                    }
                </style>
            </head>
            <body>
                """ + logoutMessage + """
                <main class="login-box">
                    <h1>DEADLINE RADAR</h1>
                    <p class="subtitle">
                        Sign in to manage your deadlines and stay organized.
                    </p>

                    <form action="login" method="post">
                        <label for="email">Email Address</label>
                        <input type="email" id="email" name="email"
                               placeholder="Enter your email"
                               autocomplete="username" required>

                        <label for="password">Password</label>
                        <input type="password" id="password" name="password"
                               placeholder="Enter your password"
                               autocomplete="current-password" required>

                        <button type="submit">Login</button>
                    </form>

                    <p class="register">
                        Don't have an account?
                        <a href="register">Create an account</a>
                    </p>
                </main>

                <script>
                    const logoutToast = document.querySelector('.toast.success');
                    if (logoutToast) {
                        window.history.replaceState(
                            {}, document.title, window.location.pathname
                        );
                        setTimeout(() => {
                            if (logoutToast.isConnected) {
                                logoutToast.remove();
                            }
                        }, 4500);
                    }
                </script>
            </body>
            </html>
            """);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null
                || email.trim().isEmpty() || password.isEmpty()) {
            showLoginError(response,
                    request.getContextPath(),
                    "Please enter both your email and password.");
            return;
        }

        String hashedPassword = hashPassword(password);

        String sql = "SELECT ID, NAME FROM DR_USERS "
                   + "WHERE EMAIL = ? AND PASSWORD = ?";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Database connection failed.");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, email.trim());
                ps.setString(2, hashedPassword);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("ID");
                        String userName = rs.getString("NAME");

                        HttpSession oldSession = request.getSession(false);
                        if (oldSession != null) {
                            oldSession.invalidate();
                        }

                        HttpSession session = request.getSession(true);
                        session.setAttribute("userId", userId);
                        session.setAttribute("userName", userName);
                        session.setMaxInactiveInterval(2 * 60);

                        showLoginSuccess(
                                response,
                                request.getContextPath(),
                                userName);
                    } else {
                        showLoginError(
                                response,
                                request.getContextPath(),
                                "Invalid email or password. Please try again.");
                    }
                }
            }

        } catch (Exception e) {
            log("Error while validating login credentials.", e);
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "A login error occurred. Check the Tomcat console.");
        }
    }

    private void showLoginSuccess(HttpServletResponse response,
                                  String contextPath,
                                  String userName)
            throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        String safeName = escapeHtml(userName == null ? "" : userName);
        String safeContextPath = escapeHtml(contextPath);

        response.getWriter().println("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Login Successful | Deadline Radar</title>
                <style>
                    * { box-sizing: border-box; }
                    body {
                        margin: 0; min-height: 100vh; display: grid;
                        place-items: center; background: #081321;
                        color: #eaf4ff; font-family: Arial, sans-serif;
                    }
                    .toast {
                        position: fixed; top: 24px; right: 24px;
                        width: min(390px, calc(100% - 32px));
                        display: flex; gap: 12px; align-items: flex-start;
                        padding: 17px; border-radius: 12px;
                        background: #102b3a; border: 1px solid #28516a;
                        border-left: 4px solid #34d399;
                        box-shadow: 0 12px 35px #0006;
                        animation: appear .35s ease-out;
                    }
                    .icon {
                        display: grid; place-items: center;
                        width: 30px; height: 30px; border-radius: 50%;
                        background: #14533f; color: #6ee7b7;
                        font-weight: bold; flex-shrink: 0;
                    }
                    p { margin: 6px 0 0; color: #b5c7d9; font-size: 13px; }
                    .loading { color: #9bb4cb; text-align: center; }
                    @keyframes appear {
                        from { opacity: 0; transform: translateY(-10px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                </style>
            </head>
            <body>
                <div class="toast" role="status">
                    <div class="icon">&#10003;</div>
                    <div>
                        <strong>Login successful!</strong>
                        <p>Welcome back, """ + safeName + """
                        . Redirecting you to your dashboard...</p>
                    </div>
                </div>
                <div class="loading">Opening your dashboard...</div>
                <script>
                    setTimeout(function () {
                        window.location.replace(
                            """ + "\"" + safeContextPath + "/dashboard\"" + """
                        );
                    }, 1300);
                </script>
            </body>
            </html>
            """);
    }

    private void showLoginError(HttpServletResponse response,
                                String contextPath,
                                String message)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/html;charset=UTF-8");

        String safeMessage = escapeHtml(message);
        String safeContextPath = escapeHtml(contextPath);

        response.getWriter().println("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Login Failed | Deadline Radar</title>
                <style>
                    * { box-sizing: border-box; }
                    body {
                        margin: 0; min-height: 100vh; display: grid;
                        place-items: center; padding: 20px;
                        background: #081321; color: #eaf4ff;
                        font-family: Arial, sans-serif;
                    }
                    .box {
                        width: 100%; max-width: 420px; padding: 32px;
                        border-radius: 16px; background: #0b1f36;
                        border: 1px solid #1c3a57; text-align: center;
                    }
                    h2 { color: #fb7185; }
                    p { color: #b5c7d9; line-height: 1.6; }
                    a {
                        display: inline-block; padding: 12px 18px;
                        border-radius: 8px; background: #38bdf8;
                        color: #081321; font-weight: bold;
                        text-decoration: none;
                    }
                    .toast {
                        position: fixed; top: 22px; right: 22px;
                        width: min(390px, calc(100% - 32px));
                        padding: 16px; border-radius: 12px;
                        text-align: left; background: #102b3a;
                        border: 1px solid #542637;
                        border-left: 4px solid #fb7185;
                        box-shadow: 0 12px 35px #0006;
                    }
                </style>
            </head>
            <body>
                <div class="toast" role="alert">
                    <strong style="color:#fda4af">Login failed</strong>
                    <p>""" + safeMessage + """
                    	
                    </p>
                </div>
                <main class="box">
                    <h2>Unable to sign in</h2>
                    <p>Please check your details and try again.</p>
                    <a href=\"""" + safeContextPath + """
                    		
                    		/login\">Back to Login</a>
                </main>
            </body>
            </html>
            """);
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    // Keep consistent with the password hashing used during registration.
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(
                    password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
}
