package com.example.demo.dto;

public class IncidentAlert {
    private Long id;
    private String description;
    private Double co2Level;
    private Double latitude;
    private Double longitude;

    // Constructors
    public IncidentAlert() {
    }

    public IncidentAlert(Long id, String description, Double co2Level, Double latitude, Double longitude) {
        this.id = id;
        this.description = description;
        this.co2Level = co2Level;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
