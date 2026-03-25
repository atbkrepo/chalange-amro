INSERT INTO product (id, name, description, price) VALUES
    (1,  'MacBook Pro 16"',       'Apple M3 Pro, 18GB RAM, 512GB SSD',                   2499.99),
    (2,  'Dell XPS 15',           'Intel i7-13700H, 16GB RAM, 512GB SSD',                1799.99),
    (3,  'iPhone 15 Pro',         '256GB, Titanium, A17 Pro chip',                       1199.99),
    (4,  'Samsung Galaxy S24',    '256GB, Snapdragon 8 Gen 3',                            899.99),
    (5,  'Sony WH-1000XM5',      'Wireless noise-cancelling headphones',                  349.99),
    (6,  'AirPods Pro 2',        'Active noise cancellation, USB-C',                      249.99),
    (7,  'iPad Air M2',          '11-inch, 128GB, Wi-Fi',                                 599.99),
    (8,  'Samsung Galaxy Tab S9', '11-inch, 128GB, Snapdragon 8 Gen 2',                   749.99),
    (9,  'Apple Watch Series 9',  '45mm, GPS, Aluminium case',                             429.99),
    (10, 'Logitech MX Master 3S', 'Wireless ergonomic mouse, 8K DPI',                      99.99),
    (11, 'Mechanical Keyboard',    'Cherry MX Brown switches, RGB, TKL',                   149.99),
    (12, 'LG UltraWide 34"',      '34WN80C-B, USB-C, QHD IPS',                            499.99),
    (13, 'Sony PlayStation 5',     'Disc edition, DualSense controller',                   499.99),
    (14, 'Nintendo Switch OLED',   '7-inch OLED screen, 64GB',                             349.99),
    (15, 'Kindle Paperwhite',      '6.8-inch, 16GB, waterproof',                           139.99)
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory (id, product_id, quantity, reserved_quantity) VALUES
    (1,  1,  25,  0),
    (2,  2,  40,  0),
    (3,  3,  100, 0),
    (4,  4,  80,  0),
    (5,  5,  60,  0),
    (6,  6,  150, 0),
    (7,  7,  45,  0),
    (8,  8,  35,  0),
    (9,  9,  50,  0),
    (10, 10, 200, 0),
    (11, 11, 120, 0),
    (12, 12, 30,  0),
    (13, 13, 20,  0),
    (14, 14, 55,  0),
    (15, 15, 90,  0)
ON CONFLICT (id) DO NOTHING;

SELECT setval('product_id_seq',   (SELECT COALESCE(MAX(id), 0) FROM product),   true);
SELECT setval('inventory_id_seq', (SELECT COALESCE(MAX(id), 0) FROM inventory), true);
