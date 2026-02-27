package org.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;

public class Main {
    // Server runs on this port (http://localhost:8080)
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        // Create and start the HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API endpoint to handle conversions
        server.createContext("/api/convert", (exchange) -> handleConversion(exchange));

        // Serve HTML file from browser
        server.createContext("/", (exchange) -> handleRoot(exchange));

        server.start();
        System.out.println("Server running at http://localhost:" + PORT);
        System.out.println("Open http://localhost:" + PORT + " in your browser");
        System.out.println("Press Ctrl+C to stop");
    }

    // Method called when /api/convert is accessed
    private static void handleConversion(HttpExchange exchange) throws IOException {
        // Allow requests from HTML page (CORS)
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Content-Type", "application/json");

        // Handle preflight requests
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        // Only handle POST requests
        if (!("POST".equals(exchange.getRequestMethod()))) {
            sendError(exchange, "Only POST requests allowed");
            return;
        }

        try {
            // Read the data sent from HTML
            String requestBody = readRequest(exchange);
            JSONObject json = new JSONObject(requestBody);

            // Extract the values from JSON
            double value = json.getDouble("value");
            String from = json.getString("from").toLowerCase();
            String to = json.getString("to").toLowerCase();
            String type = json.getString("type").toLowerCase();

            // Do the conversion
            double result = convert(value, from, to, type);

            // Create and send response
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("result", String.format("%.2f", result));
            response.put("display", value + " " + from.toUpperCase() + " = " +
                    String.format("%.2f", result) + to.toUpperCase());

            sendResponse(exchange, response.toString());

        } catch (Exception e) {
            // If something goes wrong, send error
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("error", e.getMessage());
            sendError(exchange, error.toString());
        }
    }

    // Convert between units - decides which type of conversion to do
    private static double convert(double value, String from, String to, String type) {
        if (type.equals("length")) {
            return convertLength(value, from, to);
        } else if (type.equals("weight")) {
            return convertWeight(value, from, to);
        } else if (type.equals("temperature")) {
            return convertTemperature(value, from, to);
        }
        return 0;
    }

    // Convert between length units (mm, cm, m, km, in, ft, yd, mile)
    private static double convertLength(double value, String from, String to) {
        // First, convert to meters (common unit)
        double meters = 0;

        if (from.equals("mm")) meters = value * 0.001;
        else if (from.equals("cm")) meters = value * 0.01;
        else if (from.equals("m")) meters = value;
        else if (from.equals("km")) meters = value * 1000;
        else if (from.equals("in")) meters = value * 0.0254;
        else if (from.equals("ft")) meters = value * 0.3048;
        else if (from.equals("yd")) meters = value * 0.9144;
        else if (from.equals("mile")) meters = value * 1609.34;

        // Then convert from meters to target unit
        if (to.equals("mm")) return meters / 0.001;
        else if (to.equals("cm")) return meters / 0.01;
        else if (to.equals("m")) return meters;
        else if (to.equals("km")) return meters / 1000;
        else if (to.equals("in")) return meters / 0.0254;
        else if (to.equals("ft")) return meters / 0.3048;
        else if (to.equals("yd")) return meters / 0.9144;
        else if (to.equals("mile")) return meters / 1609.34;

        return 0;
    }

    // Convert between weight units (mg, g, kg, oz, lb, ton)
    private static double convertWeight(double value, String from, String to) {
        // First, convert to grams (common unit)
        double grams = 0;

        if (from.equals("mg")) grams = value * 0.001;
        else if (from.equals("g")) grams = value;
        else if (from.equals("kg")) grams = value * 1000;
        else if (from.equals("oz")) grams = value * 28.3495;
        else if (from.equals("lb")) grams = value * 453.592;
        else if (from.equals("ton")) grams = value * 1000000;

        // Then convert from grams to target unit
        if (to.equals("mg")) return grams / 0.001;
        else if (to.equals("g")) return grams;
        else if (to.equals("kg")) return grams / 1000;
        else if (to.equals("oz")) return grams / 28.3495;
        else if (to.equals("lb")) return grams / 453.592;
        else if (to.equals("ton")) return grams / 1000000;

        return 0;
    }

    // Convert between temperature units (C, F, K)
    private static double convertTemperature(double value, String from, String to) {
        // Step 1: Convert to Celsius
        double celsius = 0;

        if (from.equals("c")) {
            celsius = value;
        } else if (from.equals("f")) {
            celsius = (value - 32) * 5 / 9.0;
        } else if (from.equals("k")) {
            celsius = value - 273.15;
        }

        // Step 2: Convert from Celsius to target
        if (to.equals("c")) {
            return celsius;
        } else if (to.equals("f")) {
            return (celsius * 9 / 5.0) + 32;
        } else if (to.equals("k")) {
            return celsius + 273.15;
        }

        return 0;
    }

    // Read the data sent from the HTML page
    private static String readRequest(HttpExchange exchange) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    // Send a response to the HTML page
    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // Send an error response
    private static void sendError(HttpExchange exchange, String error) throws IOException {
        byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(400, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // Handle requests to the root path - serve HTML
    private static void handleRoot(HttpExchange exchange) throws IOException {
        try {
            // Load HTML file from resources folder
            String htmlContent = loadHtmlFile();

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            byte[] bytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (Exception e) {
            String response = "Error loading page";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // Load HTML file from resources folder
    private static String loadHtmlFile() throws IOException {
        try {
            // Try loading from JAR resources
            InputStream is = Main.class.getResourceAsStream("/converter.html");
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                return sb.toString();
            }
        } catch (Exception e) {
            // Fall back to file system
            System.out.println("Warning: Could not load from JAR, trying file system...");
        }

        // Fall back to file system (for development)
        try {
            return new String(Files.readAllBytes(Paths.get("src/main/resources/converter.html")), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<html><body>HTML file not found</body></html>";
        }
    }
}
