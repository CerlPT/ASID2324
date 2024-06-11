package com.clubs.clubs;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClubsService {
       @Autowired
    private ClubsRepository clubsRepository;

    public Clubs saveClubs(Clubs clubs) {
        return clubsRepository.save(clubs);
    }

    public void deleteClubsById(int id) {
        clubsRepository.deleteById(id);
    }

    public List<Clubs> getAllClubss() {
        return clubsRepository.findAll();
    }

    public Clubs getClubsById(int id) {
        return clubsRepository.findById(id)
                                 .orElseThrow(() -> new RuntimeException("Town not found with id: " + id));
    }
}
