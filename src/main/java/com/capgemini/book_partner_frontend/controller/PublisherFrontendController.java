package com.capgemini.book_partner_frontend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.capgemini.book_partner_frontend.DTO.publisher.PagedResult;
import com.capgemini.book_partner_frontend.DTO.publisher.PublisherDto;
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
            Model model) {

        PagedResult<PublisherDto> result = publisherClientService.fetchPublishersPaginated(page, size, searchBy, keyword);

        model.addAttribute("publisherList", result.content());
        model.addAttribute("pageMeta", result.metadata());
        model.addAttribute("currentSize", size);
        model.addAttribute("searchBy", searchBy);
        model.addAttribute("keyword", keyword);
        model.addAttribute("newPublisher", new PublisherDto());

        return "publishers-page";
    }

    @PostMapping("/add")
    public String addPublisher(@ModelAttribute("newPublisher") PublisherDto newPublisher, RedirectAttributes redirectAttributes) {
        try {
            publisherClientService.createPublisher(newPublisher);
            redirectAttributes.addFlashAttribute("message", "Publisher created successfully.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Could not create publisher. Please verify input values.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/publishers";
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
        return "redirect:/publishers";
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
