package com.towns.towns;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/towns")
public class TownController {
    @Autowired
    private TownService townService;

    @PostMapping
    public Town createTown(@RequestBody Town town) {
        return townService.saveTown(town);
    }

    @DeleteMapping("/{id}")
    public void deleteTown(@PathVariable int id) {
        townService.deleteTownById(id);
    }

    @GetMapping
    public List<Town> getAllTowns() {
        return townService.getAllTowns();
    }

    @GetMapping("/{id}")
    public Town getStudentById(@PathVariable int id) {
        return townService.getTownById(id);
    }
}

