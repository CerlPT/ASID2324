package com.clubs.clubs;

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
@RequestMapping("/clubs")
public class ClubsController {
    
    @Autowired
    private ClubsService clubsService;

    @PostMapping
    public Clubs createClubs(@RequestBody Clubs clubs) {
        return clubsService.saveClubs(clubs);
    }

    @DeleteMapping("/{id}")
    public void deleteClubs(@PathVariable int id) {
        clubsService.deleteClubsById(id);
    }

    @GetMapping
    public List<Clubs> getAllStudents() {
        return clubsService.getAllClubss();
    }

    @GetMapping("/{id}")
    public Clubs getClubsById(@PathVariable int id) {
        return clubsService.getClubsById(id);
    }
}
