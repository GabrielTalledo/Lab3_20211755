package com.example.pomodoropucp.DTOs;

import java.io.Serializable;

public class Tarea implements Serializable {

    //Atributos:
    private int id;
    private String todo;
    private boolean completed;
    private int userId;

    //Getter y setters:
    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTodo() {
        return todo;
    }

    public void setTodo(String todo) {
        this.todo = todo;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
