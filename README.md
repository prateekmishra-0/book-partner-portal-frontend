# 💻 Book Partner Portal (Frontend Client)

This repository contains the frontend application for the Book Partner Portal. It is a Server-Side Rendered (SSR) web interface constructed with Spring MVC and Thymeleaf. It functions as an entirely independent client, structurally isolated from the backend service.

## 🏗 Architecture & Design Patterns

*   **Isolated SSR Application:** The frontend runs on its own dedicated port (8081). It dynamically generates HTML pages on the server by retrieving data from the backend via standard HTTP requests using the modern web client approach.
*   **Automated Security Headers:** To guarantee secure interactions, a customizer bean automatically intercepts and attaches the necessary secret authorization headers to every single outgoing request to the API.
*   **Hypermedia Parsing:** The client is built to manually map and unpack complex hypermedia-driven JSON responses, safely converting embedded payload arrays and pagination data into usable Java objects.

## 🚦 Traffic Control & Rate Limiting

The application features an integrated traffic management layer to block abusive request patterns and shield the underlying backend from being flooded.

*   **Traffic Interceptor:** A dedicated web interceptor inspects incoming traffic before it ever reaches the application logic.
*   **IP-Based Monitoring:** Using thread-safe memory structures, the system actively counts the number of hits originating from each specific IP address.
*   **Threshold Enforcement:** The interceptor strictly limits traffic to a maximum of 50 requests per 10-second interval. Any IP crossing this threshold is immediately blocked and receives a 429 Too Many Requests response.

## 🔄 Concurrency Control & Optimistic Locking

To safely manage scenarios where multiple users attempt to modify the exact same data simultaneously, a stateless locking strategy is utilized.

*   **Payload Fingerprinting:** When data is requested, the system generates a unique hash based on the payload's contents.
*   **Lost-Update Prevention:** When submitting an update, this hash must be included. The server compares it against the current state; if there is a mismatch (indicating another user already altered the record), it blocks the transaction and issues a 409 Conflict error to prevent accidental data overwrites.
