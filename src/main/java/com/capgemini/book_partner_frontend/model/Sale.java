package com.capgemini.book_partner_frontend.model;

import lombok.Data;
import java.util.Map;

@Data
public class Sale {
    // Basic fields from the JSON
    private String ordNum;
    private String ordDate;
    private Integer qty;
    private String payterms;
    private Double totalAmount; //calculated math field

    // Catches the nested "title" object in the JSON
    private TitleInfo title;

    // Catches the HATEOAS links
    private Map<String, Map<String, String>> _links;

    // Inner class to map the nested JSON structure
    @Data
    public static class TitleInfo {
        private String title;
        private Double price;
    }

    // Helper method to slice the exact Composite ID (e.g., "7131,N914008,PS2091")
    // out of the URL so we can use it for Editing later!
    public String getCompositeId() {
        if (_links != null && _links.containsKey("self")) {
            String href = _links.get("self").get("href");
            return href.substring(href.lastIndexOf("/") + 1);
        }
        return null;
    }
}