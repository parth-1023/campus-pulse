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
		if (!incidents.isEmpty()) {
			Incident first = incidents.get(0);
			try {
				String json = objectMapper.writeValueAsString(first);
				System.out.println("Serialized incident JSON: " + json);
			} catch (Exception e) {
				System.err.println("SERIALIZATION FAILED!");
				e.printStackTrace();
				throw e;
			}
		}
	}
}
