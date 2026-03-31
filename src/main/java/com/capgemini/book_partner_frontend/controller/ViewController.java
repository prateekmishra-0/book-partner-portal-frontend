package com.capgemini.book_partner_frontend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/employees")
    public String showEmployeesPage() {
        // Tells Spring to look in templates/employee/ for employees.html
        return "employee/employees";
    }

    // NEW: Serve the details page
    @GetMapping("/employee-details")
    public String showEmployeeDetailsPage() {
        return "employee/employee-details";
    }


}