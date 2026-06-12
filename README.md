# 🎫 TMF621 Trouble Ticket API (Pure Java)

A lightweight, high-performance, and beginner-friendly implementation of the **TMF621 Trouble Ticket REST API** specification (Release 18.0.0).

This project uses **zero frameworks** (no Spring Boot) and is built using the standard JDK `HttpServer` and the lightweight **Jackson** library for JSON mapping. It runs in under **20MB of RAM** and starts instantly.

---

## 🚀 Key Features
- **Pure Java Architecture**: Uses only standard built-in libraries (no databases to install or configure).
- **In-Memory Storage**: Uses a thread-safe `ConcurrentHashMap` in memory for instantaneous reads/writes.
- **TMF621 Specification Conformance**:
  - **POST /troubleTicket/v2/troubleTicket**: Validates mandatory fields (`ticketType`, `description`, `severity`), generates unique UUID `id`, builds self-referencing `href`, and initializes the first audit log.
  - **GET /troubleTicket/v2/troubleTicket/{id}**: Retrieves a single ticket (returns a standardized `404 Not Found` JSON if missing).
  - **GET /troubleTicket/v2/troubleTicket**: Lists all tickets with query filter support (`status`, `severity`, `ticketType`).
  - **PATCH /troubleTicket/v2/troubleTicket/{id}**: Performs partial updates. Automatically appends a new `StatusChange` history record to the audit trail if the ticket's `status` changes.
- **Comprehensive Integration Tests**: A full end-to-end test suite using Java's standard `HttpClient` and JUnit 5.

---

## 🛠️ How to Run

Ensure you have **Java 17+** and **Maven** installed.

### Start the Server:
```bash
mvn compile exec:java -Dexec.mainClass="com.example.troubleticket.TroubleTicketServer"
```
The server will start listening on **`http://localhost:8080`**.

### Run the Integration Tests:
```bash
mvn test
```

---

## 📬 Testing with Postman

A pre-configured Postman Collection is included in the project root: **`TMF621_Trouble_Ticket.postman_collection.json`**.

### How to use:
1. Open Postman.
2. Click **Import** in the top-left and select the collection JSON file.
3. Start running requests! The collection includes a script that automatically captures the ticket ID on creation and updates subsequent requests.

---

## 🗂️ Project Structure
```
├── pom.xml                                     # Lightweight Maven configuration
├── README.md                                   # Project guide
├── Code_Walkthrough.md                         # Detailed code architecture guide
├── TMF621_Trouble_Ticket.postman_collection.json # Postman Test Suite
└── src/
    ├── main/java/com/example/troubleticket/
    │   ├── TroubleTicketServer.java            # Server routing & REST logic
    │   └── model/                              # TMF621 resource POJOs
    └── test/java/com/example/troubleticket/
        └── TroubleTicketApplicationTests.java  # HTTP Client test suite
```
