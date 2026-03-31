package com.capgemini.book_partner_frontend.DTO.author;

import java.util.List;

public record PagedResult<T>(
    List<T> content,
    PageMetadata metadata
) {}