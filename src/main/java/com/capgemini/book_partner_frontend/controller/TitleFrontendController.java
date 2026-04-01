package com.capgemini.book_partner_frontend.controller;

import java.util.List;
import java.util.Collections;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.capgemini.book_partner_frontend.DTO.title.TitleDto;
import com.capgemini.book_partner_frontend.DTO.author.AuthorDto;
import com.capgemini.book_partner_frontend.DTO.title.PagedResult;
import com.capgemini.book_partner_frontend.service.TitleClientService;
import com.capgemini.book_partner_frontend.service.AuthorClientService;

@Controller
@RequestMapping("/titles")
public class TitleFrontendController {

    private final TitleClientService titleClientService;
    private final AuthorClientService authorClientService;

    public TitleFrontendController(TitleClientService titleClientService, AuthorClientService authorClientService) {
        this.titleClientService = titleClientService;
        this.authorClientService = authorClientService;
    }

    // --- Add Title Success/Error ---
    @PostMapping("/add")
    public String addTitle(@ModelAttribute("newTitle") TitleDto newTitle,
                          @RequestParam(required = false) String[] authorIds,
                          RedirectAttributes redirectAttributes) {
        try {
            titleClientService.createTitle(newTitle);
            
            // Handle author associations if provided
            if (authorIds != null && authorIds.length > 0) {
                List<String> authList = java.util.Arrays.asList(authorIds);
                titleClientService.saveTitleAuthors(newTitle.getTitleId(), authList);
            }
            
            redirectAttributes.addFlashAttribute("message", "New book title has been created successfully!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Could not create title. Please check all required fields.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/titles";
    }

    // --- Edit Title Success/Error ---
    @PostMapping("/edit/{id}")
    public String editTitle(@PathVariable("id") String id, @ModelAttribute("title") TitleDto updatedTitle,
                            @RequestParam(required = false) String[] authorIds,
                            RedirectAttributes redirectAttributes) {
        try {
            updatedTitle.setTitleId(id);
            titleClientService.updateTitle(id, updatedTitle);

            // Always sync author associations — even if the user deselected all authors.
            // Passing an empty list causes saveTitleAuthors to delete all existing ones.
            List<String> authList = (authorIds != null) ? java.util.Arrays.asList(authorIds) : Collections.emptyList();
            titleClientService.saveTitleAuthors(id, authList);

            redirectAttributes.addFlashAttribute("message", "Title information has been updated successfully!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Update failed. Please verify all fields.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/titles/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deleteTitle(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            titleClientService.deleteTitle(id);
            redirectAttributes.addFlashAttribute("message", "Book title has been successfully deleted.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Something went wrong. Could not delete title.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/titles";
    }

    @GetMapping("/{id}")
    public String viewTitleDetails(@PathVariable("id") String id, Model model) {
        TitleDto title = titleClientService.fetchTitleById(id);
        List<AuthorDto> authors = titleClientService.fetchAuthorsByTitle(id); 
        
        model.addAttribute("title", title);
        model.addAttribute("authorList", authors);
        return "Titles/title-details"; 
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        try {
            TitleDto title = titleClientService.fetchTitleById(id);
            List<AuthorDto> availableAuthors = authorClientService.fetchAuthors();
            List<AuthorDto> currentAuthors = titleClientService.fetchAuthorsByTitle(id);

            model.addAttribute("title", title);
            model.addAttribute("availableAuthors", availableAuthors);
            model.addAttribute("currentAuthors", currentAuthors);
            model.addAttribute("publisherList", titleClientService.fetchAllPublishers());
            model.addAttribute("isEdit", true);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading title for editing: " + e.getMessage());
        }
        return "Titles/title-form";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        try {
            List<AuthorDto> availableAuthors = authorClientService.fetchAuthors();

            model.addAttribute("title", new TitleDto());
            model.addAttribute("availableAuthors", availableAuthors);
            model.addAttribute("currentAuthors", Collections.emptyList());
            model.addAttribute("publisherList", titleClientService.fetchAllPublishers());
            model.addAttribute("isEdit", false);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
        }
        return "Titles/title-form";
    }

    // --- Master Title Dashboard ---
    @GetMapping
    public String viewTitlesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String searchBy,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir,
            Model model) {

        PagedResult<TitleDto> result = titleClientService.fetchTitlesPaginated(page, size, searchBy, keyword, sortBy, sortDir);

        model.addAttribute("titleList", result.content());
        model.addAttribute("pageMeta", result.metadata());

        model.addAttribute("currentSize", size);
        model.addAttribute("searchBy", searchBy);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentSortBy", sortBy);
        model.addAttribute("currentSortDir", sortDir);

        // Empty DTO for the Add Modal
        model.addAttribute("newTitle", new TitleDto());

        // List of available authors for selection
        try {
            List<AuthorDto> availableAuthors = authorClientService.fetchAuthors();
            model.addAttribute("availableAuthors", availableAuthors);
        } catch (Exception e) {
            model.addAttribute("availableAuthors", List.of());
        }

        return "Titles/titles-page";
    }
}
