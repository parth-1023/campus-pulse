package com.example.demo.controller;

import com.example.demo.model.Incident;
import com.example.demo.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@CrossOrigin(origins = "http://localhost:5173") // Authorize CORS requests from Vite React frontend
public class IncidentController {

    private final IncidentRepository incidentRepository;

    @Autowired
    public IncidentController(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    // Secure endpoint returning all incidents from Supabase
    @GetMapping
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }
}
