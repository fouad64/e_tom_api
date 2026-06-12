package com.example.troubleticket;

import com.example.troubleticket.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TroubleTicketServer {

    // Thread-safe in-memory database
    private static final Map<String, TroubleTicket> ticketsDb = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Mapped to base path /troubleTicket/v2/troubleTicket
        server.createContext("/troubleTicket/v2/troubleTicket", new TroubleTicketHandler());
        
        server.setExecutor(null); // default executor
        System.out.println("Starting Trouble Ticket API server on port " + port + "...");
        server.start();
        System.out.println("Server started successfully!");
    }

    static class TroubleTicketHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            
            // Set JSON Content-Type response header
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            try {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGet(exchange, path);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handlePost(exchange);
                } else if ("PATCH".equalsIgnoreCase(method)) {
                    handlePatch(exchange, path);
                } else {
                    // Method Not Allowed
                    sendResponse(exchange, 405, new ErrorResponse("405", "Method Not Allowed", "HTTP Method " + method + " not supported"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, new ErrorResponse("500", "Internal Server Error", e.getMessage()));
            }
        }

        // --- GET API ---
        private void handleGet(HttpExchange exchange, String path) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            
            // Case 1: GET /troubleTicket/v2/troubleTicket/{id} (Retrieve single ticket)
            if (path.matches("^/troubleTicket/v2/troubleTicket/[^/]+$")) {
                String id = path.substring(path.lastIndexOf('/') + 1);
                TroubleTicket ticket = ticketsDb.get(id);
                
                if (ticket == null) {
                    sendResponse(exchange, 404, new ErrorResponse("404", "Not Found", "TroubleTicket with id '" + id + "' does not exist"));
                } else {
                    sendResponse(exchange, 200, ticket);
                }
            } 
            // Case 2: GET /troubleTicket/v2/troubleTicket (List all with optional filters)
            else if (path.equals("/troubleTicket/v2/troubleTicket")) {
                Map<String, String> filters = parseQueryParams(query);
                List<TroubleTicket> list = new ArrayList<>(ticketsDb.values());

                if (filters.containsKey("status")) {
                    String statusVal = filters.get("status");
                    list = list.stream().filter(t -> statusVal.equalsIgnoreCase(t.getStatus())).collect(Collectors.toList());
                }
                if (filters.containsKey("severity")) {
                    String sevVal = filters.get("severity");
                    list = list.stream().filter(t -> sevVal.equalsIgnoreCase(t.getSeverity())).collect(Collectors.toList());
                }
                if (filters.containsKey("ticketType")) {
                    String typeVal = filters.get("ticketType");
                    list = list.stream().filter(t -> typeVal.equalsIgnoreCase(t.getTicketType())).collect(Collectors.toList());
                }

                sendResponse(exchange, 200, list);
            } else {
                sendResponse(exchange, 404, new ErrorResponse("404", "Not Found", "Endpoint not found"));
            }
        }

        // --- POST API ---
        private void handlePost(HttpExchange exchange) throws IOException {
            InputStream requestBody = exchange.getRequestBody();
            TroubleTicket ticket;
            
            try {
                ticket = objectMapper.readValue(requestBody, TroubleTicket.class);
            } catch (Exception e) {
                sendResponse(exchange, 400, new ErrorResponse("400", "Bad Request", "Invalid JSON format: " + e.getMessage()));
                return;
            }

            // Simple validation of mandatory fields
            List<String> missingFields = new ArrayList<>();
            if (ticket.getDescription() == null || ticket.getDescription().trim().isEmpty()) missingFields.add("description");
            if (ticket.getSeverity() == null || ticket.getSeverity().trim().isEmpty()) missingFields.add("severity");
            if (ticket.getTicketType() == null || ticket.getTicketType().trim().isEmpty()) missingFields.add("ticketType");

            if (!missingFields.isEmpty()) {
                String fieldsStr = String.join(", ", missingFields);
                sendResponse(exchange, 400, new ErrorResponse("400", "Bad Request", "Mandatory fields [" + fieldsStr + "] are missing or invalid"));
                return;
            }

            // Set dynamic fields
            if (ticket.getId() == null) {
                ticket.setId(UUID.randomUUID().toString());
            }

            // Build href dynamically
            String host = exchange.getRequestHeaders().getFirst("Host");
            if (host == null) host = "localhost:8080";
            ticket.setHref("http://" + host + "/troubleTicket/v2/troubleTicket/" + ticket.getId());

            // Initialize default status and first audit log entry
            if (ticket.getStatus() == null) {
                ticket.setStatus("Acknowledged");
            }
            if (ticket.getStatusChange().isEmpty()) {
                ticket.getStatusChange().add(new StatusChange(ticket.getStatus(), "Ticket created", "TroubleTicket"));
            }

            // Save to DB
            ticketsDb.put(ticket.getId(), ticket);
            sendResponse(exchange, 201, ticket);
        }

        // --- PATCH API ---
        private void handlePatch(HttpExchange exchange, String path) throws IOException {
            if (!path.matches("^/troubleTicket/v2/troubleTicket/[^/]+$")) {
                sendResponse(exchange, 400, new ErrorResponse("400", "Bad Request", "ID is required for PATCH updates"));
                return;
            }

            String id = path.substring(path.lastIndexOf('/') + 1);
            TroubleTicket ticket = ticketsDb.get(id);

            if (ticket == null) {
                sendResponse(exchange, 404, new ErrorResponse("404", "Not Found", "TroubleTicket with id '" + id + "' does not exist"));
                return;
            }

            InputStream requestBody = exchange.getRequestBody();
            Map<String, Object> updates;
            try {
                updates = objectMapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                sendResponse(exchange, 400, new ErrorResponse("400", "Bad Request", "Invalid JSON format: " + e.getMessage()));
                return;
            }

            String originalStatus = ticket.getStatus();
            boolean statusChanged = false;
            String newStatus = null;
            String statusChangeReasonText = null;

            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Silently ignore non-patchable fields
                if (key.equals("id") || key.equals("href") || key.equals("creationDate") || key.equals("lastUpdate") || key.equals("statusChange")) {
                    continue;
                }

                switch (key) {
                    case "name":
                        ticket.setName((String) value);
                        break;
                    case "externalId":
                        ticket.setExternalId((String) value);
                        break;
                    case "ticketType":
                        ticket.setTicketType((String) value);
                        break;
                    case "description":
                        ticket.setDescription((String) value);
                        break;
                    case "severity":
                        ticket.setSeverity((String) value);
                        break;
                    case "priority":
                        ticket.setPriority((String) value);
                        break;
                    case "status":
                        newStatus = (String) value;
                        if (newStatus != null && !newStatus.equalsIgnoreCase(originalStatus)) {
                            statusChanged = true;
                        }
                        break;
                    case "statusChangeReason":
                        statusChangeReasonText = (String) value;
                        ticket.setStatusChangeReason(statusChangeReasonText);
                        break;
                    case "requestedResolutionDate":
                        ticket.setRequestedResolutionDate((String) value);
                        break;
                    case "expectedResolutionDate":
                        ticket.setExpectedResolutionDate((String) value);
                        break;
                    case "resolutionDate":
                        ticket.setResolutionDate((String) value);
                        break;
                    case "channel":
                        if (value == null) {
                            ticket.setChannel(null);
                        } else {
                            ticket.setChannel(objectMapper.convertValue(value, Channel.class));
                        }
                        break;
                    case "note":
                        if (value != null) {
                            List<Note> newNotes = objectMapper.convertValue(value, new TypeReference<List<Note>>() {});
                            ticket.getNote().addAll(newNotes);
                        }
                        break;
                    case "attachment":
                        if (value != null) {
                            List<Attachment> newAttachments = objectMapper.convertValue(value, new TypeReference<List<Attachment>>() {});
                            ticket.getAttachment().addAll(newAttachments);
                        }
                        break;
                    case "relatedParty":
                        if (value != null) {
                            List<RelatedParty> newParties = objectMapper.convertValue(value, new TypeReference<List<RelatedParty>>() {});
                            ticket.getRelatedParty().addAll(newParties);
                        }
                        break;
                    case "relatedEntity":
                        if (value != null) {
                            List<RelatedEntity> newEntities = objectMapper.convertValue(value, new TypeReference<List<RelatedEntity>>() {});
                            ticket.getRelatedEntity().addAll(newEntities);
                        }
                        break;
                    case "ticketRelationship":
                        if (value != null) {
                            List<TicketRelationship> newRelations = objectMapper.convertValue(value, new TypeReference<List<TicketRelationship>>() {});
                            ticket.getTicketRelationship().addAll(newRelations);
                        }
                        break;
                }
            }

            // State Machine Transition Audit log if status changes
            if (statusChanged && newStatus != null) {
                ticket.setStatus(newStatus);
                String reason = statusChangeReasonText != null ? statusChangeReasonText : "Status updated via PATCH";
                ticket.getStatusChange().add(new StatusChange(newStatus, reason, "TroubleTicket"));
            }

            ticket.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
            
            // Save back
            ticketsDb.put(ticket.getId(), ticket);
            sendResponse(exchange, 200, ticket);
        }

        // --- Helper Methods ---
        private void sendResponse(HttpExchange exchange, int statusCode, Object body) throws IOException {
            byte[] responseBytes = objectMapper.writeValueAsBytes(body);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }

        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> result = new HashMap<>();
            if (query == null || query.trim().isEmpty()) {
                return result;
            }
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] idx = pair.split("=");
                if (idx.length == 2) {
                    result.put(idx[0], idx[1]);
                }
            }
            return result;
        }
    }
}
