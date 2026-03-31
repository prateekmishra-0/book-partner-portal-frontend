package com.capgemini.book_partner_frontend.DTO.author;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorTitleDto {
    private String bookTitle;
    private String publisherName;
    private Integer royaltyPer;
    private Double price;
}