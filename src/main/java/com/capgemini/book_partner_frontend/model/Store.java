package com.capgemini.book_partner_frontend.model;

import lombok.Data;
import java.util.Map;

@Data
public class Store {

    // Added to catch the ID when you type it into the HTML form
    private String storId;

    private String storName;
    private String storAddress;
    private String city;
    private String state;
    private String zip;

    private Map<String, Map<String, String>> _links;

    public String getStorId() {
        // 1. If you typed the ID in the HTML form, use that!
        if (this.storId != null && !this.storId.isEmpty()) {
            return this.storId;
        }
        // 2. If we are just reading data from the backend, slice it from the URL
        if (_links != null && _links.containsKey("self")) {
            String href = _links.get("self").get("href");
            return href.substring(href.lastIndexOf("/") + 1);
        }
        return null;
    }
}