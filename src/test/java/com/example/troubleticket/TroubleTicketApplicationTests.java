package com.example.troubleticket;

import com.example.troubleticket.model.TroubleTicket;
import com.example.troubleticket.model.ErrorResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TroubleTicketApplicationTests {

    private static final int TEST_PORT = 8081;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT + "/troubleTicket/v2/troubleTicket";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    public static void startServer() throws Exception {
        // Start server in background on test port
        String[] args = {};
        System.setProperty("sun.net.httpserver.maxReqHeaders", "100");
        
        // Start server manually for testing
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress(TEST_PORT), 0
        );
        server.createContext("/troubleTicket/v2/troubleTicket", new TroubleTicketServer.TroubleTicketHandler());
        server.start();
        
        // Save server reference to stop it later
        testServer = server;
    }

    private static com.sun.net.httpserver.HttpServer testServer;

    @AfterAll
    public static void stopServer() {
        if (testServer != null) {
            testServer.stop(0);
        }
    }

    @Test
    public void testLifecycle() throws Exception {
        // 1. POST (Create Ticket)
        String createPayload = """
                {
                  "description": "December bill shows VOD charge for movie I could not watch",
                  "severity": "Minor",
                  "ticketType": "Bill Dispute",
                  "name": "VOD charge complaint"
                }
                """;

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createPayload))
                .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createResponse.statusCode());

        TroubleTicket created = objectMapper.readValue(createResponse.body(), TroubleTicket.class);
        assertNotNull(created.getId());
        assertEquals("Acknowledged", created.getStatus());
        assertEquals("Minor", created.getSeverity());
        assertEquals(1, created.getStatusChange().size());
        assertEquals("Ticket created", created.getStatusChange().get(0).getChangeReason());

        String ticketId = created.getId();

        // 2. GET (Retrieve Ticket)
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + ticketId))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponse.statusCode());

        TroubleTicket retrieved = objectMapper.readValue(getResponse.body(), TroubleTicket.class);
        assertEquals(ticketId, retrieved.getId());
        assertEquals("VOD charge complaint", retrieved.getName());

        // 3. GET LIST (List All Tickets)
        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        HttpResponse<String> listResponse = client.send(listRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, listResponse.statusCode());

        List<TroubleTicket> list = objectMapper.readValue(listResponse.body(), new TypeReference<List<TroubleTicket>>() {});
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(t -> t.getId().equals(ticketId)));

        // 4. PATCH (Partial Update)
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "InProgress");
        updates.put("statusChangeReason", "Investigating logs");

        HttpRequest patchRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + ticketId))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updates)))
                .build();

        HttpResponse<String> patchResponse = client.send(patchRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, patchResponse.statusCode());

        TroubleTicket patched = objectMapper.readValue(patchResponse.body(), TroubleTicket.class);
        assertEquals("InProgress", patched.getStatus());
        assertEquals(2, patched.getStatusChange().size());
        assertEquals("Investigating logs", patched.getStatusChange().get(1).getChangeReason());
    }

    @Test
    public void testGetNotFound() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/non-existent-uuid"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());

        ErrorResponse error = objectMapper.readValue(response.body(), ErrorResponse.class);
        assertEquals("404", error.getCode());
        assertEquals("Not Found", error.getReason());
        assertTrue(error.getMessage().contains("does not exist"));
    }

    @Test
    public void testPostValidationError() throws Exception {
        // Missing mandatory ticketType & severity
        String invalidPayload = """
                {
                  "description": "Validation error ticket"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());

        ErrorResponse error = objectMapper.readValue(response.body(), ErrorResponse.class);
        assertEquals("400", error.getCode());
        assertEquals("Bad Request", error.getReason());
        assertTrue(error.getMessage().contains("severity"));
        assertTrue(error.getMessage().contains("ticketType"));
    }
}
