package com.skybooker.flight.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlightSchemaCleanup implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        backfillDurationMinutes();
        dropAircraftTypeColumn();
    }

    private void backfillDurationMinutes() {
        try {
            int updated = jdbcTemplate.update("""
                    UPDATE flights
                    SET duration_minutes = TIMESTAMPDIFF(MINUTE, departure_time, arrival_time)
                    WHERE duration_minutes IS NULL
                      AND departure_time IS NOT NULL
                      AND arrival_time IS NOT NULL
                      AND arrival_time > departure_time
                    """);
            if (updated > 0) {
                log.info("Backfilled duration_minutes for {} flight row(s)", updated);
            }
        } catch (Exception e) {
            log.warn("Could not backfill flights.duration_minutes: {}", e.getMessage());
        }
    }

    private void dropAircraftTypeColumn() {
        try {
            Integer columnCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'flights'
                      AND COLUMN_NAME = 'aircraft_type'
                    """, Integer.class);

            if (columnCount != null && columnCount > 0) {
                jdbcTemplate.execute("ALTER TABLE flights DROP COLUMN aircraft_type");
                log.info("Dropped unused flights.aircraft_type column");
            }
        } catch (Exception e) {
            log.warn("Could not check or drop unused flights.aircraft_type column: {}", e.getMessage());
        }
    }
}
