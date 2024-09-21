package com.example.pomodoropucp.DTOs;

import java.io.Serializable;
import java.util.List;

public class TareaTodo implements Serializable {

    private List<Tarea> todos;

    public List<Tarea> getTodos() {
        return todos;
    }

    public void setTodos(List<Tarea> todos) {
        this.todos = todos;
    }
}
