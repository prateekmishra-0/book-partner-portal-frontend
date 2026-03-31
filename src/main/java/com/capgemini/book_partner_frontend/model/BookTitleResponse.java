package com.capgemini.book_partner_frontend.model;

import lombok.Data;

import java.util.List;

// Added 'public' here so the Controller can find it!
@Data
public class BookTitleResponse {

    private Embedded _embedded;
    @Data
    public static class Embedded {
        private List<BookTitle> titles;
    }
}
