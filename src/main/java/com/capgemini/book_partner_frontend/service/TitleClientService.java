package com.capgemini.book_partner_frontend.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.capgemini.book_partner_frontend.DTO.author.AuthorDto;
import com.capgemini.book_partner_frontend.DTO.publisher.PublisherDto;
import com.capgemini.book_partner_frontend.DTO.title.PageMetadata;
import com.capgemini.book_partner_frontend.DTO.title.PagedResult;
import com.capgemini.book_partner_frontend.DTO.title.TitleDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class TitleClientService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AuthorClientService authorClientService;

    @Value("${backend.api.url}")
    private String backendUrl;
    public TitleClientService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, AuthorClientService authorClientService) {
        this.restClient = restClientBuilder.baseUrl(backendUrl).build();
        this.objectMapper = objectMapper;
        this.authorClientService = authorClientService;
    }

    public List<TitleDto> fetchTitles() {
        try {
            String jsonResponse = restClient.get().uri(backendUrl+"/api/titles").retrieve().body(String.class);
            if (jsonResponse == null) return Collections.emptyList();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode titlesNode = rootNode.at("/_embedded/titles");
            if (titlesNode.isMissingNode() || titlesNode.isEmpty()) return Collections.emptyList();
            return objectMapper.readerForListOf(TitleDto.class).readValue(titlesNode);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public void createTitle(TitleDto newTitle) {
        restClient.post().uri(backendUrl+"/api/titles").body(newTitle).retrieve().toBodilessEntity(); 
    }

    public void deleteTitle(String id) {
        restClient.delete().uri(backendUrl+"/api/titles/{id}", id).retrieve().toBodilessEntity(); 
    }

    public TitleDto fetchTitleById(String id) {
        try {
            TitleDto title = restClient.get().uri(backendUrl+"/api/titles/{id}", id).retrieve().body(TitleDto.class);
            // The backend returns pubId as a plain field, but publisher is lazy — 
            // fetch it separately using the pubId and inject it into the DTO.
            if (title != null && title.getPubId() != null) {
                PublisherDto publisher = fetchPublisherById(title.getPubId());
                title.setPublisher(publisher);
            }
            return title;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fetches a single Publisher from the backend by its pubId.
     * Used to enrich the TitleDto with publisher details on the details page.
     */
    private PublisherDto fetchPublisherById(String pubId) {
        try {
            return restClient.get()
                    .uri(backendUrl+"/api/publishers/{id}", pubId)
                    .retrieve()
                    .body(PublisherDto.class);
        } catch (Exception e) {
            System.err.println("Could not fetch publisher with id: " + pubId + " — " + e.getMessage());
            return null;
        }
    }

    public void updateTitle(String id, TitleDto updatedTitle) {
        // Removed the try-catch and onStatus so the Controller can actually catch the error!
        restClient.put()
                .uri(backendUrl+"/api/titles/{id}", id)
                .body(updatedTitle)
                .retrieve()
                .toBodilessEntity();
    }

    public List<AuthorDto> fetchAuthorsByTitle(String titleId) {
        try {
            System.out.println("=== Fetching authors for titleId: " + titleId + " ===");
            
            String jsonResponse = restClient.get()
                    .uri(backendUrl+"/api/titleAuthors/search/byTitle?titleId={id}&page=0&size=50", titleId)
                    .retrieve().body(String.class);
            
            if (jsonResponse == null) {
                System.out.println("API returned null response");
                return Collections.emptyList();
            }
            
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode titleAuthorsNode = rootNode.at("/_embedded/titleAuthors");
            
            if (titleAuthorsNode.isMissingNode() || titleAuthorsNode.isEmpty()) {
                System.out.println("No titleAuthors found");
                return Collections.emptyList();
            }
            
            List<AuthorDto> authors = new java.util.ArrayList<>();
            
            for (JsonNode taNode : titleAuthorsNode) {
                try {
                    // Extract auId from self link: format is "/api/titleAuthors/{auId}_{titleId}"
                    JsonNode selfLink = taNode.at("/_links/self/href");
                    if (!selfLink.isMissingNode()) {
                        String href = selfLink.asText();
                        // Extract auId from URL: http://localhost:8080/api/titleAuthors/{auId}_{titleId}
                        String compositeKey = href.substring(href.lastIndexOf('/') + 1);
                        String auId = compositeKey.split("_")[0];
                        
                        System.out.println("Extracted auId: " + auId);
                        
                        // Fetch full author details
                        AuthorDto author = authorClientService.fetchAuthorById(auId);
                        if (author != null) {
                            authors.add(author);
                            System.out.println("Added author: " + author.getFirstName() + " " + author.getLastName());
                        } else {
                            System.out.println("Failed to fetch author with auId: " + auId);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error processing TitleAuthor record: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
            
            System.out.println("=== Total authors fetched: " + authors.size() + " ===");
            return authors;
        } catch (Exception e) {
            System.err.println("Error fetching authors by title: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    
    /**
     * Replaces all author associations for a title.
     * Step 1: Fetch existing associations via the search endpoint.
     * Step 2: Delete each one individually using the composite key (auId_titleId).
     * Step 3: Add the new selected author associations.
     * This guarantees the final state exactly matches the selected list.
     */
    public void saveTitleAuthors(String titleId, List<String> authorIds) {
        try {
            // Step 1: Fetch all current associations for this title
            String jsonResponse = restClient.get()
                    .uri(backendUrl+"/api/titleAuthors/search/byTitle?titleId={id}&page=0&size=100", titleId)
                    .retrieve()
                    .body(String.class);

            // Step 2: Delete each existing association by its composite key
            if (jsonResponse != null) {
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode titleAuthorsNode = rootNode.at("/_embedded/titleAuthors");
                if (!titleAuthorsNode.isMissingNode() && !titleAuthorsNode.isEmpty()) {
                    for (JsonNode taNode : titleAuthorsNode) {
                        JsonNode selfLink = taNode.at("/_links/self/href");
                        if (!selfLink.isMissingNode()) {
                            String href = selfLink.asText();
                            // compositeKey format: "auId_titleId"  e.g. "172-32-1176_BU1032"
                            String compositeKey = href.substring(href.lastIndexOf('/') + 1);
                            restClient.delete()
                                    .uri(backendUrl+"/api/titleAuthors/{key}", compositeKey)
                                    .retrieve()
                                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                                        System.err.println("Delete titleAuthor failed for key: " + compositeKey);
                                    })
                                    .toBodilessEntity();
                        }
                    }
                }
            }

            // Step 3: Add new associations
            for (int i = 0; i < authorIds.size(); i++) {
                String auId = authorIds.get(i);
                String payload = "{\"id\":{\"titleId\":\"" + titleId + "\",\"auId\":\"" + auId + "\"},\"auOrd\":" + (i + 1) + ",\"royaltyPer\":0}";
                restClient.post()
                        .uri(backendUrl+"/api/titleAuthors")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetches all publishers from the backend for use in the add/edit form dropdown.
     */
    public List<PublisherDto> fetchAllPublishers() {
        try {
            String jsonResponse = restClient.get().uri(backendUrl+"/api/publishers").retrieve().body(String.class);
            if (jsonResponse == null) return Collections.emptyList();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode publishersNode = rootNode.at("/_embedded/publishers");
            if (publishersNode.isMissingNode() || publishersNode.isEmpty()) return Collections.emptyList();
            return objectMapper.readerForListOf(PublisherDto.class).readValue(publishersNode);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public PagedResult<TitleDto> fetchTitlesPaginated(int page, int size, String searchBy, String keyword,
                                                       String sortBy, String sortDir) {
        try {
            // Build sort segment: &sort=fieldName,direction
            String sortParam = (sortBy != null && !sortBy.isBlank())
                    ? "&sort=" + sortBy + "," + (sortDir != null && sortDir.equals("desc") ? "desc" : "asc")
                    : "";

            String uri = backendUrl+"/api/titles?page=" + page + "&size=" + size + sortParam;

            if (keyword != null && !keyword.trim().isEmpty() && searchBy != null) {
                String searchPath = switch (searchBy) {
                    case "title" -> "search/name?title=" + keyword;
                    case "type" -> "search/type?type=" + keyword;
                    case "price-exact" -> "search/price-exact?price=" + keyword;
                    case "price-gt" -> "search/price-gt?price=" + keyword;
                    case "price-lt" -> "search/price-lt?price=" + keyword;
                    default -> "";
                };
                if (!searchPath.isEmpty()) {
                    uri =backendUrl+ "/api/titles/" + searchPath + "&page=" + page + "&size=" + size + sortParam;
                }
            }

            String jsonResponse = restClient.get().uri(uri).retrieve().body(String.class);
            if (jsonResponse == null) return new PagedResult<>(Collections.emptyList(), new PageMetadata(0, 0, 0, 0));

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode titlesNode = rootNode.at("/_embedded/titles");
            List<TitleDto> titles = (titlesNode.isMissingNode() || titlesNode.isEmpty()) 
                ? Collections.emptyList() 
                : objectMapper.readerForListOf(TitleDto.class).readValue(titlesNode);

            JsonNode pageNode = rootNode.at("/page");
            PageMetadata pageMeta = new PageMetadata(
                pageNode.at("/number").asInt(0),
                pageNode.at("/size").asInt(5),
                pageNode.at("/totalElements").asInt(0),
                pageNode.at("/totalPages").asInt(0)
            );

            return new PagedResult<>(titles, pageMeta);
        } catch (Exception e) {
            e.printStackTrace();
            return new PagedResult<>(Collections.emptyList(), new PageMetadata(0, 0, 0, 0));
        }
    }

    /**
     * Advanced search with price comparison
     */
    public PagedResult<TitleDto> searchTitlesAdvanced(int page, int size, String searchType, String searchValue) {
        try {
            String uri =backendUrl+ "/api/titles?page=" + page + "&size=" + size;

            if (searchValue != null && !searchValue.trim().isEmpty()) {
                String searchPath = switch (searchType) {
                    case "title" -> "search/name?title=" + searchValue;
                    case "type" -> "search/type?type=" + searchValue;
                    case "price-exact" -> "search/price-exact?price=" + searchValue;
                    case "price-gt" -> "search/price-gt?price=" + searchValue;
                    case "price-lt" -> "search/price-lt?price=" + searchValue;
                    default -> "";
                };
                if (!searchPath.isEmpty()) {
                    uri = backendUrl+"/api/titles/" + searchPath + "&page=" + page + "&size=" + size;
                }
            }

            String jsonResponse = restClient.get().uri(uri).retrieve().body(String.class);
            if (jsonResponse == null) return new PagedResult<>(Collections.emptyList(), new PageMetadata(0, 0, 0, 0));

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode titlesNode = rootNode.at("/_embedded/titles");
            List<TitleDto> titles = (titlesNode.isMissingNode() || titlesNode.isEmpty()) 
                ? Collections.emptyList() 
                : objectMapper.readerForListOf(TitleDto.class).readValue(titlesNode);

            JsonNode pageNode = rootNode.at("/page");
            PageMetadata pageMeta = new PageMetadata(
                pageNode.at("/number").asInt(0),
                pageNode.at("/size").asInt(5),
                pageNode.at("/totalElements").asInt(0),
                pageNode.at("/totalPages").asInt(0)
            );

            return new PagedResult<>(titles, pageMeta);
        } catch (Exception e) {
            e.printStackTrace();
            return new PagedResult<>(Collections.emptyList(), new PageMetadata(0, 0, 0, 0));
        }
    }
}
