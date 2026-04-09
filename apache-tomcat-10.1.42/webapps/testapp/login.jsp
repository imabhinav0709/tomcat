<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - TestApp</title>
</head>
<body>
    <h1>Login</h1>
    <p>Use demo credentials: <strong>admin / admin123</strong></p>

    <% if (request.getParameter("error") != null) { %>
        <p style="color: red;">Invalid username or password. Try again.</p>
    <% } %>

    <% if (request.getParameter("logout") != null) { %>
        <p style="color: green;">You have been logged out successfully.</p>
    <% } %>

    <form action="login" method="post">
        <p>
            <label for="username">Username:</label>
            <input type="text" id="username" name="username" required>
        </p>
        <p>
            <label for="password">Password:</label>
            <input type="password" id="password" name="password" required>
        </p>
        <p>
            <button type="submit">Sign In</button>
        </p>
    </form>

    <p><a href="index.html">Back to Home</a></p>
</body>
</html>
