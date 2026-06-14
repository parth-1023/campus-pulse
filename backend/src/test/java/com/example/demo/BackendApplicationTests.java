package com.example.demo;

import com.example.demo.model.Incident;
import com.example.demo.repository.IncidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class BackendApplicationTests {

	@Autowired
	private IncidentRepository incidentRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void contextLoads() {
	}

	@Test
	void serializationTest() throws Exception {
		List<Incident> incidents = incidentRepository.findAll();
		System.out.println("Fetched " + incidents.size() + " incidents.");
		for (Incident incident : incidents) {
			String json = objectMapper.writeValueAsString(incident);
			org.junit.jupiter.api.Assertions.assertNotNull(json);
			org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"type\":\"Point\""));
		}
	}
}
