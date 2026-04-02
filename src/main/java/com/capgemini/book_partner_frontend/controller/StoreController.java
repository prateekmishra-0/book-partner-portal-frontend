package com.capgemini.book_partner_frontend.controller;

import com.capgemini.book_partner_frontend.model.Store;
import com.capgemini.book_partner_frontend.model.StoreResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Controller responsible for managing Partner Stores UI interactions.
 * Acts as a frontend client communicating with the backend REST API.
 */
@Controller
public class StoreController {

    private final RestClient restClient;
//    @Value("${backend.api.url}")
//    private String backendUrl;

    /**
     * Initializes the REST client with the backend API base URL.
     */
    public StoreController(@Value("${backend.api.url}") String backendUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(backendUrl)
                .build();
    }

    @GetMapping("/stores")
    public String showStoresPage(
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String searchKeyword,
            Model model) {

        StoreResponse response;

        if (searchType != null && searchKeyword != null && !searchKeyword.isBlank()) {
            // Build safe URI for custom search endpoints
            String safeUrl = UriComponentsBuilder.fromPath("/api/stores/search/" + searchType)
                    .queryParam(searchType, searchKeyword.trim())
                    .build()
                    .toUriString();
            response = restClient.get().uri(safeUrl).retrieve().body(StoreResponse.class);
        } else {
            // Retrieve default collection if no search parameters are provided
            response = restClient.get().uri("/api/stores").retrieve().body(StoreResponse.class);
        }

        if (response != null && response.get_embedded() != null) {
            model.addAttribute("storesList", response.get_embedded().getStores());
        }

        model.addAttribute("searchType", searchType);
        model.addAttribute("searchKeyword", searchKeyword);

        // Initialize an empty form object unless one was passed via redirect attributes (on error)
        if (!model.containsAttribute("newStore")) {
            model.addAttribute("newStore", new Store());
        }

        return "stores/stores_list";
    }

    @PostMapping("/stores/add")
    public String addStore(@ModelAttribute Store newStore, RedirectAttributes redirectAttributes) {

        // 1. SANITIZE THE DATA: Convert empty strings to actual nulls
        if (newStore.getZip() != null && newStore.getZip().trim().isEmpty()) {
            newStore.setZip(null);
        }
        if (newStore.getState() != null && newStore.getState().trim().isEmpty()) {
            newStore.setState(null);
        }
        if (newStore.getState() != null) {
            newStore.setState(newStore.getState().toUpperCase());
        }

        try {
            restClient.post()
                    .uri("/api/stores")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(newStore)
                    .retrieve()
                    .toBodilessEntity();

            redirectAttributes.addFlashAttribute("successMessage", "Store '" + newStore.getStorName() + "' was successfully added!");

        } catch (org.springframework.web.client.HttpClientErrorException.BadRequest e) {
            // 2. Catch 400 Bad Request (Validation Errors from the backend)
            redirectAttributes.addFlashAttribute("duplicateIdError", "Validation Failed: Please check your formatting (e.g., Zip must be 5 digits).");
            redirectAttributes.addFlashAttribute("newStore", newStore);

        } catch (Exception e) {
            // 3. Catch 500 Errors (Database panic for Duplicate IDs)
            redirectAttributes.addFlashAttribute("duplicateIdError", "Action Failed: This Store ID already exists.");
            redirectAttributes.addFlashAttribute("newStore", newStore);
        }

        return "redirect:/stores";
    }

    @GetMapping("/stores/delete/{id}")
    public String deleteStore(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            restClient.delete().uri("/api/stores/" + id).retrieve().toBodilessEntity();
            redirectAttributes.addFlashAttribute("successMessage", "Store deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete store.");
        }
        // Note: It is correct to redirect back to /stores after a delete because the Sales Ledger you were just looking at no longer exists!
        return "redirect:/stores";
    }

    @PostMapping("/stores/edit")
    public String editStore(@ModelAttribute Store updatedStore, RedirectAttributes redirectAttributes) {

        // 1. SANITIZE THE DATA: Convert empty strings to actual nulls
        if (updatedStore.getZip() != null && updatedStore.getZip().trim().isEmpty()) {
            updatedStore.setZip(null);
        }
        if (updatedStore.getState() != null && updatedStore.getState().trim().isEmpty()) {
            updatedStore.setState(null);
        }
        if (updatedStore.getState() != null) {
            updatedStore.setState(updatedStore.getState().toUpperCase());
        }

        try {
            restClient.patch()
                    .uri("/api/stores/" + updatedStore.getStorId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(updatedStore)
                    .retrieve()
                    .toBodilessEntity();
            redirectAttributes.addFlashAttribute("successMessage", "Store updated successfully!");

        } catch (org.springframework.web.client.HttpClientErrorException.BadRequest e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Update Failed: Please check your formatting (e.g., Zip must be 5 digits).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update store.");
        }

        // FIX: Redirect back to the specific Store's Sales Ledger instead of the main directory!
        return "redirect:/stores/" + updatedStore.getStorId() + "/sales";
    }
}