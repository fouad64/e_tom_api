# 🎫 TMF621 Trouble Ticket API
### A Comprehensive Study & Implementation Guide
> **TM Forum Standard · Release 18.0.0 · June 2018**  
> Prepared for Group Presentation & Implementation Planning

---

## 📋 Table of Contents

1. [Introduction](#1-introduction)
2. [Purpose & Functionality](#2-purpose--functionality)
3. [How It Works — REST Principles](#3-how-it-works--rest-principles)
4. [Ticket Lifecycle & State Machine](#4-ticket-lifecycle--state-machine)
5. [Full Database Schema](#5-full-database-schema)
6. [API Verbs — Deep Dive](#6-api-verbs--deep-dive)
7. [Notification System](#7-notification-system)
8. [Sample I/O Walkthrough](#8-sample-io-walkthrough)
9. [Choosing Your 3 Verbs](#9-choosing-your-3-verbs)
10. [Presentation Outline](#10-presentation-outline)

---

## 1. Introduction

The **TMF621 Trouble Ticket API** is an open, standardized REST API published by the **TM Forum** — a global industry association for telecommunications companies. It defines a universal contract for how software systems should create, read, update, and delete trouble tickets.

### 🌍 What Is TM Forum?
TM Forum is a non-profit organization that creates open standards for the telecom and digital services industry. Their Open API suite (of which TMF621 is one) allows different vendors' systems to communicate without custom integration work.

### 🔌 Where Does This API Live?
```
Client Systems                    TMF621 API              Backend Systems
─────────────────                ───────────            ──────────────────
CRM Application   ──────────►   Trouble Ticket  ──►    Ticketing Database
Network Mgmt Tool ──────────►     REST API      ──►    Notification Engine
B2B Partner App   ──────────►   (TMF621)        ──►    Workflow Processor
Customer Portal   ──────────►                   ──►    Reporting System
```

### 📌 Key Metadata
| Property | Value |
|---|---|
| Standard | TM Forum Open API |
| API ID | TMF621 |
| Version | 3.0.1 |
| Release | 18.0.0 |
| Date | June 2018 |
| Protocol | REST / HTTP |
| Data Format | JSON |
| Base Path | `/troubleTicket/v2/troubleTicket` |

---

## 2. Purpose & Functionality

### 🎯 What Problem Does It Solve?

Before standards like TMF621, every company built its own ticketing interface. A CRM from Vendor A couldn't talk to a ticketing system from Vendor B without expensive custom middleware. TMF621 solves this by defining a **single, agreed-upon interface**.

### 🛠️ Core Capabilities

| Capability | Description |
|---|---|
| **Create Tickets** | Submit a new issue, complaint, or incident |
| **Retrieve Tickets** | Fetch one ticket or a filtered list |
| **Update Tickets** | Partially modify ticket details or status |
| **Delete Tickets** | Remove a ticket (admin only) |
| **Track Lifecycle** | Manage state transitions from creation to closure |
| **Event Notifications** | Push updates to registered subscribers |
| **Audit Trail** | Automatic status change history maintained by server |

### 🏢 Real-World Use Cases

```
USE CASE 1: Billing Dispute
────────────────────────────
Customer notices wrong charge on bill
→ Opens a "billingTicket" via CRM
→ API creates ticket, assigns ID
→ Agent reviews, updates status to InProgress
→ Customer gets notified via webhook
→ Issue resolved, ticket closed

USE CASE 2: Network Fault
────────────────────────────
Monitoring system detects network degradation
→ Auto-creates "incident" ticket via API
→ Engineers assigned (relatedParty)
→ Affected services linked (relatedEntity)
→ Status flows: Acknowledged → InProgress → Resolved → Closed

USE CASE 3: B2B Escalation
────────────────────────────
Partner company reports service issue
→ Submits ticket with externalId from their system
→ Both systems stay in sync via notifications
→ TicketRelationship links to parent ticket
```

---

## 3. How It Works — REST Principles

### 📐 The Uniform Contract

TMF621 strictly follows REST conventions. Every operation maps to a standard HTTP method:

```
HTTP METHOD     OPERATION              WHEN TO USE
───────────────────────────────────────────────────────────
GET             Read / Query           Retrieve data, never modify
POST            Create                 Create new resources
PATCH           Partial Update         Modify specific fields only
PUT             Complete Update        Replace entire resource
DELETE          Remove                 Delete a resource permanently
```

### 🗺️ Endpoint Map

```
Base URL: https://{host}:{port}/troubleTicket/v2

┌─────────────────────────────────────────────────────┐
│  TROUBLE TICKET RESOURCE                            │
├──────────────────────┬──────────────────────────────┤
│  GET    /troubleTicket              │ List all       │
│  POST   /troubleTicket              │ Create new     │
│  GET    /troubleTicket/{id}         │ Get one        │
│  PATCH  /troubleTicket/{id}         │ Update one     │
│  DELETE /troubleTicket/{id}         │ Delete one     │
├──────────────────────┬──────────────────────────────┤
│  NOTIFICATION HUB                                   │
├─────────────────────────────────────────────────────┤
│  POST   /hub                        │ Register       │
│  DELETE /hub/{id}                   │ Unregister     │
│  POST   /client/listener            │ Push event     │
└─────────────────────────────────────────────────────┘
```

### 🔧 Key HTTP Conventions

| Convention | Detail |
|---|---|
| **Success Create** | `201 Created` |
| **Success Read/Update** | `200 OK` |
| **Success Delete** | `204 No Content` |
| **Conflict** | `409 Conflict` |
| **Not Found** | `404 Not Found` |
| **Content Type (read)** | `application/json` |
| **Content Type (patch)** | `application/merge-patch+json` |
| **Filtering** | `GET /troubleTicket?status=Pending&severity=Urgent` |
| **Field Selection** | `GET /troubleTicket?fields=id,name,status` |

---

## 4. Ticket Lifecycle & State Machine

### 🔄 State Diagram

```
                        ┌─────────────┐
                        │   INITIAL   │
                        │  (Created)  │
                        └──────┬──────┘
                               │ Ticket Validation
                    ┌──────────┴──────────┐
                    │ FAIL                │ SUCCESS
                    ▼                     ▼
             ┌──────────┐         ┌──────────────┐
             │ REJECTED │──END    │ ACKNOWLEDGED │
             └──────────┘         └──────┬───────┘
                                         │ Start Ticket Process
                                         ▼
                                  ┌─────────────┐
                          ┌──────►│ IN PROGRESS │◄──────────────┐
                          │       └──────┬──────┘               │
                          │              │                       │
             Extra info   │    ┌─────────┴──────────┐  Extra info
             received     │    │                    │  received
                          │    ▼                    ▼
                          │ ┌─────────┐      ┌──────────────┐
                          └─│ PENDING │      │     HELD     │
                            └────┬────┘      └──────┬───────┘
                                 │                  │
                    Cannot       │                  │ Cannot
                    supply info  │                  │ solve issue
                                 ▼                  ▼
                            ┌───────────────────────────┐
                            │         CANCELLED         │──END
                            └───────────────────────────┘

                 ┌─────────────────────────────┐
                 │         IN PROGRESS         │
                 └──────────────┬──────────────┘
                                │ Resolve Ticket
                                ▼
                         ┌────────────┐
                ┌────────│  RESOLVED  │
                │        └────────────┘
                │ Reject        │ Accept Resolution
                │ Resolution    ▼
                └───────► ┌──────────┐
                           │  CLOSED  │──END
                           └──────────┘
```

### 📊 State Descriptions

| State | Description | Who Sets It |
|---|---|---|
| `Acknowledged` | Ticket validated and accepted | System |
| `InProgress` | Actively being worked on | Agent/System |
| `Pending` | Waiting for additional info from customer | Agent |
| `Held` | Blocked by an external issue | Agent |
| `Resolved` | Issue has been fixed | Agent |
| `Closed` | Customer accepted resolution | Customer/System |
| `Rejected` | Ticket failed validation | System |
| `Cancelled` | Could not proceed | Agent/System |

### 🔐 State Transition Rules

```
Acknowledged  →  InProgress        (Start working)
InProgress    →  Pending           (Need more info)
InProgress    →  Held              (Blocked externally)
InProgress    →  Resolved          (Issue fixed)
Pending       →  InProgress        (Info received)
Pending       →  Cancelled         (Info cannot be supplied)
Held          →  InProgress        (Block resolved)
Held          →  Cancelled         (Issue cannot be solved)
Resolved      →  Closed            (Resolution accepted)
Resolved      →  InProgress        (Resolution rejected)
Rejected      →  [END]
Cancelled     →  [END]
Closed        →  [END]
```

---

## 5. Full Database Schema

This section maps the TMF621 resource model to a relational database design you can implement directly.

### 🗄️ Entity Relationship Overview

```
                        ┌───────────────────────────────────────────┐
                        │              TROUBLE_TICKET               │
                        │  (Core entity — all others relate here)   │
                        └───────┬───────────────────────────────────┘
                                │
           ┌────────────────────┼──────────────────────┐
           │                    │                       │
    ┌──────▼──────┐    ┌────────▼────────┐   ┌─────────▼────────┐
    │    NOTE     │    │  STATUS_CHANGE  │   │   ATTACHMENT     │
    │  (0 to many)│    │  (0 to many)    │   │  (0 to many)     │
    └─────────────┘    └─────────────────┘   └──────────────────┘
           │
    ┌──────▼──────────┐    ┌──────────────────┐   ┌──────────────────┐
    │  RELATED_PARTY  │    │  RELATED_ENTITY  │   │ TICKET_RELATION  │
    │  (0 to many)    │    │  (0 to many)     │   │  (0 to many)     │
    └─────────────────┘    └──────────────────┘   └──────────────────┘
           │
    ┌──────▼──────┐
    │   CHANNEL   │
    │   (0 to 1)  │
    └─────────────┘
```

---

### 📋 Table 1: `trouble_ticket` (Core Table)

The central table. Every other table references this one via `ticket_id`.

```sql
CREATE TABLE trouble_ticket (
    id                       VARCHAR(50)   PRIMARY KEY,
    href                     TEXT,
    name                     VARCHAR(255),
    external_id              VARCHAR(100),
    ticket_type              VARCHAR(100)  NOT NULL,     -- MANDATORY
    description              TEXT          NOT NULL,     -- MANDATORY
    severity                 VARCHAR(50)   NOT NULL,     -- MANDATORY
    priority                 VARCHAR(50),
    status                   VARCHAR(50)   DEFAULT 'Acknowledged',
    status_change_reason     TEXT,
    creation_date            TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    last_update              TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    requested_resolution_date TIMESTAMP,
    expected_resolution_date  TIMESTAMP,
    resolution_date           TIMESTAMP,
    base_type                VARCHAR(100)  DEFAULT 'TroubleTicket',
    type                     VARCHAR(100),
    schema_location          TEXT,
    channel_id               VARCHAR(50)   REFERENCES channel(id)
);
```

**Field Reference:**

| Column | Type | Mandatory | Server-Set | Notes |
|---|---|---|---|---|
| `id` | VARCHAR | Auto | ✅ Yes | UUID or sequential |
| `href` | TEXT | No | ✅ Yes | Self-URL |
| `name` | VARCHAR | No | No | Short user label |
| `external_id` | VARCHAR | No | No | Partner system ref |
| `ticket_type` | VARCHAR | ✅ Yes | No | incident/complaint/request |
| `description` | TEXT | ✅ Yes | No | Full problem description |
| `severity` | VARCHAR | ✅ Yes | No | Critical/Major/Minor/Urgent |
| `priority` | VARCHAR | No | By system | High/Medium/Low |
| `status` | VARCHAR | No | ✅ Yes | State machine value |
| `status_change_reason` | TEXT | No | No | Why status changed |
| `creation_date` | TIMESTAMP | No | ✅ Yes | Auto on insert |
| `last_update` | TIMESTAMP | No | ✅ Yes | Auto on update |
| `requested_resolution_date` | TIMESTAMP | No | No | User requested |
| `expected_resolution_date` | TIMESTAMP | No | No | System estimated |
| `resolution_date` | TIMESTAMP | No | No | Actual resolution |
| `base_type` | VARCHAR | No | No | Always 'TroubleTicket' |
| `type` | VARCHAR | No | No | Subtype (BillingTicket…) |
| `schema_location` | TEXT | No | No | YAML schema URL |
| `channel_id` | FK | No | No | FK → channel |

---

### 📋 Table 2: `note`

Stores free-text comments attached to a ticket over time.

```sql
CREATE TABLE note (
    id          VARCHAR(50)   PRIMARY KEY,
    ticket_id   VARCHAR(50)   NOT NULL REFERENCES trouble_ticket(id) ON DELETE CASCADE,
    date        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    author      VARCHAR(255),
    text        TEXT          NOT NULL,
    type        VARCHAR(100)
);
```

| Column | Type | Notes |
|---|---|---|
| `id` | VARCHAR | PK |
| `ticket_id` | FK | Links to `trouble_ticket` |
| `date` | TIMESTAMP | When note was written |
| `author` | VARCHAR | Who wrote it (agent name, etc.) |
| `text` | TEXT | The note content |
| `type` | VARCHAR | Note classification |

**Relationship:** One `trouble_ticket` → Many `note` (1:N)

---

### 📋 Table 3: `status_change`

Immutable audit log. **Server populates this — clients cannot write to it.**

```sql
CREATE TABLE status_change (
    id             SERIAL        PRIMARY KEY,
    ticket_id      VARCHAR(50)   NOT NULL REFERENCES trouble_ticket(id) ON DELETE CASCADE,
    status         VARCHAR(50)   NOT NULL,
    change_reason  TEXT,
    change_date    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    type           VARCHAR(100)
);
```

| Column | Type | Notes |
|---|---|---|
| `id` | SERIAL | Auto-increment PK |
| `ticket_id` | FK | Links to `trouble_ticket` |
| `status` | VARCHAR | The new status that was set |
| `change_reason` | TEXT | Why the status changed |
| `change_date` | TIMESTAMP | When the change occurred |
| `type` | VARCHAR | Type of status change |

**Relationship:** One `trouble_ticket` → Many `status_change` (1:N)  
> ⚠️ **Implementation Note:** This table is READ-ONLY for API clients. It is written by the server whenever `status` changes on the parent ticket. Implement a database trigger or service-layer hook to auto-insert a record here on every status update.

---

### 📋 Table 4: `attachment`

Files or documents linked to a ticket (scanned bills, photos, etc.)

```sql
CREATE TABLE attachment (
    id           VARCHAR(50)   PRIMARY KEY,
    ticket_id    VARCHAR(50)   NOT NULL REFERENCES trouble_ticket(id) ON DELETE CASCADE,
    href         TEXT,
    name         VARCHAR(255),
    description  TEXT,
    url          TEXT,
    mime_type    VARCHAR(100),
    size         DECIMAL,
    size_unit    VARCHAR(20),
    valid_from   TIMESTAMP,
    valid_to     TIMESTAMP,
    type         VARCHAR(100),
    referred_type VARCHAR(100)
);
```

| Column | Type | Notes |
|---|---|---|
| `id` | VARCHAR | PK |
| `ticket_id` | FK | Links to `trouble_ticket` |
| `href` | TEXT | API reference URL |
| `name` | VARCHAR | File name |
| `description` | TEXT | What the file contains |
| `url` | TEXT | Direct download URL |
| `mime_type` | VARCHAR | e.g. application/pdf, image/jpeg |
| `size` | DECIMAL | File size |
| `size_unit` | VARCHAR | MB, kB, B |
| `valid_from/to` | TIMESTAMP | Validity window |
| `type` | VARCHAR | Attachment subtype |
| `referred_type` | VARCHAR | Always 'Attachment' |

**Relationship:** One `trouble_ticket` → Many `attachment` (1:N)

---

### 📋 Table 5: `channel`

The sales/interaction channel through which the ticket was raised.

```sql
CREATE TABLE channel (
    id    VARCHAR(50)  PRIMARY KEY,
    name  VARCHAR(255),
    type  VARCHAR(100)
);
```

| Column | Type | Notes |
|---|---|---|
| `id` | VARCHAR | PK |
| `name` | VARCHAR | e.g. "Self Service", "Call Centre", "Mobile App" |
| `type` | VARCHAR | Channel class type |

**Relationship:** One `channel` → Many `trouble_ticket` (1:N)  
> The FK lives in `trouble_ticket.channel_id`

---

### 📋 Table 6: `related_party`

People or organizations associated with a ticket (customer, agent, owner, etc.)

```sql
CREATE TABLE related_party (
    id             VARCHAR(50)  PRIMARY KEY,
    ticket_id      VARCHAR(50)  NOT NULL REFERENCES trouble_ticket(id) ON DELETE CASCADE,
    href           TEXT,
    role           VARCHAR(100),
    name           VARCHAR(255),
    referred_type  VARCHAR(100)
);
```

| Column | Type | Notes |
|---|---|---|
| `id` | VARCHAR | PK (party's own ID) |
| `ticket_id` | FK | Links to `trouble_ticket` |
| `href` | TEXT | Party resource URL |
| `role` | VARCHAR | owner / customer / agent / reviser |
| `name` | VARCHAR | Human-readable name |
| `referred_type` | VARCHAR | Individual / Organization / Customer |

**Relationship:** One `trouble_ticket` → Many `related_party` (1:N)

**Common roles you'll encounter:**
```
"role": "owner"     → The agent responsible for resolution
"role": "customer"  → Who raised the issue
"role": "reviser"   → Who last modified the ticket
```

---

### 📋 Table 7: `related_entity`

External resources the ticket is about (a bill, a product, a service, etc.)

```sql
CREATE TABLE related_entity (
    id             VARCHAR(50)   PRIMARY KEY,
    ticket_id      VARCHAR(50)   NOT NULL REFERENCES trouble_ticket(id) ON DELETE CASCADE,
    href           TEXT,
    name           VARCHAR(255),
    role           VARCHAR(100),
    referred_type  VARCHAR(100)
);
```

| Column | Type | Notes |
|---|---|---|
| `id` | VARCHAR | Entity's ID in its own system |
| `ticket_id` | FK | Links to `trouble_ticket` |
| `href` | TEXT | URL to the entity in its own API |
| `name` | VARCHAR | e.g. "December Bill", "Router Model X" |
| `role` | VARCHAR | disputedBill / damagedDevice / affectedService |
| `referred_type` | VARCHAR | CustomerBill / Product / Service / Resource |

**Relationship:** One `trouble_ticket` → Many `related_entity` (1:N)

---

### 📋 Table 8: `ticket_relationship`

Links one trouble ticket to another (parent/child, dependencies, duplicates).

```sql
CREATE TABLE ticket_relationship (
    id           VARCHAR(50)  PRIMARY KEY,
    ticket_id    VARCHAR(50)  NOT NULL REFERENCES trouble_ticket(id) ON DELETE CASCADE,
    related_id   VARCHAR(50)  NOT NULL,
    href         TEXT,
    type         VARCHAR(100)
);
```

| Column | Type | Notes |
|---|---|---|
| `id` | VARCHAR | Related ticket's ID |
| `ticket_id` | FK | The source ticket |
| `related_id` | VARCHAR | The target ticket's ID |
| `href` | TEXT | URL to the related ticket |
| `type` | VARCHAR | isChild / isParent / dependent / duplicate |

**Relationship:** Many `trouble_ticket` ↔ Many `trouble_ticket` (via junction table)

---

### 📋 Table 9: `hub` (Notification Listeners)

Stores registered notification callbacks.

```sql
CREATE TABLE hub (
    id        SERIAL  PRIMARY KEY,
    callback  TEXT    NOT NULL,
    query     TEXT
);
```

| Column | Type | Notes |
|---|---|---|
| `id` | SERIAL | PK, returned to caller |
| `callback` | TEXT | URL to POST events to |
| `query` | TEXT | Optional filter on event types |

---

### 🔗 Full ER Diagram (Text Representation)

```
trouble_ticket (PK: id)
    │
    ├──< note              (FK: ticket_id)          [1:N]
    ├──< status_change     (FK: ticket_id)          [1:N] SERVER ONLY
    ├──< attachment        (FK: ticket_id)          [1:N]
    ├──< related_party     (FK: ticket_id)          [1:N]
    ├──< related_entity    (FK: ticket_id)          [1:N]
    ├──< ticket_relationship (FK: ticket_id)        [1:N]
    └──> channel           (FK: channel_id in tt)  [N:1]

hub (standalone — stores listener callbacks)
```

---

## 6. API Verbs — Deep Dive

### Overview Comparison Table

| Verb | Method | Endpoint | Auth Level | Response | DB Operation |
|---|---|---|---|---|---|
| List | GET | `/troubleTicket` | User | 200 + Array | SELECT * |
| Retrieve | GET | `/troubleTicket/{id}` | User | 200 + Object | SELECT WHERE id |
| Create | POST | `/troubleTicket` | User | 201 + Object | INSERT |
| Update | PATCH | `/troubleTicket/{id}` | User | 200 + Object | UPDATE |
| Delete | DELETE | `/troubleTicket/{id}` | **Admin** | 204 Empty | DELETE |

---

### 🟢 VERB 1: GET `/troubleTicket` — List All Tickets

**Purpose:** Query and retrieve a collection of trouble tickets. Supports filtering and field selection.

**When to use:** Dashboard views, searching, reporting, bulk export.

**Query Parameters:**
```
?fields=id,name,status          → Only return these fields
?status=Pending                 → Filter by status
?severity=Urgent                → Filter by severity
?ticketType=billingTicket       → Filter by type
?creationDate.gt=2018-01-01     → Filter by date range
```

**Request:**
```http
GET /troubleTicket/v2/troubleTicket
Accept: application/json
```

**Success Response (200):**
```json
[
  {
    "id": "3180",
    "href": "https://host:port/troubleTicket/v2/troubleTicket/3180",
    "name": "Compliant over last bill",
    "externalId": "213",
    "ticketType": "Bill Dispute",
    "creationDate": "2018-05-01T00:00",
    "lastUpdate": "2018-05-01T00:00",
    "description": "I do not accept the last VOD charge...",
    "severity": "Urgent",
    "priority": "High",
    "status": "Pending",
    "statusChangeReason": "Need more information from the customer",
    "@type": "TroubleTicket",
    "relatedParty": [
      {
        "id": "6675",
        "href": "https://host:port/partyManagement/v2/individual/6675",
        "role": "customer",
        "name": "Mr Jack Hide",
        "@referredType": "Customer"
      }
    ]
  }
]
```

**Implementation Checklist:**
- [ ] Query all records from `trouble_ticket` table
- [ ] JOIN with sub-resource tables (note, attachment, etc.)
- [ ] Apply filters from query parameters
- [ ] Apply field selection if `?fields=` is provided
- [ ] Return as JSON array (even if one or zero results)
- [ ] Return `200 OK`

---

### 🟢 VERB 2: GET `/troubleTicket/{id}` — Retrieve One Ticket

**Purpose:** Fetch the complete details of a single ticket by its unique ID.

**When to use:** Ticket detail view, status check, before performing a PATCH.

**Request:**
```http
GET /troubleTicket/v2/troubleTicket/3180
Accept: application/json
```

**Success Response (200):** Full ticket object including all nested sub-resources:
```json
{
  "id": "3180",
  "href": "https://host:port/troubleTicket/v2/troubleTicket/3180",
  "name": "Compliant over last bill",
  "externalId": "213",
  "ticketType": "Bill Dispute",
  "creationDate": "2018-05-01T00:00",
  "lastUpdate": "2018-05-01T00:00",
  "description": "I do not accept the last VOD charge...",
  "severity": "Urgent",
  "priority": "High",
  "status": "Pending",
  "statusChangeReason": "Need more information from the customer",
  "@type": "TroubleTicket",
  "@schemaLocation": "https://host:port/troubleTicket/v2/schema/troubleTicket.yml",
  "relatedEntity": [
    {
      "id": "3472",
      "href": "https://host:port/customerBillManagement/v2/customerBill/8297",
      "role": "Disputed Bill",
      "name": "December Bill",
      "@referredType": "CustomerBill"
    }
  ],
  "statusChange": [
    {
      "status": "Pending",
      "changeReason": "Need more information from the customer",
      "changeDate": "2018-05-01T00:00"
    }
  ],
  "note": [
    {
      "date": "2018-05-01T00:00",
      "author": "Mr Jack Hide",
      "text": "This is quite important"
    }
  ],
  "relatedParty": [
    {
      "id": "6675",
      "href": "https://host:port/partyManagement/v2/individual/6675",
      "role": "owner",
      "name": "Gustave Flaubert",
      "@referredType": "Individual"
    },
    {
      "id": "6675",
      "href": "https://host:port/customerManagement/v2/customer/8897",
      "role": "customer",
      "name": "Mr Jack Hide",
      "@referredType": "Customer"
    }
  ],
  "channel": {
    "id": "8774",
    "name": "Self Service",
    "@type": "Channel"
  },
  "attachment": [
    {
      "description": "Scanned disputed bill",
      "href": "http://hostname:port/documentManagement/v2/attachment/44",
      "id": "44",
      "url": "http://xxxxx",
      "name": "December Bill",
      "@referredType": "Attachment"
    }
  ]
}
```

**Error Response (404):**
```json
{
  "code": "404",
  "reason": "Not Found",
  "message": "TroubleTicket with id '3180' does not exist"
}
```

**Implementation Checklist:**
- [ ] Parse `{id}` from URL path
- [ ] SELECT from `trouble_ticket` WHERE id = ?
- [ ] JOIN all related tables (note, statusChange, attachment, relatedParty, relatedEntity, channel, ticketRelationship)
- [ ] If not found → return `404 Not Found`
- [ ] Return full JSON object with `200 OK`

---

### 🔵 VERB 3: POST `/troubleTicket` — Create Ticket

**Purpose:** Submit a new trouble ticket into the system.

**When to use:** Customer raises a complaint, system auto-generates an incident, agent logs a request.

#### Mandatory vs Optional Fields

```
┌──────────────────────────────────────────────────────────────┐
│  MANDATORY — Request WILL FAIL without these                 │
├──────────────────────────────────────────────────────────────┤
│  • description     (TEXT)                                    │
│  • severity        (STRING: Critical/Major/Minor/Urgent)     │
│  • ticketType      (STRING: incident/complaint/request/...)  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  SERVER-POPULATED — Do NOT send these, server sets them      │
├──────────────────────────────────────────────────────────────┤
│  • id              (Generated UUID/sequence)                 │
│  • href            (Built from base URL + id)                │
│  • creationDate    (Current timestamp)                       │
│  • lastUpdate      (Current timestamp)                       │
│  • statusChange    (Empty array, grows with status changes)  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  OPTIONAL — Include as needed                                │
├──────────────────────────────────────────────────────────────┤
│  • name, externalId, priority                                │
│  • requestedResolutionDate, expectedResolutionDate           │
│  • status, @type, @baseType, @schemaLocation                 │
│  • relatedEntity, note, relatedParty                         │
│  • ticketRelationship, channel, attachment                   │
└──────────────────────────────────────────────────────────────┘
```

**Minimal Request (mandatory only):**
```http
POST /troubleTicket/v2/troubleTicket
Content-Type: application/json

{
  "description": "Compliant over last invoice",
  "severity": "Urgent",
  "ticketType": "billingTicket",
  "@type": "TroubleTicket"
}
```

**Full Request (with optional fields):**
```http
POST /troubleTicket/v2/troubleTicket
Content-Type: application/json

{
  "description": "I do not accept the last VOD charge",
  "severity": "Urgent",
  "ticketType": "Bill Dispute",
  "name": "VOD Billing Complaint",
  "externalId": "CRM-2024-9921",
  "priority": "High",
  "requestedResolutionDate": "2018-05-15T00:00",
  "@type": "TroubleTicket",
  "relatedEntity": [
    {
      "id": "3472",
      "href": "https://host:port/customerBillManagement/v2/customerBill/8297",
      "role": "Disputed Bill",
      "name": "December Bill",
      "@referredType": "CustomerBill"
    }
  ],
  "relatedParty": [
    {
      "id": "6675",
      "href": "https://host:port/customerManagement/v2/customer/8897",
      "role": "customer",
      "name": "Mr Jack Hide",
      "@referredType": "Customer"
    }
  ],
  "channel": {
    "id": "8774",
    "name": "Self Service",
    "@type": "Channel"
  }
}
```

**Success Response (201):**
```json
{
  "id": "3180",
  "href": "https://host:port/troubleTicket/v2/troubleTicket/3180",
  "description": "Compliant over last invoice",
  "severity": "Urgent",
  "ticketType": "billingTicket",
  "creationDate": "2018-05-01T00:00",
  "lastUpdate": "2018-05-01T00:00",
  "status": "Acknowledged",
  "@type": "TroubleTicket"
}
```

**Implementation Checklist:**
- [ ] Validate mandatory fields: `description`, `severity`, `ticketType`
- [ ] Return `400 Bad Request` if any mandatory field is missing
- [ ] Generate unique `id` (UUID recommended)
- [ ] Build `href` from base URL + id
- [ ] Set `creationDate` and `lastUpdate` to now
- [ ] Set initial `status` to `"Acknowledged"`
- [ ] INSERT into `trouble_ticket`
- [ ] INSERT into sub-tables for any nested objects provided (note, relatedParty, etc.)
- [ ] Return `201 Created` with the full new object

---

### 🟡 VERB 4: PATCH `/troubleTicket/{id}` — Partial Update

**Purpose:** Modify specific fields of an existing ticket. Only the fields you send get changed — everything else stays the same.

**Key difference from PUT:** PUT would replace the entire object. PATCH only touches what you specify.

**Content-Type must be:** `application/merge-patch+json`

#### Patchable vs Non-Patchable

```
┌──────────────────────────────────────────────────────┐
│  ✅ PATCHABLE — Clients can change these             │
├──────────────────────────────────────────────────────┤
│  externalId              name                        │
│  ticketType              description                 │
│  reason                  severity                    │
│  priority                status                      │
│  requestedResolutionDate expectedResolutionDate      │
│  resolutionDate          relatedEntity               │
│  note                    relatedParty                │
│  ticketRelationship      channel                     │
│  attachment                                          │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│  ❌ NON-PATCHABLE — Server-controlled, read-only     │
├──────────────────────────────────────────────────────┤
│  id                      href                        │
│  creationDate            lastUpdate (server updates) │
│  statusChange            @baseType                   │
│  @type                   @schemaLocation             │
└──────────────────────────────────────────────────────┘
```

**Simple Request (change description only):**
```http
PATCH /troubleTicket/v2/troubleTicket/3180
Content-Type: application/merge-patch+json

{
  "description": "Updated description with more details"
}
```

**Status Update Request (advance the workflow):**
```http
PATCH /troubleTicket/v2/troubleTicket/3180
Content-Type: application/merge-patch+json

{
  "status": "InProgress",
  "statusChangeReason": "All information received, investigation started"
}
```

**Rich Update (multiple fields):**
```http
PATCH /troubleTicket/v2/troubleTicket/3180
Content-Type: application/merge-patch+json

{
  "priority": "High",
  "expectedResolutionDate": "2018-05-10T00:00",
  "statusChangeReason": "Escalated due to customer priority",
  "note": [
    {
      "date": "2018-05-02T09:30",
      "author": "Agent Smith",
      "text": "Reviewed billing records, discrepancy confirmed"
    }
  ]
}
```

**Success Response (200):**
```json
{
  "id": "3180",
  "href": "https://host:port/troubleTicket/v2/troubleTicket/3180",
  "name": "Compliant over last bill",
  "description": "Updated description with more details",
  "severity": "Urgent",
  "priority": "High",
  "status": "InProgress",
  "lastUpdate": "2018-05-02T09:30:00",
  "statusChange": [
    {
      "status": "InProgress",
      "changeReason": "All information received, investigation started",
      "changeDate": "2018-05-02T09:30:00"
    }
  ]
}
```

**Implementation Checklist:**
- [ ] Parse `{id}` from URL path — return `404` if not found
- [ ] Parse JSON body
- [ ] Ignore any non-patchable fields silently (or return `400`)
- [ ] UPDATE only the provided fields in `trouble_ticket`
- [ ] Set `last_update` to current timestamp (server-side)
- [ ] If `status` changed → INSERT a new row in `status_change` table
- [ ] Handle nested arrays (notes, relatedParty): merge or replace strategy
- [ ] Return `200 OK` with the full updated object

---

### 🔴 VERB 5: DELETE `/troubleTicket/{id}` — Delete Ticket

**Purpose:** Permanently remove a trouble ticket and all its sub-resources.

**⚠️ Admin-only operation.** Standard users should not be able to call this.

**Request:**
```http
DELETE /troubleTicket/v2/troubleTicket/3180
```

**Success Response (204):**
```
HTTP/1.1 204 No Content
(Empty body)
```

**Error Response (404):**
```json
{
  "code": "404",
  "reason": "Not Found",
  "message": "TroubleTicket with id '3180' does not exist"
}
```

**Implementation Checklist:**
- [ ] Parse `{id}` from URL path
- [ ] Check ticket exists — return `404` if not
- [ ] Check caller has admin role
- [ ] DELETE from all sub-tables first (or use CASCADE): note, statusChange, attachment, relatedParty, relatedEntity, ticketRelationship
- [ ] DELETE the `trouble_ticket` record
- [ ] Return `204 No Content` (no body)

---

## 7. Notification System

The API includes a **publish-subscribe (Pub/Sub)** mechanism allowing external systems to receive real-time updates.

### 🔔 How It Works

```
STEP 1: Register
─────────────────
External System  ──POST /hub──►  TMF621 API
{"callback": "http://my-system.com/events"}
                 ◄─201 Created──
                 {"id": "42", "callback": "..."}

STEP 2: Event Occurs
─────────────────────
[Ticket status changes from InProgress to Resolved]

STEP 3: Notification Pushed
────────────────────────────
TMF621 API  ──POST http://my-system.com/events──►  External System
{
  "eventId": "00001",
  "eventTime": "2018-05-02T10:00:00",
  "eventType": "TroubleTicketStatusChangeNotification",
  "event": {
    "troubleTicket": { ...full ticket object... }
  }
}

STEP 4: Unregister (when done)
──────────────────────────────
External System  ──DELETE /hub/42──►  TMF621 API
                 ◄─204 No Content──
```

### 📬 Notification Types

| Event Type | Triggered When |
|---|---|
| `TroubleTicketCreationNotification` | New ticket is created (POST success) |
| `TroubleTicketChangeNotification` | Any field on ticket changes (PATCH success) |
| `TroubleTicketStatusChangeNotification` | `status` field specifically changes |
| `TroubleTicketResolvedNotification` | Ticket status becomes `Resolved` |
| `TroubleTicketInformationRequiredNotification` | System needs more data from user |

### 📦 Notification Payload Structure

```json
{
  "eventId": "00001",
  "eventTime": "2018-11-16T16:42:25-04:00",
  "eventType": "TroubleTicketInformationRequiredNotification",
  "resourcePath": "/troubleTicket/3180",
  "fieldPath": "missing=attachment",
  "event": {
    "troubleTicket": {
      "id": "3180",
      "href": "https://host:port/troubleTicket/v2/troubleTicket/3180",
      "name": "Compliant over last bill"
    }
  }
}
```

> **`fieldPath`** uses a query-string-like syntax to indicate which field is missing:  
> `missing=attachment` → The attachment field needs to be provided  
> `missing=relatedEntity&id=3472` → A specific related entity is missing data

---

## 8. Sample I/O Walkthrough

### 🎬 Full End-to-End Scenario: Bill Dispute

This walkthrough demonstrates a complete ticket lifecycle using all 5 verbs.

---

#### Step 1 — Customer Raises Dispute (POST)

```
Customer logs into Self-Service Portal
→ Clicks "Dispute Bill"
→ Portal calls API
```

```http
POST /troubleTicket/v2/troubleTicket
Content-Type: application/json

{
  "description": "December bill shows VOD charge for movie I could not watch due to buffering",
  "severity": "Minor",
  "ticketType": "Bill Dispute",
  "name": "VOD charge complaint",
  "requestedResolutionDate": "2018-05-15T00:00",
  "relatedEntity": [{
    "id": "8297",
    "role": "Disputed Bill",
    "name": "December Bill",
    "@referredType": "CustomerBill"
  }],
  "relatedParty": [{
    "id": "6675",
    "role": "customer",
    "name": "Mr Jack Hide",
    "@referredType": "Customer"
  }],
  "channel": { "id": "8774", "name": "Self Service" }
}
```

```json
← 201 Created
{
  "id": "3180",
  "status": "Acknowledged",
  "creationDate": "2018-05-01T08:00:00"
}
```

---

#### Step 2 — Agent Checks the Ticket (GET by ID)

```
Agent receives work queue item
→ Opens ticket detail
→ Portal calls API
```

```http
GET /troubleTicket/v2/troubleTicket/3180
```

```json
← 200 OK
{ ...full ticket with all sub-resources... }
```

---

#### Step 3 — Agent Starts Investigation (PATCH)

```
Agent begins review
→ Advances ticket status
→ Adds note
```

```http
PATCH /troubleTicket/v2/troubleTicket/3180
Content-Type: application/merge-patch+json

{
  "status": "InProgress",
  "statusChangeReason": "Reviewing billing records",
  "note": [{
    "date": "2018-05-02T09:00:00",
    "author": "Agent Smith",
    "text": "Customer confirms movie was interrupted at 45-min mark. Checking VOD logs."
  }]
}
```

```json
← 200 OK
{ "status": "InProgress", "lastUpdate": "2018-05-02T09:00:00" }
```

---

#### Step 4 — Supervisor Checks All Pending Tickets (GET list)

```
Supervisor runs daily report
→ Filters by status
```

```http
GET /troubleTicket/v2/troubleTicket?status=InProgress&fields=id,name,severity,status
```

```json
← 200 OK
[
  { "id": "3180", "name": "VOD charge complaint", "severity": "Minor", "status": "InProgress" },
  { "id": "3181", "name": "Internet outage", "severity": "Critical", "status": "InProgress" }
]
```

---

#### Step 5 — Ticket Resolved (PATCH)

```
Agent confirms refund approved
→ Marks ticket resolved
```

```http
PATCH /troubleTicket/v2/troubleTicket/3180
Content-Type: application/merge-patch+json

{
  "status": "Resolved",
  "statusChangeReason": "VOD charge confirmed erroneous. Credit of $14.99 applied to next bill.",
  "resolutionDate": "2018-05-03T14:00:00"
}
```

```json
← 200 OK
{ "status": "Resolved", "resolutionDate": "2018-05-03T14:00:00" }
```

---

#### Step 6 — Old Test Ticket Cleaned Up (DELETE)

```
Admin removes a test ticket created during QA
```

```http
DELETE /troubleTicket/v2/troubleTicket/9999
```

```
← 204 No Content
```

---

## 9. Choosing Your 3 Verbs

### 🤔 Decision Framework

Consider these factors:
- **Complexity** — how much logic does each verb require?
- **Database impact** — how many tables are touched?
- **Presentation value** — what makes a good demo?
- **Dependencies** — some verbs depend on others

### 📊 Verb Comparison Matrix

| Verb | Complexity | DB Tables Touched | Demo Value | Depends On |
|---|---|---|---|---|
| GET (list) | Low | All (read) | Medium | Nothing |
| GET (single) | Low | All (read) | Medium | POST |
| POST (create) | Medium | All (write) | **High** | Nothing |
| PATCH (update) | **High** | Multiple (write + audit) | **High** | POST |
| DELETE | Low | All (delete cascade) | Low | POST |

### 🏆 Recommended Combination: POST + GET + PATCH

```
WHY THIS COMBINATION?

POST  → You can demo creating tickets with real payloads
        Tests mandatory field validation (great for presentation)
        Shows server-side auto-population (id, href, dates)

GET   → You can verify POST worked by retrieving what you created
        Shows filtering and field selection
        Natural demo flow: "here's what we just created"

PATCH → The most complex verb — great to show depth of understanding
        Forces you to implement the status machine
        Auto-inserts status_change records (shows business logic)
        Shows merge semantics (partial update, not full replace)

RESULT: A complete CREATE → READ → UPDATE loop that mirrors
        real-world ticket workflows end to end.
```

### 🥈 Alternative: POST + GET + DELETE

Good if you want simpler implementation with cleaner demo (create → view → remove), but less educational depth than PATCH.

### 🥉 Alternative: GET + GET + POST

Safe but conservative — lacks the update lifecycle, which is the most interesting part of a ticketing system.

---

## 10. Presentation Outline

Use this structure for your group presentation:

### 📢 Suggested Slide Flow

```
SLIDE 1: Title
─────────────
TMF621 Trouble Ticket API
Group [X] | Date

SLIDE 2: What Is TMF621?
─────────────────────────
• TM Forum, open standard
• Problem it solves
• Where it fits (diagram)

SLIDE 3: Core Concepts
────────────────────────
• What is a trouble ticket
• Key attributes
• The lifecycle / state machine diagram

SLIDE 4: Architecture
──────────────────────
• REST uniform contract table
• Endpoint map
• JSON format

SLIDE 5: Database Schema
─────────────────────────
• ER diagram
• Core table walkthrough
• Relationships explained

SLIDE 6: All 5 Verbs Overview
───────────────────────────────
• Quick table of all verbs
• Why each exists

SLIDE 7: Our Chosen Verb 1 — POST
───────────────────────────────────
• Purpose
• Mandatory fields
• Live demo / code walkthrough
• Request + Response sample

SLIDE 8: Our Chosen Verb 2 — GET
──────────────────────────────────
• List vs single retrieve
• Filtering demo
• Live demo / code walkthrough

SLIDE 9: Our Chosen Verb 3 — PATCH
────────────────────────────────────
• Merge patch explained
• Patchable vs non-patchable
• Status machine trigger
• Live demo / code walkthrough

SLIDE 10: End-to-End Demo
───────────────────────────
• Full scenario walkthrough (billing dispute)
• Show database state changing

SLIDE 11: Notifications (Bonus)
─────────────────────────────────
• Pub/Sub overview
• Hub register/unregister
• Event payload sample

SLIDE 12: Challenges & Learnings
──────────────────────────────────
• What was hard
• Design decisions made
• What we'd do differently

SLIDE 13: Q&A
──────────────
Questions?
```

---

## 📚 Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────────┐
│                    TMF621 QUICK REFERENCE                           │
├──────────┬─────────────────────────┬────────┬───────────────────────┤
│  METHOD  │  ENDPOINT               │  CODE  │  PURPOSE              │
├──────────┼─────────────────────────┼────────┼───────────────────────┤
│  GET     │  /troubleTicket         │  200   │  List all tickets     │
│  GET     │  /troubleTicket/{id}    │  200   │  Get one ticket       │
│  POST    │  /troubleTicket         │  201   │  Create ticket        │
│  PATCH   │  /troubleTicket/{id}    │  200   │  Update ticket        │
│  DELETE  │  /troubleTicket/{id}    │  204   │  Delete ticket        │
│  POST    │  /hub                   │  201   │  Register listener    │
│  DELETE  │  /hub/{id}              │  204   │  Unregister listener  │
│  POST    │  /client/listener       │  201   │  Push event           │
├──────────┴─────────────────────────┴────────┴───────────────────────┤
│  MANDATORY FIELDS ON CREATE: description, severity, ticketType      │
│  SERVER-AUTO-SET: id, href, creationDate, lastUpdate, statusChange  │
│  NON-PATCHABLE: id, href, creationDate, statusChange, @type         │
│  PATCH CONTENT-TYPE: application/merge-patch+json                   │
│  STATES: Acknowledged→InProgress→Resolved→Closed (happy path)       │
└─────────────────────────────────────────────────────────────────────┘
```

---

*TMF621 Trouble Ticket API REST Specification — TM Forum Release 18.0.0 — © TM Forum 2018*  
*Study guide prepared for group implementation and presentation purposes*
