package com.capgemini.book_partner_frontend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.capgemini.book_partner_frontend.DTO.publisher.PagedResult;
import com.capgemini.book_partner_frontend.DTO.publisher.PublisherDto;
import com.capgemini.book_partner_frontend.DTO.publisher.PublisherTitleDto;
import com.capgemini.book_partner_frontend.service.PublisherClientService;

@Controller
@RequestMapping("/publishers")
public class PublisherFrontendController {

    private final PublisherClientService publisherClientService;

    public PublisherFrontendController(PublisherClientService publisherClientService) {
        this.publisherClientService = publisherClientService;
    }

    @GetMapping
    public String viewPublishersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String searchBy,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "pubName") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            Model model) {

        PagedResult<PublisherDto> result = publisherClientService.fetchPublishersPaginated(page, size, searchBy, keyword, sort, dir);

        model.addAttribute("publisherList", result.content());
        model.addAttribute("pageMeta", result.metadata());
        model.addAttribute("currentSize", size);
        model.addAttribute("searchBy", searchBy);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("reverseDir", dir.equals("asc") ? "desc" : "asc");
        model.addAttribute("newPublisher", new PublisherDto());

        return "publishers-page";
    }

    @GetMapping("/{id}")
    public String viewPublisherDetails(@PathVariable("id") String id, Model model) {
        PublisherDto publisher = publisherClientService.fetchPublisherById(id);
        List<PublisherTitleDto> titles = publisherClientService.fetchTitlesByPublisher(id);

        model.addAttribute("publisher", publisher);
        model.addAttribute("titleList", titles);
        return "publisher-details";
    }

    @PostMapping("/add")
    public ResponseEntity<String> addPublisher(@ModelAttribute("newPublisher") PublisherDto newPublisher, RedirectAttributes redirectAttributes) {
        try {
            publisherClientService.createPublisher(newPublisher);

            redirectAttributes.addFlashAttribute("message", "Publisher created successfully.");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return ResponseEntity.ok("Success");
        } catch (HttpClientErrorException.Conflict e) {
            System.out.println(e.getResponseBodyAsString());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getResponseBodyAsString());
        }catch(HttpClientErrorException.BadRequest e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/edit/{id}")
    public String editPublisher(
            @PathVariable("id") String id,
            @ModelAttribute("publisher") PublisherDto updatedPublisher,
            RedirectAttributes redirectAttributes) {
        try {
            updatedPublisher.setPubId(id);
            publisherClientService.updatePublisher(id, updatedPublisher);
            redirectAttributes.addFlashAttribute("message", "Publisher updated successfully.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Could not update publisher. Please verify input values.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/publishers/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deletePublisher(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            publisherClientService.deletePublisher(id);
            redirectAttributes.addFlashAttribute("message", "Publisher deactivated successfully.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Could not deactivate publisher.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/publishers";
    }
}
