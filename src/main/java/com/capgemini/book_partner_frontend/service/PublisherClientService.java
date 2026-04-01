package com.capgemini.book_partner_frontend.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.capgemini.book_partner_frontend.DTO.publisher.PageMetadata;
import com.capgemini.book_partner_frontend.DTO.publisher.PagedResult;
import com.capgemini.book_partner_frontend.DTO.publisher.PublisherDto;
import com.capgemini.book_partner_frontend.DTO.publisher.PublisherTitleDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PublisherClientService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PublisherClientService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${backend.api.url}") String backendApiUrl) {
        this.restClient = restClientBuilder.baseUrl(backendApiUrl).build();
        this.objectMapper = objectMapper;
    }

    public void createPublisher(PublisherDto newPublisher) {
        restClient.post()
                .uri("/api/publishers")
                .body(newPublisher)
                .retrieve()
                .toBodilessEntity();
    }

    public void updatePublisher(String id, PublisherDto updatedPublisher) {
        restClient.put()
                .uri("/api/publishers/{id}", id)
                .body(updatedPublisher)
                .retrieve()
                .toBodilessEntity();
    }

    public void deletePublisher(String id) {
        restClient.delete()
                .uri("/api/publishers/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }

    public PagedResult<PublisherDto> fetchPublishersPaginated(
            int page,
            int size,
            String searchBy,
            String keyword,
            String sort,
            String dir) {
        try {
            String uri = buildListOrSearchUri(page, size, searchBy, keyword, sort, dir);

            String jsonResponse = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (jsonResponse == null) {
                return new PagedResult<>(Collections.emptyList(), null);
            }

            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            JsonNode publishersNode = rootNode.at("/_embedded/publishers");
            List<PublisherDto> publishers = Collections.emptyList();
            if (!publishersNode.isMissingNode() && !publishersNode.isEmpty()) {
                publishers = objectMapper.readerForListOf(PublisherDto.class).readValue(publishersNode);
            }

            JsonNode pageNode = rootNode.at("/page");
            PageMetadata metadata = null;
            if (!pageNode.isMissingNode()) {
                metadata = objectMapper.treeToValue(pageNode, PageMetadata.class);
            }

            return new PagedResult<>(publishers, metadata);
        } catch (Exception e) {
            e.printStackTrace();
            return new PagedResult<>(Collections.emptyList(), null);
        }
    }

    public PublisherDto fetchPublisherById(String id) {
        try {
            return restClient.get()
                    .uri("/api/publishers/{id}", id)
                    .retrieve()
                    .body(PublisherDto.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<PublisherTitleDto> fetchTitlesByPublisher(String pubId) {
        try {
            String jsonResponse = restClient.get()
                    .uri("/api/titles/search/publisher?pubId={id}", pubId)
                    .retrieve()
                    .body(String.class);

            if (jsonResponse == null) {
                return Collections.emptyList();
            }

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode titlesNode = rootNode.at("/_embedded/titles");
            if (titlesNode.isMissingNode() || titlesNode.isEmpty()) {
                return Collections.emptyList();
            }

            return objectMapper.readerForListOf(PublisherTitleDto.class).readValue(titlesNode);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private String buildListOrSearchUri(int page, int size, String searchBy, String keyword, String sort, String dir) {
        String sortField = (sort == null || sort.isBlank()) ? "pubName" : sort;
        String sortDir = (dir == null || dir.isBlank()) ? "asc" : dir;

        if (keyword == null || keyword.trim().isEmpty() || searchBy == null || searchBy.trim().isEmpty()) {
            return "/api/publishers?page=" + page + "&size=" + size + "&sort=" + sortField + "," + sortDir;
        }

        String cleanedKeyword = keyword.trim();
        return switch (searchBy) {
            case "city" -> "/api/publishers/search/city?city=" + cleanedKeyword + "&page=" + page + "&size=" + size + "&sort=" + sortField + "," + sortDir;
            case "pubName" -> "/api/publishers/search/pubname?pubName=" + cleanedKeyword + "&page=" + page + "&size=" + size + "&sort=" + sortField + "," + sortDir;
            case "state" -> "/api/publishers/search/state?state=" + cleanedKeyword + "&page=" + page + "&size=" + size + "&sort=" + sortField + "," + sortDir;
            case "country" -> "/api/publishers/search/country?country=" + cleanedKeyword + "&page=" + page + "&size=" + size + "&sort=" + sortField + "," + sortDir;
            default -> "/api/publishers?page=" + page + "&size=" + size + "&sort=" + sortField + "," + sortDir;
        };
    }
}
