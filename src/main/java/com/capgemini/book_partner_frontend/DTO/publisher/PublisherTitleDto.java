package com.capgemini.book_partner_frontend.DTO.publisher;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublisherTitleDto {

    private String titleId;
    private String title;
    private String type;
    private Double price;
    private Integer royalty;
    private Integer ytdSales;
    private String notes;

    @JsonProperty("_links")
    private void unpackIdFromHref(Map<String, Object> links) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> self = (Map<String, String>) links.get("self");
            String href = self.get("href");
            if (href != null) {
                this.titleId = href.substring(href.lastIndexOf("/") + 1);
            }
        } catch (Exception e) {
            // Ignore parsing errors for non-HAL payloads.
        }
    }
}
