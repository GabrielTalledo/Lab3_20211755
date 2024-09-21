package com.example.pomodoropucp;

import java.io.Serializable;

public class Usuario implements Serializable {

    //Atributos:
    private Integer id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String gender;

    //Constructor:

    public Usuario(String email, String firstName, String gender, String lastName, Integer userId, String username) {
        this.email = email;
        this.firstName = firstName;
        this.gender = gender;
        this.lastName = lastName;
        this.id = userId;
        this.username = username;
    }

    //Métodos útiles:
    public String getFullName(){
        return firstName + " " + lastName;
    }

    public boolean isMale(){
        return gender.equals("male");
    }

    //Getters y setters:

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer userId) {
        this.id = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
