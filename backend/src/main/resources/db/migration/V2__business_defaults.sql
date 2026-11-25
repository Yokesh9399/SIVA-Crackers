-- =====================================================================
-- SIVA Crackers – V2 business defaults (Phase 4.2)
--
-- The single business_settings row with ONLY known, approved facts:
--   shop name, Eachanari / Coimbatore / Tamil Nadu, and the website's
--   approved notices, highlights, safety tips and banner text.
--
-- NOT invented: phone, alternate phone, WhatsApp number, email, business
-- hours, street address, pincode and licence details are NULL. The website
-- shows them as "To be updated" until the owner fills them in (Phase 5 admin).
--
-- This is not demo data (is_demo = 0). The demo catalogue is separate and
-- optional (Phase 4.3, APP_SEED_DEMO).
-- =====================================================================

INSERT INTO business_settings (
    id, shop_name, tagline,
    address_line1, area, city, state, pincode,
    phone, alternate_phone, whatsapp_number, whatsapp_enquiry_enabled,
    email, business_hours,
    map_embed_url, map_is_approximate,
    licence_details,
    announcement_text, announcement_active,
    about_text, price_disclaimer, legal_notice, whatsapp_notice,
    created_at, updated_at
) VALUES (
    1, 'SIVA Crackers', 'Family-owned crackers shop in Eachanari, Coimbatore',
    NULL, 'Eachanari', 'Coimbatore', 'Tamil Nadu', NULL,
    NULL, NULL, NULL, TRUE,
    NULL, NULL,
    'https://www.google.com/maps?q=Eachanari,+Coimbatore,+Tamil+Nadu&output=embed', TRUE,
    NULL,
    'Browse our crackers range online · Prices shown are indicative · All purchases are made in person at our Eachanari shop', TRUE,
    'SIVA Crackers is a family-owned crackers retail shop in Eachanari, Coimbatore. Use this website to explore our range, compare indicative prices and plan your list before you visit the shop.',
    'Prices shown are indicative only and may change without notice. Please confirm the final price at the shop.',
    'This website is an information catalogue only. SIVA Crackers does not sell, book, accept payment for, or deliver firecrackers online. All purchases are made in person at our shop, subject to applicable laws and local regulations.',
    'WhatsApp is for general product enquiries only. We do not take, confirm or process firecracker orders over WhatsApp.',
    UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
);

INSERT INTO business_highlights (settings_id, position, title, text) VALUES
    (1, 0, 'Family-owned shop',       'A local, family-run crackers shop in Eachanari, Coimbatore.'),
    (1, 1, 'Browse before you visit', 'See categories, brands and indicative prices from home.'),
    (1, 2, 'Safety first',            'Safety guidance with every product. Please celebrate responsibly.'),
    (1, 3, 'Buy in person',           'All purchases are made in person at the shop counter.');

INSERT INTO safety_tips (settings_id, position, text) VALUES
    (1, 0, 'Always have an adult present. Children must be supervised at all times.'),
    (1, 1, 'Use crackers only in open spaces, away from buildings, vehicles and dry grass.'),
    (1, 2, 'Keep a bucket of water or sand nearby.'),
    (1, 3, 'Light at arm''s length and step back immediately. Never lean over a cracker.'),
    (1, 4, 'Never try to relight a cracker that did not go off. Soak it in water.'),
    (1, 5, 'Wear cotton clothes and footwear. Avoid loose synthetic clothing.'),
    (1, 6, 'Follow the timings and rules set by local authorities.');

INSERT INTO banners (eyebrow, title, subtitle, cta_label, cta_link, theme, display_order, active, is_demo, created_at, updated_at) VALUES
    ('SIVA Crackers · Eachanari', 'Light up every celebration',
     'Explore sparklers, flower pots, aerial shots and gift boxes, all in one place.',
     'Browse products', '/products', 'gold', 1, TRUE, FALSE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Plan before you visit', 'Check indicative prices from home',
     'Use our category-wise price list to plan your list, then visit the shop to buy in person.',
     'View price list', '/price-list', 'orange', 2, TRUE, FALSE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Celebrate responsibly', 'Safety comes first',
     'Simple safety tips for a happy, accident-free festival for your family and neighbours.',
     'Read safety tips', '/safety', 'blue', 3, TRUE, FALSE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
