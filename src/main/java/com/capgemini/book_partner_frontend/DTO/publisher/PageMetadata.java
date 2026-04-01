package com.capgemini.book_partner_frontend.DTO.publisher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PageMetadata(
    int size,
    long totalElements,
    int totalPages,
    int number
) {}
