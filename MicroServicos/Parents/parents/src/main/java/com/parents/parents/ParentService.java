package com.parents.parents;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParentService {
    @Autowired
    private ParentRepository parentRepository;

    public Parent saveParent(Parent parent) {
        return parentRepository.save(parent);
    }

    public void deleteParentById(int id) {
        parentRepository.deleteById(id);
    }

    public List<Parent> getAllParents() {
        return parentRepository.findAll();
    }

    public Parent getParentById(int id) {
        return parentRepository.findById(id)
                                 .orElseThrow(() -> new RuntimeException("Town not found with id: " + id));
    }
}

