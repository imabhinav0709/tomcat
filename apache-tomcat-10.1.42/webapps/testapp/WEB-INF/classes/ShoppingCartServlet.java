import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShoppingCartServlet extends HttpServlet {

    private static final List<Product> CATALOG = List.of(
            new Product("P1001", "Wireless Mouse", 19.99),
            new Product("P1002", "USB Keyboard", 29.99),
            new Product("P1003", "HD Monitor", 149.99),
            new Product("P1004", "Laptop Stand", 39.99)
    );

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        writeStateResponse(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(true);
        Map<String, Integer> cart = getCart(session);

        String action = request.getParameter("action");
        String productId = request.getParameter("productId");

        try {
            if ("add".equalsIgnoreCase(action)) {
                int quantity = parseQuantity(request.getParameter("quantity"), 1);
                cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
            } else if ("update".equalsIgnoreCase(action)) {
                int quantity = parseQuantity(request.getParameter("quantity"), 1);
                if (quantity <= 0) {
                    cart.remove(productId);
                } else {
                    cart.put(productId, quantity);
                }
            } else if ("remove".equalsIgnoreCase(action)) {
                cart.remove(productId);
            } else if ("clear".equalsIgnoreCase(action)) {
                cart.clear();
            }

            session.setAttribute("cart", cart);
            writeStateResponse(request, response);
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.print("{\"success\":false,\"message\":\"" + escapeJson(ex.getMessage()) + "\"}");
            }
        }
    }

    private void writeStateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(true);
        Map<String, Integer> cart = getCart(session);

        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.print(buildStateJson(cart));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getCart(HttpSession session) {
        Object existing = session.getAttribute("cart");
        if (existing instanceof Map<?, ?> map) {
            Map<String, Integer> cart = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                cart.put(String.valueOf(entry.getKey()), Integer.valueOf(String.valueOf(entry.getValue())));
            }
            return cart;
        }

        Map<String, Integer> cart = new LinkedHashMap<>();
        session.setAttribute("cart", cart);
        return cart;
    }

    private String buildStateJson(Map<String, Integer> cart) {
        List<CartLine> lines = new ArrayList<>();
        double total = 0.0;
        int itemCount = 0;

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Product product = findProduct(entry.getKey());
            if (product == null) {
                continue;
            }

            int quantity = Math.max(entry.getValue(), 0);
            double lineTotal = roundMoney(product.price() * quantity);
            total += lineTotal;
            itemCount += quantity;
            lines.add(new CartLine(product.id(), product.name(), product.price(), quantity, lineTotal));
        }

        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"success\":true,");
        json.append("\"products\":").append(productsJson());
        json.append(',');
        json.append("\"cart\":").append(cartJson(lines));
        json.append(',');
        json.append("\"itemCount\":").append(itemCount);
        json.append(',');
        json.append("\"total\":").append(formatMoney(total));
        json.append('}');
        return json.toString();
    }

    private String productsJson() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < CATALOG.size(); i++) {
            Product product = CATALOG.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{')
                .append("\"id\":\"").append(escapeJson(product.id())).append("\",")
                .append("\"name\":\"").append(escapeJson(product.name())).append("\",")
                .append("\"price\":").append(formatMoney(product.price()))
                .append('}');
        }
        json.append(']');
        return json.toString();
    }

    private String cartJson(List<CartLine> lines) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < lines.size(); i++) {
            CartLine line = lines.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{')
                .append("\"id\":\"").append(escapeJson(line.id())).append("\",")
                .append("\"name\":\"").append(escapeJson(line.name())).append("\",")
                .append("\"price\":").append(formatMoney(line.price())).append(',')
                .append("\"quantity\":").append(line.quantity()).append(',')
                .append("\"lineTotal\":").append(formatMoney(line.lineTotal()))
                .append('}');
        }
        json.append(']');
        return json.toString();
    }

    private Product findProduct(String productId) {
        for (Product product : CATALOG) {
            if (product.id().equals(productId)) {
                return product;
            }
        }
        return null;
    }

    private int parseQuantity(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private String formatMoney(double value) {
        return String.format(java.util.Locale.US, "%.2f", roundMoney(value));
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private record Product(String id, String name, double price) {}

    private record CartLine(String id, String name, double price, int quantity, double lineTotal) {}
}
