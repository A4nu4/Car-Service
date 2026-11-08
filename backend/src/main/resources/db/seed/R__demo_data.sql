-- Demo branches and fleet for local development (only loaded by the "dev" profile).
-- Mirrors the cars in the frontend's src/data/cars.mock.ts. No user accounts are seeded:
-- the first admin is created from BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD.

INSERT INTO locations (id, name, address_line, city, postal_code, country, phone, opening_hours)
VALUES
    ('11111111-0000-0000-0000-000000000001', 'Berlin Central Station', 'Europaplatz 1', 'Berlin', '10557', 'DE',
     '+49 30 1234567', 'Mon-Sun 07:00-22:00'),
    ('11111111-0000-0000-0000-000000000002', 'Munich Airport', 'Terminalstrasse Mitte 18', 'Munich', '85356', 'DE',
     '+49 89 7654321', 'Mon-Sun 06:00-23:00'),
    ('11111111-0000-0000-0000-000000000003', 'Hamburg City', 'Moenckebergstrasse 7', 'Hamburg', '20095', 'DE',
     '+49 40 5550101', 'Mon-Sat 08:00-20:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cars (id, brand, model, model_year, category, transmission, fuel_type, seats, price_per_day,
                  vin, registration_number, mileage, location_id)
VALUES
    ('22222222-0000-0000-0000-000000000001', 'Toyota', 'Yaris', 2024, 'ECONOMY', 'AUTOMATIC', 'HYBRID', 5, 34.00,
     'VNKKL3D30PA000001', 'B-CR 1001', 12500, '11111111-0000-0000-0000-000000000001'),
    ('22222222-0000-0000-0000-000000000002', 'Skoda', 'Octavia', 2023, 'ECONOMY', 'MANUAL', 'DIESEL', 5, 46.00,
     'TMBJJ7NE0P0000002', 'B-CR 1002', 34100, '11111111-0000-0000-0000-000000000001'),
    ('22222222-0000-0000-0000-000000000003', 'VW', 'Tiguan', 2024, 'SUV', 'AUTOMATIC', 'PETROL', 5, 59.00,
     'WVGZZZ5NZRW000003', 'M-CR 2001', 8900, '11111111-0000-0000-0000-000000000002'),
    ('22222222-0000-0000-0000-000000000004', 'Tesla', 'Model 3', 2024, 'ELECTRIC', 'AUTOMATIC', 'ELECTRIC', 5, 79.00,
     '5YJ3E7EA1PF000004', 'M-CR 2002', 15200, '11111111-0000-0000-0000-000000000002'),
    ('22222222-0000-0000-0000-000000000005', 'Nissan', 'Leaf', 2023, 'ELECTRIC', 'AUTOMATIC', 'ELECTRIC', 5, 52.00,
     'SJNFAAZE1U0000005', 'HH-CR 3001', 21000, '11111111-0000-0000-0000-000000000003'),
    ('22222222-0000-0000-0000-000000000006', 'Mercedes', 'Vito', 2022, 'VAN', 'MANUAL', 'DIESEL', 9, 89.00,
     'W1V44760313000006', 'HH-CR 3002', 56700, '11111111-0000-0000-0000-000000000003')
ON CONFLICT (id) DO NOTHING;
