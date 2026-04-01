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

import com.capgemini.book_partner_frontend.DTO.author.AuthorDto;
import com.capgemini.book_partner_frontend.DTO.author.AuthorTitleDto;
import com.capgemini.book_partner_frontend.DTO.author.PagedResult;
import com.capgemini.book_partner_frontend.service.AuthorClientService;

@Controller
@RequestMapping("/authors")
public class AuthorFrontendController {

    private final AuthorClientService authorClientService;

    public AuthorFrontendController(AuthorClientService authorClientService) {
        this.authorClientService = authorClientService;
    }

    // --- Add Author Success/Error ---
    @PostMapping("/add")
    public ResponseEntity<String> addAuthor(@ModelAttribute("newAuthor") AuthorDto newAuthor, RedirectAttributes redirectAttributes) {
        try {
            authorClientService.createAuthor(newAuthor);
            
            // We still keep this for standard non-JS fallbacks
            redirectAttributes.addFlashAttribute("message", "New author registered successfully!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            
            return ResponseEntity.ok("Success");
        } catch (HttpClientErrorException.Conflict e) {

            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // --- Edit Author Success/Error ---
    @PostMapping("/edit/{id}")
    public String editAuthor(@PathVariable("id") String id, @ModelAttribute("author") AuthorDto updatedAuthor, RedirectAttributes redirectAttributes) {
        try {
            updatedAuthor.setAuId(id);
            authorClientService.updateAuthor(id, updatedAuthor);
            redirectAttributes.addFlashAttribute("message", "Profile updated successfully!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Update failed. Please verify all fields.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/authors/" + id;
    }

@PostMapping("/delete/{id}")
public String deleteAuthor(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
    try {
        authorClientService.deleteAuthor(id);
        // Pass success message
        redirectAttributes.addFlashAttribute("message", "Author profile has been successfully deactivated.");
        redirectAttributes.addFlashAttribute("messageType", "success");
    } catch (Exception e) {
        // Pass error message
        redirectAttributes.addFlashAttribute("message", "Something went wrong. Could not delete author.");
        redirectAttributes.addFlashAttribute("messageType", "error");
    }
    return "redirect:/authors";
}

    @GetMapping("/{id}")
    public String viewAuthorDetails(@PathVariable("id") String id, Model model) {
        AuthorDto author = authorClientService.fetchAuthorById(id);
        List<AuthorTitleDto> titles = authorClientService.fetchTitlesByAuthor(id); 
        
        model.addAttribute("author", author);
        model.addAttribute("titleList", titles);
        return "authors/author-details"; 
    }



@GetMapping
public String viewAuthorsPage(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "5") int size,
        @RequestParam(required = false) String searchBy,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "firstName") String sort,      
        @RequestParam(defaultValue = "asc") String dir,            
        Model model) {

    // Pass sort and dir to your service
    PagedResult<AuthorDto> result = authorClientService.fetchAuthorsPaginated(page, size, searchBy, keyword, sort, dir);

    model.addAttribute("authorList", result.content());
    model.addAttribute("pageMeta", result.metadata());
    model.addAttribute("currentSize", size);
    model.addAttribute("searchBy", searchBy);
    model.addAttribute("keyword", keyword);
    model.addAttribute("sort", sort);
    model.addAttribute("dir", dir);
    model.addAttribute("reverseDir", dir.equals("asc") ? "desc" : "asc"); // For toggling
    model.addAttribute("newAuthor", new AuthorDto());
    
    return "authors/authors-page"; 
}

    
}