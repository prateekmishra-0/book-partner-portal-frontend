package com.capgemini.book_partner_frontend.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.capgemini.book_partner_frontend.DTO.author.AuthorDto;
import com.capgemini.book_partner_frontend.DTO.author.AuthorTitleDto;
import com.capgemini.book_partner_frontend.DTO.author.PageMetadata;
import com.capgemini.book_partner_frontend.DTO.author.PagedResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuthorClientService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${backend.api.url}")
    private String backendUrl;

    public AuthorClientService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        // We still keep the baseUrl here just in case, but we will be explicit in our methods
        this.restClient = restClientBuilder.baseUrl(backendUrl).build();
        this.objectMapper = objectMapper;
    }

    public List<AuthorDto> fetchAuthors() {
        try {
            // FIX: Prepend backendUrl
            String jsonResponse = restClient.get()
                    .uri(backendUrl + "/api/authors")
                    .retrieve()
                    .body(String.class);
                    
            if (jsonResponse == null) return Collections.emptyList();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode authorsNode = rootNode.at("/_embedded/authors");
            if (authorsNode.isMissingNode() || authorsNode.isEmpty()) return Collections.emptyList();
            return objectMapper.readerForListOf(AuthorDto.class).readValue(authorsNode);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public void createAuthor(AuthorDto newAuthor) {
        // FIX: Prepend backendUrl
        restClient.post()
                .uri(backendUrl + "/api/authors")
                .body(newAuthor)
                .retrieve()
                .toBodilessEntity(); 
    }

    public void deleteAuthor(String id) {
        // FIX: Prepend backendUrl
        restClient.delete()
                .uri(backendUrl + "/api/authors/{id}", id)
                .retrieve()
                .toBodilessEntity(); 
    }

    public AuthorDto fetchAuthorById(String id) {
        try {
            // FIX: Prepend backendUrl
            return restClient.get()
                    .uri(backendUrl + "/api/authors/{id}", id)
                    .retrieve()
                    .body(AuthorDto.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null; 
        }
    }

    public void updateAuthor(String id, AuthorDto updatedAuthor) {
        // Removed the try-catch and onStatus so the Controller can actually catch the error!
        restClient.put()
                .uri(backendUrl + "/api/authors/{id}", id)
                .body(updatedAuthor)
                .retrieve()
                .toBodilessEntity();
    }

    public List<AuthorTitleDto> fetchTitlesByAuthor(String auId) {
        try {
            // FIX: Prepend backendUrl
            String jsonResponse = restClient.get()
                    .uri(backendUrl + "/api/titleAuthors/search/byAuthor?auId={id}", auId)
                    .retrieve()
                    .body(String.class);
                    
            if (jsonResponse == null) return Collections.emptyList();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode titlesNode = rootNode.at("/_embedded/titleAuthors");
            if (titlesNode.isMissingNode() || titlesNode.isEmpty()) return Collections.emptyList();
            return objectMapper.readerForListOf(AuthorTitleDto.class).readValue(titlesNode);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // --- DROPDOWN SEARCH LOGIC ---
    public PagedResult<AuthorDto> fetchAuthorsPaginated(int page, int size, String searchBy, String keyword, String sort, String dir) {
        try {
            // 1. Start building the URI Path
            StringBuilder uriPath = new StringBuilder();

            // 2. Determine if we are using a Search endpoint or a FindAll endpoint
            if (keyword != null && !keyword.trim().isEmpty() && searchBy != null) {
                String searchPath = switch (searchBy) {
                    case "firstName" -> "firstname?firstName=" + keyword;
                    case "lastName" -> "lastname?lastName=" + keyword;
                    case "city" -> "city?city=" + keyword;
                    case "phone" -> "phone?phone=" + keyword;
                    case "state" -> "state?state=" + keyword;
                    default -> "";
                };
                
                if (!searchPath.isEmpty()) {
                    uriPath.append("/api/authors/search/").append(searchPath);
                } else {
                    uriPath.append("/api/authors?");
                }
            } else {
                uriPath.append("/api/authors?");
            }

            // 3. Append Pagination and Sorting
            String separator = uriPath.toString().contains("?") ? "&" : "?";
            
            uriPath.append(separator)
                .append("page=").append(page)
                .append("&size=").append(size)
                .append("&sort=").append(sort).append(",").append(dir);

            // 4. Fetch from backend
            // FIX: Prepend the backendUrl explicitly
            String fullUri = this.backendUrl + uriPath.toString();

            String jsonResponse = restClient.get()
                    .uri(fullUri)
                    .retrieve()
                    .body(String.class);

            if (jsonResponse == null) return new PagedResult<>(Collections.emptyList(), null);

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            
            // 5. Extract Data
            JsonNode authorsNode = rootNode.at("/_embedded/authors");
            List<AuthorDto> authors = Collections.emptyList();
            if (!authorsNode.isMissingNode() && !authorsNode.isEmpty()) {
                authors = objectMapper.readerForListOf(AuthorDto.class).readValue(authorsNode);
            }

            // 6. Extract Metadata
            JsonNode pageNode = rootNode.at("/page");
            PageMetadata metadata = null;
            if (!pageNode.isMissingNode()) {
                metadata = objectMapper.treeToValue(pageNode, PageMetadata.class);
            }

            return new PagedResult<>(authors, metadata);

        } catch (Exception e) {
            System.err.println("Error fetching paginated authors: " + e.getMessage());
            e.printStackTrace();
            return new PagedResult<>(Collections.emptyList(), null);
        }
    }
}