package com.example.demo.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import tools.jackson.databind.annotation.JsonSerialize;
import com.example.demo.config.PointSerializer;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    @Column(name = "co2_level")
    private Double co2Level;

    // We specify the columnDefinition as geometry(Point, 4326) to map to PostGIS
    // Point type
    @Column(columnDefinition = "geometry(Point, 4326)")
    @JsonSerialize(using = PointSerializer.class)
    private Point location;

    // Constructors
    public Incident() {
    }

    public Incident(String description, Double co2Level, Point location) {
        this.description = description;
        this.co2Level = co2Level;
        this.location = location;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getCo2Level() {
        return co2Level;
    }

    public void setCo2Level(Double co2Level) {
        this.co2Level = co2Level;
    }

    @JsonSerialize(using = PointSerializer.class)
    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }
}
