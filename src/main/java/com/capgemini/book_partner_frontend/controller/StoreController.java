package com.capgemini.book_partner_frontend.controller;

import com.capgemini.book_partner_frontend.model.Store;
import com.capgemini.book_partner_frontend.model.StoreResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class StoreController {

    private final RestClient restClient;

    public StoreController() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8080")
                .build();
    }

    // --- 1. DISPLAY STORES & SEARCH ---
    @GetMapping("/stores")
    public String showStoresPage(
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String searchKeyword,
            Model model) {

        StoreResponse response;

        if (searchType != null && searchKeyword != null && !searchKeyword.isBlank()) {

            // NEW: Use UriComponentsBuilder for safe URL encoding!
            // This safely translates spaces into "%20" so the backend can read them.
            String safeUrl = UriComponentsBuilder.fromPath("/api/stores/search/" + searchType)
                    .queryParam(searchType, searchKeyword.trim())
                    .build()
                    .toUriString();

            response = restClient.get().uri(safeUrl).retrieve().body(StoreResponse.class);

        } else {
            response = restClient.get().uri("/api/stores").retrieve().body(StoreResponse.class);
        }

        if (response != null && response.get_embedded() != null) {
            model.addAttribute("storesList", response.get_embedded().getStores());
        }

        model.addAttribute("searchType", searchType);
        model.addAttribute("searchKeyword", searchKeyword);
        model.addAttribute("newStore", new Store());

        return "stores/stores_list";
    }

    // --- 2. CATCH THE FORM SUBMISSION (ADD NEW STORE) ---
    @PostMapping("/stores/add")
    public String addStore(@ModelAttribute Store newStore) {
        try {
            // Send the filled-out Store object to your backend as JSON
            restClient.post()
                    .uri("/api/stores")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(newStore)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.out.println("Error creating store: " + e.getMessage());
            // It will print the 409 Conflict here if you try to add a duplicate ID!
        }

        // Instantly refresh the page to show the new data in the table
        return "redirect:/stores";
    }

    // --- 3. DELETE STORE ---
    @GetMapping("/stores/delete/{id}")
    public String deleteStore(@PathVariable String id) {
        try {
            restClient.delete()
                    .uri("/api/stores/" + id)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.out.println("Error deleting store: " + e.getMessage());
        }
        // Instantly refresh the page so the deleted store disappears
        return "redirect:/stores";
    }

    // --- 4. UPDATE STORE ---
    @PostMapping("/stores/edit")
    public String editStore(@ModelAttribute Store updatedStore) {
        try {
            // We use PATCH here to update existing data
            restClient.patch()
                    .uri("/api/stores/" + updatedStore.getStorId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(updatedStore)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.out.println("Error updating store: " + e.getMessage());
        }
        // Instantly refresh the page to see the changes
        return "redirect:/stores";
    }
}