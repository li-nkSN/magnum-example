DROP TABLE IF EXISTS car;

CREATE TABLE car (
    model VARCHAR(50) NOT NULL,
    id bigint PRIMARY KEY,
    top_speed INT NOT NULL,
    vin INT,
    created TIMESTAMP WITH TIME ZONE NOT NULL,
    related_car_ids INT8[]
);

INSERT INTO car (model, id, top_speed, vin, created, related_car_ids) VALUES
('McLaren Senna', 1, 208, 123, '2024-11-24T22:17:30.000000000Z'::timestamptz,NULL),
('Ferrari F8 Tributo', 2, 212, 124, '2024-11-24T22:17:31.000000000Z'::timestamptz,NULL),
('Aston Martin Superleggera', 3, 211, null, '2024-11-24T22:17:32.000000000Z'::timestamptz,'{101}');
