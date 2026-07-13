-- Amenity dictionary: fixed reference data hosts pick from when creating listings.
-- Distinct from the M6 business seed (users/listings/bookings), which is reset nightly.
INSERT INTO amenity (name) VALUES
    ('Wifi'),
    ('Kitchen'),
    ('Free parking'),
    ('Air conditioning'),
    ('Heating'),
    ('Washer'),
    ('Dryer'),
    ('TV'),
    ('Pool'),
    ('Hot tub'),
    ('Gym'),
    ('Elevator'),
    ('Workspace'),
    ('Pet friendly'),
    ('Smoke alarm'),
    ('Breakfast');
