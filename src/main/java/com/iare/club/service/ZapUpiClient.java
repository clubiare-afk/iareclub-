package com.iare.club.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ZapUpiClient {
    private final HttpClient http;
    private final ObjectMapper json;
    private final String apiKey;
    private final URI createOrderUri;
    private final URI orderStatusUri;
    private final Duration readTimeout;

    public ZapUpiClient(ObjectMapper json,
            @Value("${zapupi.api-key:}") String apiKey,
            @Value("${zapupi.api-base-url:https://pay.zapupi.com/api}") String apiBaseUrl,
            @Value("${zapupi.connect-timeout:5s}") Duration connectTimeout,
            @Value("${zapupi.read-timeout:15s}") Duration readTimeout) {
        this.json = json;
        this.apiKey = apiKey.strip();
        String base = apiBaseUrl.replaceAll("/$", "");
        if (!base.startsWith("https://")) throw new IllegalStateException("ZapUPI API base URL must use HTTPS");
        this.createOrderUri = URI.create(base + "/create-order");
        this.orderStatusUri = URI.create(base + "/order-status");
        this.readTimeout = readTimeout;
        this.http = HttpClient.newBuilder().connectTimeout(connectTimeout).version(HttpClient.Version.HTTP_1_1).build();
    }

    public CreatedOrder createOrder(String orderId, BigDecimal amount, String remark,
            String successUrl, String failedUrl, String timeoutUrl, String webhookUrl) {
        requireConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("zap_key", apiKey);
        body.put("order_id", orderId);
        body.put("amount", amount.toPlainString());
        body.put("remark", remark);
        body.put("success_url", successUrl);
        body.put("failed_url", failedUrl);
        body.put("timeout_url", timeoutUrl);
        body.put("webhook_url", webhookUrl);
        JsonNode root = post(createOrderUri, body);
        if (!"success".equalsIgnoreCase(text(root, "status")))
            throw new IllegalStateException(safeMessage(root, "ZapUPI rejected the payment order"));
        String returnedOrderId = text(root, "order_id");
        if (!orderId.equals(returnedOrderId)) throw new IllegalStateException("ZapUPI returned a mismatched order ID");
        String paymentUrl = text(root, "payment_url");
        if (paymentUrl == null || !paymentUrl.startsWith("https://pay.zapupi.com/"))
            throw new IllegalStateException("ZapUPI returned an invalid payment URL");
        String paymentData=text(root,"payment_data");if(paymentData==null||paymentData.isBlank())paymentData=text(root,"upi_button");
        return new CreatedOrder(returnedOrderId, text(root, "txn_id"), text(root, "environment"), paymentUrl,
                text(root,"pay_id"), paymentData);
    }

    public Verification orderStatus(String orderId) {
        requireConfigured();
        JsonNode root = post(orderStatusUri, Map.of("zap_key", apiKey, "order_id", orderId));
        if (!"success".equalsIgnoreCase(text(root, "status"))) return Verification.notFound(orderId, safeMessage(root, "Order not found"));
        JsonNode data = root.path("data");
        String returnedOrderId = text(data, "order_id");
        if (!orderId.equals(returnedOrderId)) throw new IllegalStateException("ZapUPI returned a mismatched order ID");
        return new Verification(returnedOrderId, text(data, "status"), decimal(data, "amount"),
                decimal(data, "pay_amount"), text(data, "txn_id"), text(data, "utr"),
                text(data, "environment"), text(root, "message"));
    }

    public boolean isConfigured() { return !apiKey.isBlank(); }
    private void requireConfigured() { if (apiKey.isBlank()) throw new IllegalStateException("ZAPUPI_API_KEY is not configured"); }

    private JsonNode post(URI uri, Object body) {
        try {
            String requestBody = json.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(readTimeout)
                    .header("Content-Type", "application/json").header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 500)
                throw new IllegalStateException("ZapUPI returned HTTP " + response.statusCode());
            return json.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ZapUPI request was interrupted", e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("ZapUPI request failed", e);
        }
    }

    private static String text(JsonNode node, String field) { JsonNode value = node.get(field); return value == null || value.isNull() ? null : value.asText(); }
    private static BigDecimal decimal(JsonNode node, String field) { try { String value = text(node, field); return value == null ? null : new BigDecimal(value); } catch (NumberFormatException e) { return null; } }
    private static String safeMessage(JsonNode root, String fallback) { String message = text(root, "message"); return message == null || message.isBlank() ? fallback : message; }

    public record CreatedOrder(String orderId, String transactionId, String environment, String paymentUrl,
            String payId, String encodedPaymentData) {}
    public record Verification(String orderId, String status, BigDecimal amount, BigDecimal paidAmount,
            String transactionId, String utr, String environment, String message) {
        static Verification notFound(String id, String message) { return new Verification(id, "NotFound", null, null, null, null, null, message); }
        public boolean successful() { return "Success".equalsIgnoreCase(status); }
        public boolean pending() { return "Pending".equalsIgnoreCase(status) || "NotFound".equalsIgnoreCase(status); }
    }
}
