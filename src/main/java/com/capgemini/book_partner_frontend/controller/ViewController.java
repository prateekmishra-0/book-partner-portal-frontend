package com.capgemini.book_partner_frontend.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class ViewController {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String backendApiUrl;

    public ViewController(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${backend.api.url}") String backendApiUrl) {
        this.restClient = restClientBuilder.baseUrl(backendApiUrl).build();
        this.objectMapper = objectMapper;
        this.backendApiUrl = backendApiUrl;
    }

    @GetMapping("/employees")
    public String showEmployeesPage() {
        // Tells Spring to look in templates/employee/ for employees.html
        return "employee/employees";
    }

    // NEW: Serve the details page
    @GetMapping("/employee-details")
    public String showEmployeeDetailsPage() {
        return "employee/employee-details";
    }

    @GetMapping("/api-docs")
    public String showApiDocs() {
        return "redirect:/api-docs/swagger-ui/index.html";
    }

    @GetMapping("/api-docs/swagger-ui/**")
    @ResponseBody
    public ResponseEntity<byte[]> proxySwaggerUi(HttpServletRequest request) {
        String requestPath = request.getRequestURI().substring("/api-docs".length());
        return proxyGet(requestPath, request.getQueryString());
    }

    @GetMapping({"/v3/api-docs", "/v3/api-docs/**"})
    @ResponseBody
    public ResponseEntity<byte[]> proxyOpenApi(HttpServletRequest request) {
        return proxyGet(request.getRequestURI(), request.getQueryString());
    }

    private ResponseEntity<byte[]> proxyGet(String path, String queryString) {
        String uri = (queryString == null || queryString.isBlank()) ? path : path + "?" + queryString;

        ResponseEntity<byte[]> backendResponse = restClient.get()
                .uri(uri)
                .retrieve()
                .toEntity(byte[].class);

        HttpHeaders headers = new HttpHeaders();
        MediaType contentType = backendResponse.getHeaders().getContentType();
        if (contentType != null) {
            headers.setContentType(contentType);
        }

        return new ResponseEntity<>(backendResponse.getBody(), headers, backendResponse.getStatusCode());
    }
}