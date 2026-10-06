package com.example.rest.dao;

import com.example.rest.entity.Employee;

import java.util.List;

public interface EmployeeDAO {
    List<Employee> findAll();
}
