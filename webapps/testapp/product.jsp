<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Product Detail - TestApp</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
            color: #1f2937;
        }
        .container {
            max-width: 900px;
            margin: 0 auto;
            padding: 28px;
        }
        .card {
            background: #fff;
            border-radius: 18px;
            box-shadow: 0 18px 40px rgba(15, 23, 42, 0.10);
            padding: 28px;
        }
        .muted { color: #64748b; }
        .price {
            font-size: 2rem;
            font-weight: 800;
            color: #0f172a;
            margin: 8px 0 18px;
        }
        .controls {
            display: flex;
            gap: 12px;
            flex-wrap: wrap;
            align-items: center;
            margin-top: 20px;
        }
        .qty-input {
            width: 86px;
            padding: 10px;
            border: 1px solid #cbd5e1;
            border-radius: 10px;
            font-size: 1rem;
        }
        .btn {
            border: 0;
            border-radius: 12px;
            padding: 12px 16px;
            cursor: pointer;
            font-weight: 700;
        }
        .btn-primary { background: #2563eb; color: #fff; }
        .btn-secondary { background: #e2e8f0; color: #0f172a; }
        .meta {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 12px;
            margin-top: 20px;
        }
        .meta div {
            background: #f8fafc;
            border-radius: 14px;
            padding: 14px;
        }
        .status {
            margin-top: 16px;
            min-height: 24px;
            font-weight: 700;
        }
        a { color: #2563eb; text-decoration: none; }
        @media (max-width: 700px) {
            .meta { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
<% HttpSession userSession = session; %>
<div class="container">
    <p><a href="cart.jsp">Back to Cart</a> | <a href="secure.jsp">Secure Page</a> | <a href="logout">Logout</a></p>
    <div class="card">
        <p class="muted">Logged in as: <strong><%= userSession.getAttribute("user") %></strong></p>
        <h1 id="productName">Loading product...</h1>
        <div class="price" id="productPrice"></div>
        <p id="productDescription" class="muted"></p>

        <div class="meta">
            <div>
                <strong>Product ID</strong>
                <div id="productIdValue">-</div>
            </div>
            <div>
                <strong>Category</strong>
                <div>Shopping cart demo</div>
            </div>
        </div>

        <div class="controls">
            <label for="quantity"><strong>Quantity:</strong></label>
            <input id="quantity" class="qty-input" type="number" min="1" value="1">
            <button class="btn btn-primary" onclick="addToCart()">Add to Cart</button>
            <button class="btn btn-secondary" onclick="goToCart()">Go to Cart</button>
        </div>

        <div id="status" class="status"></div>
    </div>
</div>

<script>
    const apiUrl = '<%= request.getContextPath() %>/cart-api';
    const selectedProductId = new URLSearchParams(window.location.search).get('productId') || 'P1001';
    let selectedProduct = null;

    function money(value) {
        return Number(value).toFixed(2);
    }

    function setStatus(message, isError = false) {
        const status = document.getElementById('status');
        status.textContent = message;
        status.style.color = isError ? '#b91c1c' : '#0f766e';
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    async function loadProduct() {
        const response = await fetch(apiUrl, { credentials: 'same-origin' });
        const data = await response.json();
        if (!data.success) {
            throw new Error(data.message || 'Unable to load product catalog');
        }

        selectedProduct = (data.products || []).find(product => product.id === selectedProductId) || (data.products || [])[0];
        if (!selectedProduct) {
            throw new Error('Product not found');
        }

        document.getElementById('productName').textContent = selectedProduct.name;
        document.getElementById('productPrice').textContent = '$' + money(selectedProduct.price);
        document.getElementById('productDescription').textContent = 'This item can be added to your session cart without a page refresh.';
        document.getElementById('productIdValue').textContent = selectedProduct.id;
        setStatus('Product loaded.');
    }

    async function addToCart() {
        try {
            const quantity = document.getElementById('quantity').value || 1;
            const formData = new URLSearchParams({ action: 'add', productId: selectedProduct.id, quantity });
            const response = await fetch(apiUrl, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: formData
            });
            const data = await response.json();
            if (!data.success) {
                throw new Error(data.message || 'Cart update failed');
            }
            setStatus(escapeHtml(selectedProduct.name) + ' added to cart.');
        } catch (error) {
            setStatus(error.message, true);
        }
    }

    function goToCart() {
        window.location.href = 'cart.jsp';
    }

    loadProduct().catch(error => setStatus(error.message, true));
</script>
</body>
</html>
