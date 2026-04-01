package com.capgemini.book_partner_frontend.model;

import lombok.Data;
import java.util.Map;

/**
 * Data Transfer Object (DTO) representing a Partner Store.
 * Used for binding form data from the UI and mapping JSON responses from the REST API.
 */
@Data
public class Store {
    private String storId;

    private String storName;
    private String storAddress;
    private String city;
    private String state;
    private String zip;

    // Captures HATEOAS links provided by Spring Data REST
    private Map<String, Map<String, String>> _links;

    /**
     * Resolves the Store ID.
     * Since Spring Data REST often hides the primary key in the HATEOAS links,
     * this method acts as a fallback to extract the ID from the self-referencing URI
     * if it wasn't explicitly provided by a form submission.
     *
     * @return The extracted or explicit Store ID.
     */

    public String getStorId() {
        // Return explicitly bound ID (e.g., from an HTML form submission)
        if (this.storId != null && !this.storId.isEmpty()) {
            return this.storId;
        }

        // Extract ID from the Spring Data REST self-link
        if (_links != null && _links.containsKey("self")) {
            String href = _links.get("self").get("href");
            return href.substring(href.lastIndexOf("/") + 1);
        }
        return null;
    }
}