package com.skybooker.airline.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "airlines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airline {

    @Id
    @Column(name = "airline_id", length = 36, nullable = false, updatable = false)
    private String airlineId;

    @Column(nullable = false)
    private String name;

    @Column(name = "iata_code", nullable = false, unique = true, length = 3)
    private String iataCode;

    @Column(name = "icao_code", unique = true, length = 4)
    private String icaoCode;

    private String logoUrl;

    @Column(nullable = false)
    private String country;

    private String contactEmail;
    private String contactPhone;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.airlineId == null) this.airlineId = UUID.randomUUID().toString();
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.isActive == null) this.isActive = true;
        if (this.iataCode != null) this.iataCode = this.iataCode.toUpperCase();
        if (this.icaoCode != null) this.icaoCode = this.icaoCode.toUpperCase();
    }
}