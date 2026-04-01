package com.capgemini.book_partner_frontend.controller;

import com.capgemini.book_partner_frontend.model.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
public class SalesController {

    private final RestClient restClient;

    public SalesController() {
        this.restClient = RestClient.builder().baseUrl("http://localhost:8080").build();
    }

    @GetMapping("/stores/{id}/sales")
    public String showSalesPage(@PathVariable String id, Model model) {
        Store store = restClient.get().uri("/api/stores/" + id).retrieve().body(Store.class);
        SaleResponse salesData = restClient.get()
                .uri("/api/sales/search/byStore?storId=" + id + "&projection=saleDetail")
                .retrieve().body(SaleResponse.class);
        BookTitleResponse bookData = restClient.get()
                .uri("/api/titles")
                .retrieve()
                .body(BookTitleResponse.class);

        model.addAttribute("store", store);
        if (salesData != null && salesData.get_embedded() != null) {
            model.addAttribute("salesList", salesData.get_embedded().getSales());
        }
        if (bookData != null && bookData.get_embedded() != null) {
            model.addAttribute("availableBooks", bookData.get_embedded().getTitles());
        }

        model.addAttribute("newSale", new Sale());

        return "sales/sales_list";
    }

    // --- ADD SALE ---
    @PostMapping("/stores/{id}/sales/add")
    public String addSale(@PathVariable String id, @ModelAttribute Sale newSale, @RequestParam String titleId, RedirectAttributes redirectAttributes) {

        String formattedDate = newSale.getOrdDate();
        if (formattedDate != null && !formattedDate.contains("T")) {
            formattedDate += "T00:00:00";
        }
        String safePayTerms = (newSale.getPayterms() != null && !newSale.getPayterms().trim().isEmpty()) ? newSale.getPayterms() : "Net 30";

        Map<String, Object> compositeId = new HashMap<>();
        compositeId.put("storId", id);
        compositeId.put("ordNum", newSale.getOrdNum());
        compositeId.put("titleId", titleId);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", compositeId);
        requestBody.put("ordDate", formattedDate);
        requestBody.put("qty", newSale.getQty());
        requestBody.put("payterms", safePayTerms);
        requestBody.put("store", "http://localhost:8080/api/stores/" + id);
        requestBody.put("title", "http://localhost:8080/api/titles/" + titleId);

        try {
            restClient.post().uri("/api/sales").contentType(MediaType.APPLICATION_JSON).body(requestBody).retrieve().toBodilessEntity();
            redirectAttributes.addFlashAttribute("successMessage", "Sale successfully recorded!");
        } catch (Exception e) {
            // Now the user will actually see the error!
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save: Quantity may exceed limits or Order Number already exists.");
        }
        return "redirect:/stores/" + id + "/sales";
    }

    // --- EDIT SALE ---
    @PostMapping("/stores/{id}/sales/edit")
    public String editSale(@PathVariable String id,
                           @RequestParam String compositeId,
                           @RequestParam Integer qty,
                           @RequestParam String payterms,
                           RedirectAttributes redirectAttributes) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("qty", qty);
        requestBody.put("payterms", payterms);

        try {
            restClient.patch()
                    .uri("/api/sales/" + compositeId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
            redirectAttributes.addFlashAttribute("successMessage", "Sale successfully updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update sale. Check your values.");
        }
        return "redirect:/stores/" + id + "/sales";
    }
}