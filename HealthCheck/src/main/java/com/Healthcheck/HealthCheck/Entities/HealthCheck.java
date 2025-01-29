package com.Healthcheck.HealthCheck.Entities;


import jakarta.persistence.*;


import java.time.LocalDateTime;

@Entity
public class HealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long Id;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    public HealthCheck(){
        this.dateTime = LocalDateTime.now();
    }
}
