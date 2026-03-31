package com.capgemini.book_partner_frontend.model;

import lombok.Data;
import java.util.List;

@Data
public class StoreResponse {
    // Catches the "_embedded" JSON block
    private Embedded _embedded;

    @Data
    public static class Embedded {
        // Catches the "stores" array inside _embedded
        private List<Store> stores;
    }
}