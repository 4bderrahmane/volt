-- =============================================================================
-- Demonstration dataset (technical specification §12.5: at least 40 products across
-- 5 categories, loaded automatically at startup).
--
-- A REPEATABLE migration (R__), not a versioned one and not a
-- CommandLineRunner:
--
--   * A CommandLineRunner re-inserts on every restart unless you make it
--     idempotent by hand, and people forget. Flyway records this file once.
--   * A versioned migration here would force `out-of-order` handling later,
--     because real migrations (V2, V3...) will be authored after this file
--     has already been applied. Repeatable migrations always run last.
--
-- ON CONFLICT DO NOTHING everywhere, so re-running it is free.
--
-- This file lives in db/demo, not db/migration. Include it per profile:
--   spring.flyway.locations=classpath:db/migration,classpath:db/demo
-- so an empty production schema stays possible (ADR-0006).
-- =============================================================================

-- ------------------------------------------------------------------ categories
INSERT INTO category (code, label) VALUES
    ('CAB', 'Cables and Conductors'),
    ('MOD', 'Modular Protection and Control'),
    ('LGT', 'Lighting'),
    ('DIS', 'Distribution Boards and Enclosures'),
    ('CND', 'Conduits and Fasteners')
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------- brands
INSERT INTO brand (name) VALUES
    ('Schneider Electric'),
    ('Legrand'),
    ('Hager'),
    ('Nexans'),
    ('Philips'),
    ('ABB')
ON CONFLICT (name) DO NOTHING;

-- -------------------------------------------------------------------- products
-- Category and brand are resolved by natural key rather than by hard-coded id,
-- so this file does not depend on identity sequence values.
INSERT INTO product (reference, label, description, price_excl_vat, unit,
                     stock_quantity, active, category_id, brand_id)
VALUES
    ('CAB-RO2V-3G15', 'U-1000 R2V 3G1.5 mm2 Cable', 'Rigid insulated copper cable with PVC sheath, rated 1000 V for buried or exposed installation.', 1.85, 'METER', 4200, TRUE,
     (SELECT id FROM category WHERE code = 'CAB'),
     (SELECT id FROM brand WHERE name = 'Nexans')),
    ('CAB-RO2V-3G25', 'U-1000 R2V 3G2.5 mm2 Cable', 'Rigid insulated copper cable for 20 A socket circuits.', 2.74, 'METER', 3600, TRUE,
     (SELECT id FROM category WHERE code = 'CAB'),
     (SELECT id FROM brand WHERE name = 'Nexans')),
    ('CAB-RO2V-5G6', 'U-1000 R2V 5G6 mm2 Cable', 'Three-phase supply cable for sub-distribution boards.', 9.40, 'METER', 900, TRUE,
     (SELECT id FROM category WHERE code = 'CAB'),
     (SELECT id FROM brand WHERE name = 'Nexans')),
    ('CAB-HO7VU-15', 'Blue H07V-U 1.5 mm2 Wire', 'Rigid solid-core conductor for installation inside conduit.', 0.42, 'METER', 8000, TRUE,
     (SELECT id FROM category WHERE code = 'CAB'),
     (SELECT id FROM brand WHERE name = 'Nexans')),
    ('CAB-HO7VU-25', 'Red H07V-U 2.5 mm2 Wire', 'Rigid solid-core conductor for socket circuits.', 0.68, 'METER', 7400, TRUE,
     (SELECT id FROM category WHERE code = 'CAB'),
     (SELECT id FROM brand WHERE name = 'Nexans')),
    ('CAB-HO7VK-16', 'Green/Yellow H07V-K 16 mm2 Wire', 'Flexible conductor for the main equipotential bonding connection.', 4.10, 'METER', 1100, TRUE,
     (SELECT id FROM category WHERE code = 'CAB'),
     (SELECT id FROM brand WHERE name = 'Nexans')),
    ('CAB-RJ45-C6A', 'F/UTP Cat6a Network Cable', 'Four-pair 500 MHz cable with LSZH jacket, supplied on a 100 m reel.', 1.32, 'METER', 2500, TRUE,
     (SELECT id FROM category WHERE code = 'CAB'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('CAB-COAX-17VATC', '17 VATC Coaxial Cable', '75-ohm cable for television and satellite distribution.', 1.05, 'METER', 1800, TRUE,
     (SELECT id FROM category WHERE code = 'CAB'),
     (SELECT id FROM brand WHERE name = 'Nexans')),
    ('CAB-ALARM-9C', 'Nine-Core Alarm Cable', 'Shielded 0.22 mm2 low-voltage cable.', 0.95, 'METER', 1500, TRUE,
     (SELECT id FROM category WHERE code = 'CAB'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('MOD-MCB-C10', 'Acti9 iC60N 1P+N 10 A C-Curve Circuit Breaker', 'Lighting-circuit protection with a 6 kA breaking capacity.', 24.90, 'ITEM', 320, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'Schneider Electric')),
    ('MOD-MCB-C16', 'Acti9 iC60N 1P+N 16 A C-Curve Circuit Breaker', 'Protection for 16 A socket circuits.', 25.60, 'ITEM', 410, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'Schneider Electric')),
    ('MOD-MCB-C20', 'Acti9 iC60N 1P+N 20 A C-Curve Circuit Breaker', 'Protection for 20 A socket and washing-machine circuits.', 26.80, 'ITEM', 380, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'Schneider Electric')),
    ('MOD-MCB-C32', 'Acti9 iC60N 1P+N 32 A C-Curve Circuit Breaker', 'Protection for electric cooktop circuits.', 31.40, 'ITEM', 220, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'Schneider Electric')),
    ('MOD-MCB-4P25D', 'Four-Pole 25 A D-Curve Circuit Breaker', 'Motor protection for loads with high starting current.', 96.50, 'ITEM', 60, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'ABB')),
    ('MOD-RCD-40A30', '40 A 30 mA Type AC Residual-Current Device', 'Two-module personal-protection device.', 58.20, 'ITEM', 190, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'Hager')),
    ('MOD-RCD-63A30A', '63 A 30 mA Type A Residual-Current Device', 'Protection for dedicated cooktop and washing-machine circuits.', 84.70, 'ITEM', 120, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'Hager')),
    ('MOD-RCD-40A300', '40 A 300 mA Type AC Residual-Current Device', 'Incoming-board fire protection.', 62.30, 'ITEM', 85, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'Hager')),
    ('MOD-CONTACTOR-DN', '20 A Day/Night Contactor 2NO', 'Off-peak control for electric water heaters.', 44.90, 'ITEM', 140, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('MOD-IMPULSE-16A', 'Silent Single-Pole 16 A Impulse Relay', 'Multi-point lighting control.', 36.10, 'ITEM', 160, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('MOD-SPD-T2', 'Type 2 20 kA 1P+N Surge Protector', 'Protection against transient overvoltage.', 128.00, 'ITEM', 45, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'Schneider Electric')),
    ('MOD-LOAD-2CH', 'Two-Channel Single-Phase Load Shedder', 'Power management that prevents the main circuit breaker from tripping.', 112.50, 'ITEM', 30, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'Hager')),
    ('MOD-TIMER-WEEK', 'Single-Channel Weekly Programmable Timer', 'Programmable control for outdoor lighting.', 67.40, 'ITEM', 70, TRUE,
     (SELECT id FROM category WHERE code = 'MOD'),
     (SELECT id FROM brand WHERE name = 'ABB')),
    ('LGT-PANEL-600', '600x600 LED Panel 36 W 4000 K', 'Recessed ceiling panel, UGR<19, 3600 lm, suitable for offices.', 38.90, 'ITEM', 260, TRUE,
     (SELECT id FROM category WHERE code = 'LGT'),
     (SELECT id FROM brand WHERE name = 'Philips')),
    ('LGT-PANEL-1200', '1200x300 LED Panel 40 W 4000 K', 'Recessed ceiling panel producing 4000 lm.', 46.20, 'ITEM', 140, TRUE,
     (SELECT id FROM category WHERE code = 'LGT'),
     (SELECT id FROM brand WHERE name = 'Philips')),
    ('LGT-BATTEN-1500', 'IP65 LED Batten 1500 mm 50 W', 'Weatherproof 5500 lm fitting for parking and plant rooms.', 42.70, 'ITEM', 200, TRUE,
     (SELECT id FROM category WHERE code = 'LGT'),
     (SELECT id FROM brand WHERE name = 'Philips')),
    ('LGT-BATTEN-1200', 'IP65 LED Batten 1200 mm 36 W', 'Weatherproof 4000 lm fitting for workshops.', 34.50, 'ITEM', 240, TRUE,
     (SELECT id FROM category WHERE code = 'LGT'),
     (SELECT id FROM brand WHERE name = 'Philips')),
    ('LGT-SPOT-GU10', 'White Adjustable GU10 Downlight', 'Lamp not included; requires a 75 mm ceiling cutout.', 7.80, 'ITEM', 620, TRUE,
     (SELECT id FROM category WHERE code = 'LGT'),
     (SELECT id FROM brand WHERE name = 'Philips')),
    ('LGT-BULB-GU10-5W', 'GU10 LED Bulb 5 W 3000 K', '480 lm with a 36-degree beam angle, box of 10.', 24.00, 'BOX', 180, TRUE,
     (SELECT id FROM category WHERE code = 'LGT'),
     (SELECT id FROM brand WHERE name = 'Philips')),
    ('LGT-BULB-E27-9W', 'E27 LED Bulb 9 W 2700 K', '806 lm, equivalent to 60 W, box of 10.', 29.50, 'BOX', 210, TRUE,
     (SELECT id FROM category WHERE code = 'LGT'),
     (SELECT id FROM brand WHERE name = 'Philips')),
    ('LGT-FLOOD-50W', 'Outdoor LED Floodlight 50 W IP66', '5000 lm at 4000 K with an adjustable bracket.', 31.60, 'ITEM', 150, TRUE,
     (SELECT id FROM category WHERE code = 'LGT'),
     (SELECT id FROM brand WHERE name = 'Philips')),
    ('LGT-FLOOD-150W', 'Outdoor LED Floodlight 150 W IP66', '15000 lm worksite floodlight.', 78.90, 'ITEM', 55, TRUE,
     (SELECT id FROM category WHERE code = 'LGT'),
     (SELECT id FROM brand WHERE name = 'Philips')),
    ('LGT-EMG-SATI', 'SATI Self-Contained Emergency Light 45 lm', 'Self-testing evacuation light compliant with NF C 71-820.', 52.40, 'ITEM', 130, TRUE,
     (SELECT id FROM category WHERE code = 'LGT'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('LGT-SENSOR-360', '360-Degree Ceiling Presence Sensor', 'Eight-meter range with adjustable timeout.', 68.30, 'ITEM', 75, TRUE,
     (SELECT id FROM category WHERE code = 'LGT'),
     (SELECT id FROM brand WHERE name = 'Hager')),
    ('DIS-BOX-13M', 'Single-Row 13-Module Enclosure with Opaque Door', 'Recessed IP30 enclosure with DIN rail.', 34.20, 'ITEM', 110, TRUE,
     (SELECT id FROM category WHERE code = 'DIS'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('DIS-BOX-39M', 'Three-Row 39-Module Enclosure with Clear Door', 'Recessed IP40 enclosure with earth terminal block.', 89.60, 'ITEM', 65, TRUE,
     (SELECT id FROM category WHERE code = 'DIS'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('DIS-BOX-52M', 'Four-Row 52-Module Enclosure', 'Recessed main distribution board for large residential installations.', 118.40, 'ITEM', 40, TRUE,
     (SELECT id FROM category WHERE code = 'DIS'),
     (SELECT id FROM brand WHERE name = 'Hager')),
    ('DIS-CABINET-IP65', 'IP65 Metal Electrical Cabinet 600x400x200', 'Key-locking cabinet for plant rooms.', 156.00, 'ITEM', 25, TRUE,
     (SELECT id FROM category WHERE code = 'DIS'),
     (SELECT id FROM brand WHERE name = 'ABB')),
    ('DIS-BUSBAR-13', 'Horizontal 13-Module Comb Busbar', '63 A phase-and-neutral busbar.', 14.90, 'ITEM', 240, TRUE,
     (SELECT id FROM category WHERE code = 'DIS'),
     (SELECT id FROM brand WHERE name = 'Schneider Electric')),
    ('DIS-EARTH-2X11', '2x11-Way Earth Terminal Block', 'Distribution block for protective conductors.', 9.70, 'ITEM', 300, TRUE,
     (SELECT id FROM category WHERE code = 'DIS'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('DIS-BLOCK-4P', 'Four-Pole 125 A Modular Distribution Block', 'Four sets of 13 terminals for DIN-rail mounting.', 72.80, 'ITEM', 50, TRUE,
     (SELECT id FROM category WHERE code = 'DIS'),
     (SELECT id FROM brand WHERE name = 'Hager')),
    ('DIS-SOCKET-IP55', 'Surface-Mount 2P+E 16 A Socket IP55', 'Covered socket for outdoor use.', 11.30, 'ITEM', 340, TRUE,
     (SELECT id FROM category WHERE code = 'DIS'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('DIS-SOCKET-32A', 'Industrial 3P+N+E 32 A Socket IP67', 'CEE socket for worksites and workshops.', 26.40, 'ITEM', 90, TRUE,
     (SELECT id FROM category WHERE code = 'DIS'),
     (SELECT id FROM brand WHERE name = 'ABB')),
    ('CND-ICTA-16', 'ICTA 3422 Conduit 16 mm with Draw Wire', 'Supplied as a 100 m coil for concealed installation.', 0.38, 'METER', 5000, TRUE,
     (SELECT id FROM category WHERE code = 'CND'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('CND-ICTA-20', 'ICTA 3422 Conduit 20 mm with Draw Wire', 'Supplied as a 100 m coil for socket circuits.', 0.49, 'METER', 4600, TRUE,
     (SELECT id FROM category WHERE code = 'CND'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('CND-ICTA-25', 'ICTA 3422 Conduit 25 mm with Draw Wire', 'Supplied as a 50 m coil for distribution-board feeds.', 0.71, 'METER', 2400, TRUE,
     (SELECT id FROM category WHERE code = 'CND'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('CND-TPC-63', 'Red TPC Conduit 63 mm', 'Underground network conduit supplied as a 25 m coil.', 2.35, 'METER', 1200, TRUE,
     (SELECT id FROM category WHERE code = 'CND'),
     (SELECT id FROM brand WHERE name = 'Nexans')),
    ('CND-TRUNK-40X25', 'PVC Trunking 40x25 mm with Cover', 'Two-meter length for surface mounting.', 4.60, 'METER', 1600, TRUE,
     (SELECT id FROM category WHERE code = 'CND'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('CND-TRAY-100', 'Perforated Steel Cable Tray 100 mm', 'Three-meter hot-dip galvanized length.', 12.80, 'METER', 700, TRUE,
     (SELECT id FROM category WHERE code = 'CND'),
     (SELECT id FROM brand WHERE name = 'ABB')),
    ('CND-TIE-200', 'Nylon Cable Tie 200x4.8 mm', 'Box of 100 with a 22 kg tensile rating.', 6.90, 'BOX', 420, TRUE,
     (SELECT id FROM category WHERE code = 'CND'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('CND-CEILING-BOX', 'DCL Recessed Ceiling Box 67 mm', 'Ceiling-point box supplied in packs of 20.', 18.40, 'BOX', 280, TRUE,
     (SELECT id FROM category WHERE code = 'CND'),
     (SELECT id FROM brand WHERE name = 'Legrand')),
    ('CND-JUNCTION-IP55', 'IP55 Junction Box 105x105', 'Outdoor connection box supplied in packs of 10.', 22.70, 'BOX', 190, TRUE,
     (SELECT id FROM category WHERE code = 'CND'),
     (SELECT id FROM brand WHERE name = 'Hager'))
ON CONFLICT (reference) DO NOTHING;

-- Two deactivated references, so the soft delete of §6.1 and the active filter
-- of §F1 are demonstrable without an admin having to delete something live.
UPDATE product SET active = FALSE WHERE reference IN ('CAB-COAX-17VATC', 'LGT-SPOT-GU10');

-- One product deliberately left near zero, so the 409 insufficient-stock path
-- of §13 can be demonstrated without first emptying the warehouse.
UPDATE product SET stock_quantity = 3 WHERE reference = 'MOD-SPD-T2';
