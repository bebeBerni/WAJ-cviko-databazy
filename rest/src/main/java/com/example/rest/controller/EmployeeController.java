package com.example.rest.controller;

import com.example.rest.entity.Employee;
import com.example.rest.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    @Value("${job-titles}")
    private List<String> jobTitles;

    @Value("${full-time-options}")
    private List<String> fullTimeOptions;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @InitBinder("employee")
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("firstName", "lastName", "birthDate", "email",
                "phone", "jobTitle", "salary", "fullTime");
    }

    @ModelAttribute
    public void addOptions(Model model) {
        model.addAttribute("jobTitles", jobTitles);
        model.addAttribute("fullTimeOptions", fullTimeOptions);
    }

    @GetMapping
    public String listEmployees(Model model) {
        model.addAttribute("employees", employeeService.findAll());
        return "employees/list";
    }

    @GetMapping("/{id}")
    public String viewEmployee(@PathVariable Integer id, Model model) {
        model.addAttribute("employee", employeeService.findById(id));
        return "employees/view";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("employee", new Employee());
        return "employees/form";
    }

    @PostMapping
    public String createEmployee(@Valid @ModelAttribute("employee") Employee employee,
                                 BindingResult result) {
        validateOptions(employee, result);
        if (result.hasErrors()) {
            return "employees/form";
        }
        employeeService.create(employee);
        return "redirect:/employees";
    }

    @GetMapping("/{id}/edit")
    public String showUpdateForm(@PathVariable Integer id, Model model) {
        model.addAttribute("employee", employeeService.findById(id));
        return "employees/form";
    }

    @PostMapping("/{id}")
    public String updateEmployee(@PathVariable Integer id,
                                 @Valid @ModelAttribute("employee") Employee employee,
                                 BindingResult result) {
        employeeService.findById(id);
        employee.setId(id);
        validateOptions(employee, result);
        if (result.hasErrors()) {
            return "employees/form";
        }
        employeeService.update(id, employee);
        return "redirect:/employees";
    }

    @PostMapping("/{id}/delete")
    public String deleteEmployee(@PathVariable Integer id) {
        employeeService.deleteById(id);
        return "redirect:/employees";
    }

    private void validateOptions(Employee employee, BindingResult result) {
        if (!result.hasFieldErrors("jobTitle") && !jobTitles.contains(employee.getJobTitle())) {
            result.rejectValue("jobTitle", "invalidOption", "Vyberte pozíciu zo zoznamu.");
        }
        if (!result.hasFieldErrors("fullTime") && !fullTimeOptions.contains(employee.getFullTime())) {
            result.rejectValue("fullTime", "invalidOption", "Vyberte platný typ úväzku.");
        }
    }
}
