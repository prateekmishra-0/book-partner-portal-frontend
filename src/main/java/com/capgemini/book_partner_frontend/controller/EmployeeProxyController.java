package com.capgemini.book_partner_frontend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/ui-api/employees")
public class EmployeeProxyController {

    private final RestClient restClient;

    public EmployeeProxyController(@Value("${backend.api.url}") String backendUrl) {
        this.restClient = RestClient.builder().baseUrl(backendUrl).build();
    }

    // ==========================================
    // API 1 & 2: The Master List (Pagination & Sorting)
    // ==========================================
    @GetMapping
    public ResponseEntity<String> getEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {

        String uri = String.format("/api/employees?page=%d&size=%d&projection=employeeSummary", page, size);
        if (sort != null && !sort.isEmpty()) {
            uri += "&sort=" + sort;
        }

        return restClient.get().uri(uri).retrieve().toEntity(String.class);
    }

    // ==========================================
    // API 3: Single Column Search
    // ==========================================
    // ==========================================
    // API 3: Single Column Search
    // ==========================================
    @GetMapping("/search/fname")
    public ResponseEntity<String> searchByFirstName(@RequestParam String name) {
        // CHANGED: Fixed the URI to match your custom @RestResource path
        String uri = String.format("/api/employees/search/fname?fname=%s&projection=employeeSummary", name);
        return restClient.get().uri(uri).retrieve().toEntity(String.class);
    }

    // ==========================================
    // API 5: Add New Employee
    // ==========================================
    @PostMapping
    public ResponseEntity<String> addEmployee(@RequestBody String employeeJson) {
        return restClient.post()
                .uri("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .body(employeeJson)
                .retrieve()
                .toEntity(String.class);
    }

    // ==========================================
    // API 6: Soft Delete Employee
    // ==========================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String id) {
        return restClient.delete()
                .uri("/api/employees/" + id)
                .retrieve()
                .toBodilessEntity();
    }

    // ==========================================
    // API 7: Update Employee (Partial Update)
    // ==========================================
    @PatchMapping("/{id}")
    public ResponseEntity<String> updateEmployee(@PathVariable String id, @RequestBody String updatesJson) {
        return restClient.patch()
                .uri("/api/employees/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatesJson)
                .retrieve()
                .toEntity(String.class);
    }

    // ==========================================
    // API 8: Fetch Employee Details (For Page 3)
    // ==========================================
    @GetMapping("/{id}")
    public ResponseEntity<String> getEmployeeDetails(@PathVariable String id) {
        // Uses the employeeDetail projection to fetch JOINed Job and Publisher descriptions
        String uri = String.format("/api/employees/%s?projection=employeeDetail", id);
        return restClient.get().uri(uri).retrieve().toEntity(String.class);
    }

    // ==========================================
    // API 4: Advanced Multi-Column Search
    // ==========================================
    @GetMapping("/search/advanced")
    public ResponseEntity<String> advancedSearch(
            @RequestParam(required = false) String fname,
            @RequestParam(required = false) String lname,
            @RequestParam(required = false) Integer jobLvl,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {

        // Dynamically build the URI based on which parameters the user typed
        StringBuilder uri = new StringBuilder("/api/employees/search/advanced?");
        if (fname != null && !fname.isEmpty()) uri.append("fname=").append(fname).append("&");
        if (lname != null && !lname.isEmpty()) uri.append("lname=").append(lname).append("&");
        if (jobLvl != null) uri.append("jobLvl=").append(jobLvl).append("&");

        // Add pagination and projection
        uri.append("page=").append(page).append("&size=").append(size).append("&projection=employeeSummary");

        if (sort != null && !sort.isEmpty()) {
            uri.append("&sort=").append(sort);
        }

        return restClient.get().uri(uri.toString()).retrieve().toEntity(String.class);
    }
}