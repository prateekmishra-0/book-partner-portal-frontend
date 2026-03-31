package com.capgemini.book_partner_frontend.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class SaleTestController {

    private final RestClient restClient;

    public SaleTestController() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8080")
                .build();
    }

    // -------------------------------------------------------------
    // READ OPERATIONS (GET)
    // -------------------------------------------------------------

    // TEST 1: Get All Sales (Check for 'totalAmount' calculation)
    @GetMapping("/test-sales")
    public String testGetAllSales() {
        return restClient.get()
                .uri("/api/sales")
                .retrieve()
                .body(String.class);
    }

    // TEST 2: Get a specific Sale using the Comma-Separated Composite Key
    @GetMapping("/test-sale-id")
    public String testSaleById() {
        return restClient.get()
                .uri("/api/sales/7131,N914008,PS2091?projection=saleDetail")
                .retrieve()
                .body(String.class);
    }

    // TEST 3: Search Sales by Store ID (Crucial for Page 3)
    @GetMapping("/test-sale-store")
    public String testSaleByStore() {
        return restClient.get()
                .uri("/api/sales/search/byStore?storId=7131")
                .retrieve()
                .body(String.class);
    }

    // -------------------------------------------------------------
    // WRITE OPERATIONS (POST & PATCH Only - No Deletes!)
    // -------------------------------------------------------------

    // TEST 4: Create a New Sale
    @GetMapping("/test-sale-create")
    public String testCreateSale() {
        String newSaleJson = """
            {
                "id": {
                    "storId": "7131",
                    "ordNum": "NEW-ORD-999",
                    "titleId": "BU1032"
                },
                "ordDate": "2026-03-31T10:00:00",
                "qty": 20,
                "payterms": "Net 60"
            }
            """;

        return restClient.post()
                .uri("/api/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .body(newSaleJson)
                .retrieve()
                .body(String.class);
    }

    // TEST 5: Update the Sale we just created
    @GetMapping("/test-sale-update")
    public String testUpdateSale() {
        String updateSaleJson = """
            {
                "qty": 50,
                "payterms": "Net 30"
            }
            """;

        return restClient.patch()
                .uri("/api/sales/7131,NEW-ORD-999,BU1032")
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateSaleJson)
                .retrieve()
                .body(String.class);
    }
}