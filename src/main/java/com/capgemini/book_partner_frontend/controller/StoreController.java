package com.capgemini.book_partner_frontend.controller;

import com.capgemini.book_partner_frontend.model.Store;
import com.capgemini.book_partner_frontend.model.StoreResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class StoreController {

    private final RestClient restClient;

    public StoreController() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8080")
                .build();
    }

    @GetMapping("/stores")
    public String showStoresPage(
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String searchKeyword,
            Model model) {

        StoreResponse response;

        if (searchType != null && searchKeyword != null && !searchKeyword.isBlank()) {
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

    @PostMapping("/stores/add")
    public String addStore(@ModelAttribute Store newStore, RedirectAttributes redirectAttributes) {

        // 1. SANITIZE THE DATA: Convert empty strings to actual nulls
        // This prevents the backend's strict Regex validations from panicking over empty boxes
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
        return "redirect:/stores";
    }

    @PostMapping("/stores/edit")
    public String editStore(@ModelAttribute Store updatedStore, RedirectAttributes redirectAttributes) {

        // 1. SANITIZE THE DATA: Convert empty strings to actual nulls
        // This stops the backend Regex from panicking when a user clears a field
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
            // 2. Catch 400 Bad Request (Validation Errors from the backend)
            redirectAttributes.addFlashAttribute("errorMessage", "Update Failed: Please check your formatting (e.g., Zip must be 5 digits).");
        } catch (Exception e) {
            // 3. Catch generic 500 Errors
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update store.");
        }

        return "redirect:/stores";
    }
}