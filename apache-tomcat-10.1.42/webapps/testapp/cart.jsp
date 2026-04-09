<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Shopping Cart - TestApp</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            background: #f5f7fb;
            color: #1f2937;
        }
        .container {
            max-width: 1100px;
            margin: 0 auto;
            padding: 24px;
        }
        .grid {
            display: grid;
            grid-template-columns: 1.2fr 1fr;
            gap: 20px;
        }
        .panel {
            background: #fff;
            border-radius: 14px;
            padding: 20px;
            box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
        }
        .products, .cart-table {
            width: 100%;
            border-collapse: collapse;
        }
        .products th, .products td, .cart-table th, .cart-table td {
            border-bottom: 1px solid #e5e7eb;
            padding: 12px 10px;
            text-align: left;
            vertical-align: middle;
        }
        .products th, .cart-table th {
            background: #f8fafc;
        }
        .btn {
            border: 0;
            border-radius: 10px;
            padding: 10px 14px;
            cursor: pointer;
            font-weight: 700;
        }
        .btn-primary { background: #2563eb; color: #fff; }
        .btn-secondary { background: #e2e8f0; color: #0f172a; }
        .btn-danger { background: #dc2626; color: #fff; }
        .qty-input {
            width: 72px;
            padding: 8px;
            border: 1px solid #cbd5e1;
            border-radius: 8px;
        }
        .actions { display: flex; gap: 8px; flex-wrap: wrap; }
        .muted { color: #64748b; }
        .summary {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 12px;
            margin-bottom: 14px;
        }
        .status {
            min-height: 24px;
            margin: 12px 0 0;
            color: #0f766e;
            font-weight: 700;
        }
        .top-links a {
            margin-right: 14px;
        }
        @media (max-width: 900px) {
            .grid { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
<% HttpSession userSession = session; %>
<div class="container">
    <h1>Shopping Cart</h1>
    <p class="muted">This cart updates through JavaScript calls to a servlet, without full-page refreshes.</p>
    <div class="top-links">
        <a href="secure.jsp">Back to Secure Page</a>
        <a href="books">Books</a>
        <a href="students">Students</a>
        <a href="logout">Logout</a>
    </div>
    <p>Logged in as: <strong><%= userSession.getAttribute("user") %></strong></p>

    <div class="grid">
        <div class="panel">
            <div class="summary">
                <h2>Products</h2>
                <button class="btn btn-secondary" onclick="reloadCart()">Refresh Cart State</button>
            </div>
            <table class="products">
                <thead>
                    <tr>
                        <th>Item</th>
                        <th>Price</th>
                        <th>Quantity</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody id="productRows"></tbody>
            </table>
        </div>

        <div class="panel">
            <div class="summary">
                <h2>Your Cart</h2>
                <button class="btn btn-danger" onclick="clearCart()">Clear Cart</button>
            </div>
            <table class="cart-table">
                <thead>
                    <tr>
                        <th>Item</th>
                        <th>Qty</th>
                        <th>Line Total</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody id="cartRows"></tbody>
            </table>
            <h3>Total: $<span id="cartTotal">0.00</span></h3>
            <p>Items in cart: <strong id="itemCount">0</strong></p>
            <div id="status" class="status"></div>
        </div>
    </div>
</div>

<script>
    const apiUrl = '<%= request.getContextPath() %>/cart-api';
    let currentProducts = [];

    function money(value) {
        return Number(value).toFixed(2);
    }

    function setStatus(message, isError = false) {
        const status = document.getElementById('status');
        status.textContent = message;
        status.style.color = isError ? '#b91c1c' : '#0f766e';
    }

    function renderProducts(products) {
        currentProducts = products;
        const rows = products.map(product => `
            <tr>
                <td><a href="product.jsp?productId=${encodeURIComponent(product.id)}">${escapeHtml(product.name)}</a></td>
                <td>$${money(product.price)}</td>
                <td><input class="qty-input" type="number" min="1" value="1" id="qty-${product.id}"></td>
                <td>
                    <button class="btn btn-primary" onclick="addToCart('${product.id}')">Add to Cart</button>
                </td>
            </tr>
        `).join('');
        document.getElementById('productRows').innerHTML = rows;
    }

    function renderCart(cart) {
        const rows = cart.map(item => `
            <tr>
                <td>${escapeHtml(item.name)}</td>
                <td>
                    <input class="qty-input" type="number" min="0" value="${item.quantity}" id="cart-qty-${item.id}">
                </td>
                <td>$${money(item.lineTotal)}</td>
                <td class="actions">
                    <button class="btn btn-secondary" onclick="updateCart('${item.id}')">Update</button>
                    <button class="btn btn-danger" onclick="removeFromCart('${item.id}')">Remove</button>
                </td>
            </tr>
        `).join('');
        document.getElementById('cartRows').innerHTML = rows || '<tr><td colspan="4">Your cart is empty.</td></tr>';
    }

    async function loadCart() {
        const response = await fetch(apiUrl, { credentials: 'same-origin' });
        const data = await response.json();
        if (!data.success) {
            throw new Error(data.message || 'Failed to load cart');
        }
        renderProducts(data.products || []);
        renderCart(data.cart || []);
        document.getElementById('cartTotal').textContent = data.total;
        document.getElementById('itemCount').textContent = data.itemCount;
    }

    async function postCart(payload, successMessage) {
        const formData = new URLSearchParams(payload);
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
        renderCart(data.cart || []);
        document.getElementById('cartTotal').textContent = data.total;
        document.getElementById('itemCount').textContent = data.itemCount;
        setStatus(successMessage || 'Cart updated.');
    }

    async function addToCart(productId) {
        try {
            const qtyField = document.getElementById(`qty-${productId}`);
            const quantity = qtyField ? qtyField.value : 1;
            await postCart({ action: 'add', productId, quantity }, 'Item added to cart.');
        } catch (error) {
            setStatus(error.message, true);
        }
    }

    async function updateCart(productId) {
        try {
            const qtyField = document.getElementById(`cart-qty-${productId}`);
            const quantity = qtyField ? qtyField.value : 1;
            await postCart({ action: 'update', productId, quantity }, 'Cart item updated.');
        } catch (error) {
            setStatus(error.message, true);
        }
    }

    async function removeFromCart(productId) {
        try {
            await postCart({ action: 'remove', productId }, 'Item removed from cart.');
        } catch (error) {
            setStatus(error.message, true);
        }
    }

    async function clearCart() {
        try {
            await postCart({ action: 'clear' }, 'Cart cleared.');
        } catch (error) {
            setStatus(error.message, true);
        }
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    loadCart().catch(error => setStatus(error.message, true));
</script>
</body>
</html>
