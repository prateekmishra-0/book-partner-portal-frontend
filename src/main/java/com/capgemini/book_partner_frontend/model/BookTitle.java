package com.capgemini.book_partner_frontend.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class BookTitle {
    private String titleId;
    private String title;
    private Double price;

    // 1. Catch the hidden HATEOAS links
    private Map<String, Map<String, String>> _links;

    // 2. NEW: Slice the actual ID out of the URL (e.g., extracts "BU1032")
    public String getExtractedId() {
        if (_links != null && _links.containsKey("self")) {
            String href = _links.get("self").get("href");
            return href.substring(href.lastIndexOf("/") + 1);
        }
        return titleId; // Fallback
    }
}

