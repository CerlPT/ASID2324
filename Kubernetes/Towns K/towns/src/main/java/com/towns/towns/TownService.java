package com.towns.towns;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TownService {
    @Autowired
    private TownRepository townRepository;

    public Town saveTown(Town town) {
        return townRepository.save(town);
    }

    public void deleteTownById(int id) {
        townRepository.deleteById(id);
    }

    public List<Town> getAllTowns() {
        return townRepository.findAll();
    }

    public Town getTownById(int id) {
        return townRepository.findById(id)
                                 .orElseThrow(() -> new RuntimeException("Town not found with id: " + id));
    }
}
