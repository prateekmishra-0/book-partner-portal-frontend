# 💻 Book Partner Portal — Frontend Client

> A **Server-Side Rendered (SSR)** Spring MVC web application that acts as a fully independent client to the Book Partner Portal backend API. Structurally isolated — communicates with the backend exclusively via HTTP REST calls.

---

## 🗺 System Architecture

```
Browser
   │
   ▼
Frontend Client (Spring MVC + Thymeleaf) ── port 8081
   │
   │  HTTP REST  +  X-Project-Secret header  (auto-injected on every request)
   ▼
Backend API (Spring Data REST + HATEOAS) ── port 8080
   │
   ▼
MySQL 8.0 Database (pubs schema)
```

The frontend is a **separate Spring Boot JAR** — it has its own port, its own CI/CD pipeline, and zero shared code with the backend. It consumes the backend's HATEOAS API exactly as any external client would.

**Why SSR (Server-Side Rendering)?**  
The Java server fetches data from the backend, places it in a Spring MVC `Model`, and Thymeleaf templates render full HTML pages on the server before sending them to the browser. This means:
- No client-side JavaScript required for page rendering
- SEO-friendly, fully pre-rendered HTML
- Thymeleaf templates have direct access to typed Java objects in the model

**Exception — Employee module:** The Employee pages use a hybrid approach: static HTML served from `/static/` with vanilla JavaScript making AJAX calls to a proxy `@RestController` (`EmployeeProxyController`). This demonstrates two different frontend architectures coexisting in the same application.

---

## 🏗 Architecture & Design Patterns

- **Isolated SSR Application:** Runs on its own dedicated port (`8081`). Dynamically generates HTML pages on the server by fetching data from the backend using `RestClient` (the modern replacement for `RestTemplate` in Spring 6+).

- **Automated Secret Header Injection:** A `RestClientCustomizer` bean acts as a centralized interceptor. It automatically attaches the `X-Project-Secret` header to **every single outgoing request** — no manual header management in individual service methods.

  ```java
  @Bean
  public RestClientCustomizer addSecretHeaderCustomizer() {
      return builder -> builder.defaultHeader("X-Project-Secret", secretKey);
  }
  ```

- **HAL+JSON Manual Parsing:** The backend returns HATEOAS HAL+JSON responses with a complex nested structure (`_embedded`, `_links`, `page`). This doesn't map cleanly to a single Java class, so responses are parsed manually using Jackson's `JsonNode` API:
  - `_embedded.{resourceName}[]` → `List<Dto>`
  - `_links.self.href` → entity ID extracted from URL path (Spring Data REST hides raw IDs from response bodies by default — the HATEOAS principle is to navigate by links, not raw IDs)
  - `page` → `PageMetadata` (size, totalElements, totalPages, number)

- **Swagger UI Proxy:** The backend hosts Swagger UI at `localhost:8080/swagger-ui`. A `ViewController` on the frontend proxies all Swagger resources to `localhost:8081/api-docs/swagger-ui/`, allowing developers to access API documentation without exposing or directly connecting to the backend port.

---

## 🚦 Traffic Control & Rate Limiting

An integrated traffic management layer protects the backend from request floods. Implemented as a Spring MVC `HandlerInterceptor` — runs inside the `DispatcherServlet`, after the filter chain, before any controller method executes.

**Filter vs Interceptor:**  
A `Filter` (like the backend's `SecretKeyFilter`) runs at the Servlet container level — before `DispatcherServlet`. An `Interceptor` runs inside `DispatcherServlet`, giving it access to handler (controller method) information. The interceptor is the right tool here because it has access to Spring MVC context.

- **`RateLimitInterceptor`:** Implements `HandlerInterceptor.preHandle()`. Returns `false` to block the request; `true` to allow it through.

- **IP-Based Monitoring:** Uses `ConcurrentHashMap` for thread-safe per-IP request counting. Thread safety is critical — `HandlerInterceptor` is a singleton and multiple request threads can hit it simultaneously.

- **Threshold Enforcement:** Maximum **50 requests per 10-second window** per IP. Exceeding the threshold → `429 Too Many Requests`.

  - AJAX requests (`Accept: application/json`) receive a JSON error response
  - Browser requests receive an HTML error page

- **Window Reset:** Each IP's counter resets every 10 seconds. The window start time is tracked alongside the count and compared on each request.

---

## 🔄 Concurrency Control & ETag-Based Optimistic Locking

Safely handles the **lost-update problem**: two users loading the same record and submitting conflicting changes simultaneously.

**How it works (in `EmployeeProxyController`):**

1. `GET /ui-api/employees/{id}` — the frontend fetches employee data from the backend
2. The response JSON is serialized to a string and **MD5-hashed** — this hash is the ETag (a content fingerprint)
3. The ETag is sent to the browser as an `ETag` response header
4. When the user submits an update (`PUT /ui-api/employees/{id}`), the browser sends the stored ETag in the `If-Match` request header
5. The backend recomputes the **current** ETag from the live data
6. If `If-Match` ≠ current ETag → someone else changed the record → `409 Conflict`
7. If they match → data is unchanged → update proceeds safely

This is **optimistic locking**: no database locks are held, no rows are blocked. Contention is detected and resolved at the application layer at the moment of update.

---

## 🔁 Full Request Journey

**Example: User visits `/authors` page**

```
Browser: GET http://localhost:8081/authors

Frontend
  └─ AuthorFrontendController.viewAuthorsPage()
  └─ AuthorClientService.fetchAuthorsPaginated()
       └─ RestClient: GET http://localhost:8080/api/authors?page=0&size=5
            + Header: X-Project-Secret: <auto-injected by RestClientCustomizer>

Backend (port 8080)
  └─ SecretKeyFilter ── validates header ✓
  └─ Spring Data REST ── AuthorRepository.findAll(Pageable)
  └─ Hibernate: SELECT * FROM authors WHERE is_active = true LIMIT 5
  └─ Response: HAL+JSON

Frontend (parsing)
  └─ ObjectMapper: _embedded.authors[] → List<AuthorDto>
  └─ ObjectMapper: _links.self.href → extract au_id from URL
  └─ ObjectMapper: page → PageMetadata
  └─ Model.addAttribute("authors", authorList)
  └─ Model.addAttribute("page", pageMetadata)

Thymeleaf: renders authors-page.html → full HTML
Browser receives: complete rendered page
```

**Example: Employee update with ETag check**

```
Browser: GET /ui-api/employees/A-C71970F (via JS AJAX)
  └─ EmployeeProxyController fetches from backend
  └─ MD5 hash of JSON body computed → sent as ETag header
  └─ Browser stores ETag

Browser: PUT /ui-api/employees/A-C71970F (user submits form)
  + Header: If-Match: <stored ETag>
  └─ EmployeeProxyController recomputes current ETag from live backend data
  └─ If-Match ≠ current ETag → 409 Conflict (concurrent modification detected)
  └─ If-Match = current ETag → forward update to backend → 200 OK
```

---

## 🧪 Testing & CI/CD Pipeline

- **GitHub Actions CI/CD:** Configured with a dedicated workflow that triggers on every pull request. Runs `mvn clean test`, publishes test reports, and **blocks merges on test failure**.

- **Isolated test scope:** Frontend tests mock the `RestClient` layer — no real backend connection needed. Tests verify controller routing, model population, and Thymeleaf template binding independently of the backend.

---

## ⚙ Key Configuration (`application.properties`)

| Property | Value | Reason |
|---|---|---|
| `server.port` | `8081` | Separate port from backend |
| `backend.api.url` | `http://localhost:8080` | Injected into `RestClient` via `@Value` |
| `spring.thymeleaf.cache` | `false` | Template changes reflect immediately without restart |

---

## 📦 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 11 |
| Framework | Spring Boot, Spring MVC |
| Templating | Thymeleaf (SSR) |
| HTTP Client | Spring `RestClient` |
| JSON Parsing | Jackson (`ObjectMapper`, `JsonNode`) |
| Build | Maven, GitHub Actions |
| Utilities | Lombok |
