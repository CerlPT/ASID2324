package com.parents.parents;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parents")
public class ParentController {
    @Autowired
    private ParentService parentService;

    @PostMapping
    public Parent createParent(@RequestBody Parent parent) {
        return parentService.saveParent(parent);
    }

    @DeleteMapping("/{id}")
    public void deleteParent(@PathVariable int id) {
        parentService.deleteParentById(id);
    }

    @GetMapping
    public List<Parent> getAllStudents() {
        return parentService.getAllParents();
    }

    @GetMapping("/{id}")
    public Parent getParentById(@PathVariable int id) {
        return parentService.getParentById(id);
    }
}

