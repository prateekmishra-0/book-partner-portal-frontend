package com.capgemini.book_partner_frontend.DTO.author;


import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorDto {

    private String auId; // Jackson will fill this in using the method below
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zip;
    private Integer contract;

    /**
     * Jackson calls this method the moment it sees the "_links" JSON node.
     * It grabs the URL: "http://localhost:8080/api/authors/172-32-1176"
     * and extracts just the "172-32-1176" part to set as the auId.
     */
    @JsonProperty("_links")
    private void unpackIdFromHref(Map<String, Object> links) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> self = (Map<String, String>) links.get("self");
            String href = self.get("href");
            
            if (href != null) {
                // Grab everything after the final "/"
                this.auId = href.substring(href.lastIndexOf("/") + 1);
            }
        } catch (Exception e) {
            // Fails silently if links are missing (e.g., when saving a new author)
        }
    }
}