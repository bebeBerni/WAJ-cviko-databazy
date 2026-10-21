package com.example.rest.controller;

import com.example.rest.response.ApiResponse;
import com.example.rest.entity.Employee;
import com.example.rest.service.EmployeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.net.URI;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class EmployeeRestController {

    private final EmployeeService employeeService;

    public EmployeeRestController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employees")
    public ApiResponse<List<Employee>> getAllEmployees() {
        List<Employee> employees = employeeService.findAll();
        return new ApiResponse<>("Zoznam zamestnancov bol načítaný. Počet: " + employees.size() + ".",
                employees);
    }

    @GetMapping("/employees/{id}")
    public ApiResponse<Employee> getEmployee(@PathVariable("id") @Positive(message = "ID musí byť kladné.") Integer id) {
        Employee employee = employeeService.findById(id);
        return new ApiResponse<>("Zamestnanec s ID " + id + " bol načítaný.", employee);
    }

    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<Employee>> createEmployee(@Valid @RequestBody Employee employee) {
        Employee savedEmployee = employeeService.create(employee);
        return ResponseEntity.created(URI.create("/api/employees/" + savedEmployee.getId()))
                .body(new ApiResponse<>("Zamestnanec s ID " + savedEmployee.getId()
                        + " bol úspešne vytvorený.", savedEmployee));
    }

    @PutMapping("/employees/{id}")
    public ApiResponse<Employee> updateEmployee(@PathVariable("id") @Positive(message = "ID musí byť kladné.") Integer id,
                                   @Valid @RequestBody Employee employee) {
        Employee updatedEmployee = employeeService.update(id, employee);
        return new ApiResponse<>("Zamestnanec s ID " + id + " bol úspešne aktualizovaný.",
                updatedEmployee);
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<Integer>> deleteEmployee(@PathVariable("id") @Positive(message = "ID musí byť kladné.") Integer id) {
        employeeService.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(
                "Zamestnanec s ID " + id + " bol úspešne zmazaný.", id));
    }
}
