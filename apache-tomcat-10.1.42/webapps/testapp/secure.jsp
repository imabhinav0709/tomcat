<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Secure Page - TestApp</title>
</head>
<body>
    <h1>Secure JSP Page</h1>
    <p>This page is protected by AuthFilter and only available to authenticated users.</p>

    <p>Logged in as: <strong><%= session.getAttribute("user") %></strong></p>

    <p><a href="students">Go to Student Servlet</a></p>
    <p><a href="books">Go to Books Servlet (Hibernate)</a></p>
    <p><a href="product.jsp?productId=P1001">Go to Product Detail Page</a></p>
    <p><a href="cart.jsp">Go to Shopping Cart (JavaScript + Servlets)</a></p>
    <p><a href="logout">Logout</a></p>
</body>
</html>
