package com.capgemini.book_partner_frontend.DTO.title;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.capgemini.book_partner_frontend.DTO.author.PublisherDto;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TitleDto {

    private String titleId;
    private String title;
    private String type;
    private Double price;
    private Double advance;
    private Integer royalty;
    private Integer ytdSales;
    private String notes;
    @DateTimeFormat(pattern = "uuuu-MM-dd'T'HH:mm")
    private LocalDateTime pubdate;
    private Boolean isActive;
    private String pubId;
    private PublisherDto publisher;

    /**
     * Jackson calls this method the moment it sees the "_links" JSON node.
     * It extracts the titleId from the href.
     */
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
            // Fails silently if links are missing
        }
    }
}