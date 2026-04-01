package com.capgemini.book_partner_frontend.model;

import lombok.Data;
import java.util.List;

/**
 * Wrapper class to deserialize Spring Data REST collection responses.
 * Maps the standard HAL (Hypertext Application Language) JSON structure.
 */

@Data
public class StoreResponse {
    // Maps the HAL "_embedded" node containing the actual resource collection
    private Embedded _embedded;

    /**
     * Inner class representing the embedded resources payload.
     */
    @Data
    public static class Embedded {
        // Maps the array of Store objects returned by the API
        private List<Store> stores;
    }
}