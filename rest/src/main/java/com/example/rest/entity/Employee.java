package com.example.rest.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.Date;

@Entity
@Table(name = "employee")
public class Employee {
    // Common ASCII dot-atom mailbox (RFC 5321/5322), including punycode domains.
    // Quoted local parts, address literals and SMTPUTF8 mailboxes are outside this API's scope.
    public static final String EMAIL_PATTERN =
            "\\A(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+"
            + "(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
            + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+"
            + "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\z";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "first_name")
    @NotBlank(message = "Meno je povinné.")
    @Size(max = 100, message = "Meno môže mať najviac 100 znakov.")
    private String firstName;

    @Column(name = "last_name")
    @NotBlank(message = "Priezvisko je povinné.")
    @Size(max = 100, message = "Priezvisko môže mať najviac 100 znakov.")
    private String lastName;

    @Column(name = "birth_date")
    @NotNull(message = "Dátum narodenia je povinný.")
    @Past(message = "Dátum narodenia musí byť v minulosti.")
    private Date birthDate;

    @Column(name = "email")
    @NotBlank(message = "Email je povinný.")
    @Email(message = "Email musí byť platná emailová adresa.")
    @Size(max = 254, message = "Email môže mať najviac 254 znakov.")
    @Pattern(regexp = EMAIL_PATTERN, message = "Email musí mať bežný formát meno@domena.sk.")
    private String email;

    @Column(name = "phone")
    @NotBlank(message = "Telefón je povinný.")
    @Pattern(regexp = "\\A(?=.{7,25}\\z)\\+?[0-9](?:[ ()-]*[0-9]){6,14}\\z",
            message = "Telefón musí mať 7 až 15 číslic a najviac 25 znakov; povolené sú medzery, zátvorky, pomlčky a úvodné +.")
    private String phone;

    @Column(name = "job_title")
    @NotBlank(message = "Pracovná pozícia je povinná.")
    @Size(max = 150, message = "Pracovná pozícia môže mať najviac 150 znakov.")
    private String jobTitle;

    @Column(name = "salary")
    @NotNull(message = "Plat je povinný.")
    @PositiveOrZero(message = "Plat nesmie byť záporný.")
    private Integer salary;

    @Column(name = "full_time")
    @NotNull(message = "Údaj o plnom úväzku je povinný.")
    private Boolean fullTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public Integer getSalary() {
        return salary;
    }

    public void setSalary(Integer salary) {
        this.salary = salary;
    }

    public Boolean isFullTime() {
        return fullTime;
    }

    public void setFullTime(Boolean fullTime) {
        this.fullTime = fullTime;
    }
}
