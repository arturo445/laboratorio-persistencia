-- 0. Insertar un centro de rescate inicial (ajusta las columnas si tu tabla pide algo distinto)
INSERT INTO rescue_centers (code, name, city) 
VALUES ('CEN-1', 'Centro Principal DeepBlue', 'Santa Marta');

-- 1. Insertar el caso de rescate (buscando el ID del centro que acabamos de crear)
INSERT INTO rescue_cases (case_code, status, rescue_center_id, rescue_date, version, rescue_location) 
VALUES (
    'RC-001', 
    'ADMITTED', 
    (SELECT id FROM rescue_centers LIMIT 1), 
    CURRENT_DATE, 
    0, 
    'Playa Blanca'
);

-- 2. Insertar el animal incluyendo el tracking_device_code
INSERT INTO animals (animal_code, common_name, scientific_name, sex, rescue_case_id, tracking_device_code) 
VALUES (
    'AN-001', 
    'Tortuga Carey', 
    'Eretmochelys imbricata', 
    'FEMALE', 
    (SELECT id FROM rescue_cases WHERE case_code = 'RC-001'),
    'GPS-TX-001'
);

-- 3. Insertar el registro médico inicial
INSERT INTO medical_records (animal_id, initial_weight, initial_condition, injuries, observations)
VALUES (
    (SELECT id FROM animals WHERE animal_code = 'AN-001'),
    45.50, 
    'Estable', 
    'Laceración en aleta', 
    'Requiere evaluación por el equipo de Marine Reptiles'
);

-- 4. Insertar el especialista (necesario para el frontend)
INSERT INTO specialists (professional_code, first_name, last_name, email, active) 
VALUES ('SP-001', 'Arturo', 'Abella', 'arturo@deepblue.com', true);

-- 5. Vincular al especialista usando una de las especialidades que ya insertaste en tu V2
INSERT INTO specialist_expertise (specialist_id, expertise_id) 
VALUES (
    (SELECT id FROM specialists WHERE professional_code = 'SP-001'),
    (SELECT id FROM expertise WHERE name = 'Marine Reptiles')
);

-- 6. Insertar un tratamiento de prueba inicial
INSERT INTO treatments (animal_id, specialist_id, performed_at, type, description)
VALUES (
    (SELECT id FROM animals WHERE animal_code = 'AN-001'),
    (SELECT id FROM specialists WHERE professional_code = 'SP-001'),
    CURRENT_TIMESTAMP,
    'WOUND_CARE',
    'Limpieza profunda y aplicación de antibiótico tópico'
);