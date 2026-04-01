package com.capgemini.book_partner_frontend.DTO.title;

import java.util.List;

public record PagedResult<T>(List<T> content, PageMetadata metadata) {}
