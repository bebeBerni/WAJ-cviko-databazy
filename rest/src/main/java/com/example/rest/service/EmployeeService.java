package com.example.rest.service;

import com.example.rest.entity.Employee;

import java.util.List;

public interface EmployeeService {
    List<Employee> findAll();
    Employee findById(Integer id);
    Employee create(Employee employee);
    Employee update(Integer id, Employee employee);
    void deleteById(Integer id);
}
