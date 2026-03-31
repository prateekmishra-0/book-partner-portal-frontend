package com.capgemini.book_partner_frontend.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

// We use @RestController here just for testing to dump raw JSON to the screen
@RestController
public class StoreTestController {

    private final RestClient restClient;

    public StoreTestController() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8080")
                .build();
    }

    // TEST 1: All Stores (Already verified, but keep for reference)
    @GetMapping("/test-stores")
    public String testGetAll() {
        return restClient.get().uri("/api/stores").retrieve().body(String.class);
    }

    // TEST 2: Search by City (Uses your backend path: /search/city)
    @GetMapping("/test-city")
    public String testCity() {
        // We will test with 'Seattle' since it's in your insertdata.sql
        return restClient.get()
                .uri("/api/stores/search/city?city=Seattle")
                .retrieve()
                .body(String.class);
    }

    // TEST 3: Search by State (Uses your backend path: /search/state)
    @GetMapping("/test-state")
    public String testState() {
        // Testing with 'CA' (California)
        return restClient.get()
                .uri("/api/stores/search/state?state=CA")
                .retrieve()
                .body(String.class);
    }

    // TEST 4: Search by Name (Uses your backend path: /search/name)
    @GetMapping("/test-name")
    public String testName() {
        // Testing with 'Barnum'
        return restClient.get()
                .uri("/api/stores/search/name?name=Barnum")
                .retrieve()
                .body(String.class);
    }

    // TEST 5: Create (POST) a new store
    @GetMapping("/test-create")
    public String testCreate() {
        // We create a simple Map to represent the JSON object
        Map<String, Object> newStore = new HashMap<>();
        newStore.put("storId", "9991"); // Must be exactly 4 chars
        newStore.put("storName", "Vedika's Test Hub");
        newStore.put("city", "Nagpur");
        newStore.put("state", "MH");
        newStore.put("zip", "44001"); // Must be exactly 5 digits

        return restClient.post()
                .uri("/api/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .body(newStore)
                .retrieve()
                .body(String.class);
    }

    // TEST 6: Update (PUT) the store we just created
    @GetMapping("/test-update")
    public String testUpdate() {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("storName", "Updated Name");
        updatedData.put("city", "Mumbai");

        return restClient.put()
                .uri("/api/stores/9991")
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatedData)
                .retrieve()
                .body(String.class);
    }

    // TEST 7: Delete (DELETE) the test store
    @GetMapping("/test-delete")
    public String testDelete() {
        restClient.delete()
                .uri("/api/stores/9991")
                .retrieve()
                .toBodilessEntity(); // Delete doesn't return a body

        return "Store 9991 deleted successfully (Soft Delete triggered)";
    }
}