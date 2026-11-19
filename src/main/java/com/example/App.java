package com.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class App {

    private static List<Item> items = new ArrayList<>();
    private static int nextId = 1;

    public static void main(String[] args) throws IOException, InterruptedException {

        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0
        );

        // Open the website
        server.createContext("/", exchange -> {
            if (exchange.getRequestURI().getPath().equals("/")) {
                InputStream input = App.class.getResourceAsStream(
                    "/static/index.html"
                );

                if (input == null) {
                    send(exchange, 404, "HTML file not found",
                         "text/plain");
                    return;
                }

                byte[] data = input.readAllBytes();
                input.close();

                exchange.getResponseHeaders().set(
                    "Content-Type", "text/html; charset=UTF-8"
                );

                exchange.sendResponseHeaders(200, data.length);
                exchange.getResponseBody().write(data);
                exchange.close();
            } else {
                send(exchange, 404, "Page not found", "text/plain");
            }
        });

        // Add and display items
        server.createContext("/api/items", exchange -> {

            if (exchange.getRequestMethod().equals("GET")) {
                send(exchange, 200, getItemsJson(items),
                     "application/json");
            }

            else if (exchange.getRequestMethod().equals("POST")) {

                Map<String, String> data = readForm(exchange);

                String name = data.getOrDefault("name", "").trim();
                String type = data.getOrDefault("type", "").trim();
                String category = data.getOrDefault("category", "").trim();
                String location = data.getOrDefault("location", "").trim();
                String contact = data.getOrDefault("contact", "").trim();

                if (name.isEmpty() || type.isEmpty()
                    || category.isEmpty() || location.isEmpty()
                    || contact.isEmpty()) {

                    send(exchange, 400, "{\"error\":\"All fields are required\"}",
                         "application/json");
                    return;
                }

                Item item = new Item(
                    nextId++, name, type, category, location, contact
                );

                items.add(item);

                send(exchange, 201, "{\"message\":\"Item added successfully\"}",
                     "application/json");
            }

            else {
                send(exchange, 405, "Method not allowed", "text/plain");
            }
        });

        // Search items by name
        server.createContext("/api/search", exchange -> {

            String query = getQuery(exchange.getRequestURI())
                .getOrDefault("name", "").toLowerCase();

            List<Item> results = new ArrayList<>();

            for (Item item : items) {
                if (item.getName().toLowerCase().contains(query)) {
                    results.add(item);
                }
            }

            send(exchange, 200, getItemsJson(results),
                 "application/json");
        });

        // Mark an item as returned
        server.createContext("/api/return", exchange -> {

            String idText = getQuery(exchange.getRequestURI())
                .get("id");

            if (idText == null) {
                send(exchange, 400, "{\"error\":\"Item ID required\"}",
                     "application/json");
                return;
            }

            try {
                int id = Integer.parseInt(idText);

                for (Item item : items) {
                    if (item.getId() == id) {
                        item.setReturned(true);

                        send(exchange, 200,
                             "{\"message\":\"Item marked as returned\"}",
                             "application/json");
                        return;
                    }
                }

                send(exchange, 404, "{\"error\":\"Item not found\"}",
                     "application/json");

            } catch (NumberFormatException e) {
                send(exchange, 400, "{\"error\":\"Invalid item ID\"}",
                     "application/json");
            }
        });

        server.start();

        System.out.println("Digital Lost-and-Found Notice Board started!");
        System.out.println("Open: http://localhost:8080");
        new java.util.concurrent.CountDownLatch(1).await();
    }

    // Read form data
    private static Map<String, String> readForm(
        HttpExchange exchange
    ) throws IOException {

        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        return parseParameters(body);
    }

    // Convert URL parameters into a map
    private static Map<String, String> parseParameters(String query) {

        Map<String, String> result = new HashMap<>();

        if (query == null || query.isEmpty()) {
            return result;
        }

        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }

            String[] parts = pair.split("=", 2);

            try {
                String key = decodeUrlComponent(parts[0]);
                String value = parts.length > 1
                    ? decodeUrlComponent(parts[1])
                    : "";

                result.put(key, value);

            } catch (IllegalArgumentException e) {
                // Ignore malformed parameter
            }
        }

        return result;
    }

    private static String decodeUrlComponent(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Malformed URL parameter", e);
        }
    }

    private static Map<String, String> getQuery(URI uri) {
        return parseParameters(uri.getRawQuery());
    }

    // Convert items into JSON
    private static String getItemsJson(List<Item> list) {

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Item item : list) {

            if (!first) {
                json.append(",");
            }

            first = false;

            json.append("{")
                .append("\"id\":").append(item.getId()).append(",")
                .append("\"name\":\"").append(escape(item.getName())).append("\",")
                .append("\"type\":\"").append(escape(item.getType())).append("\",")
                .append("\"category\":\"").append(escape(item.getCategory())).append("\",")
                .append("\"location\":\"").append(escape(item.getLocation())).append("\",")
                .append("\"contact\":\"").append(escape(item.getContact())).append("\",")
                .append("\"returned\":").append(item.isReturned())
                .append("}");
        }

        json.append("]");
        return json.toString();
    }

    // Escape special characters in JSON text
    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // Send HTTP response
    private static void send(
        HttpExchange exchange,
        int status,
        String response,
        String contentType
    ) throws IOException {

        byte[] data = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", contentType + "; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
}
