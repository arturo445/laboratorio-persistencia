-- =============================================================================
-- V3__add_tracking_device_to_animal.sql
-- Agrega código de dispositivo de seguimiento GPS a animales
-- =============================================================================

ALTER TABLE animals
    ADD COLUMN tracking_device_code VARCHAR(50) UNIQUE;
    
ALTER TABLE rescue_cases ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;