package com.capgemini.book_partner_frontend.model;

import lombok.Data;
import java.util.List;

@Data
public class SaleResponse {

    private Embedded _embedded;

    @Data
    public static class Embedded {
        private List<Sale> sales;
    }
}