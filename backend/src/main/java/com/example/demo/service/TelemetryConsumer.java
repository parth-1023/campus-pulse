package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.model.Incident;
import com.example.demo.dto.IncidentAlert;
import com.example.demo.repository.IncidentRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class TelemetryConsumer {

    private final IncidentRepository incidentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper(); // Instantiate directly
    private final SimpMessagingTemplate messagingTemplate;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    public TelemetryConsumer(IncidentRepository incidentRepository, SimpMessagingTemplate messagingTemplate) {
        this.incidentRepository = incidentRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = "campus.telemetry.queue")
    public void consumeTelemetry(String message) {
        System.out.println("[x] Received Telemetry Message from Queue: " + message);
        try {
            // 1. Parse JSON payload
            JsonNode node = objectMapper.readTree(message);
            double co2 = node.get("co2").asDouble();
            int motion = node.get("motion").asInt();
            double latitude = node.get("latitude").asDouble();
            double longitude = node.get("longitude").asDouble();
            String timestamp = node.get("timestamp").asText();

            // 2. Day 6 Anomaly rule: CO2 > 1500 ppm AND active motion (motion == 1)
            if (co2 > 1500.0 && motion == 1) {
                System.out.println("[!] ANOMALY DETECTED: CO2 = " + co2 + " ppm, Motion = " + motion);

                // 3. Create JTS Point (note: coordinate ordering in JTS is x=longitude,
                // y=latitude)
                Coordinate coordinate = new Coordinate(longitude, latitude);
                Point location = geometryFactory.createPoint(coordinate);

                String description = String.format("Anomaly at %s. CO2: %.1f ppm, Motion detected.", timestamp, co2);

                // 4. Save entity to Supabase
                Incident incident = new Incident(description, co2, location);
                Incident saved = incidentRepository.save(incident);

                System.out.println("[+] Anomaly saved to database. Incident ID: " + saved.getId());

                // 5. Build DTO and Broadcast over WebSocket
                IncidentAlert alert = new IncidentAlert(
                        saved.getId(),
                        saved.getDescription(),
                        saved.getCo2Level(),
                        latitude,
                        longitude);

                // Push alert directly to `/topic/alerts` queue
                messagingTemplate.convertAndSend("/topic/alerts", alert);
                System.out.println("[->] Broadcasted incident over WebSocket: ID " + saved.getId());
            }
        } catch (Exception e) {
            System.err.println("Error parsing telemetry message: " + e.getMessage());
        }
    }
}
