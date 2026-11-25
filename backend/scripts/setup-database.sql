-- SIVA Crackers – one-time MySQL setup
--
-- Creates the application database, the integration-test database and a
-- dedicated least-privilege user `siva_app` (instead of using root).
--
-- HOW TO USE (see backend/README.md):
--   1. Open a MySQL prompt as an administrator:   mysql -u root -p
--   2. Copy these statements into the prompt.
--   3. Replace  <choose-a-strong-password>  with your own password while typing.
--      Do NOT save your real password in this file and do NOT commit it.
--   4. Put the same password in your own DB_PASSWORD environment variable.

CREATE DATABASE IF NOT EXISTS siva_crackers
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS siva_crackers_test
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'siva_app'@'localhost' IDENTIFIED BY '<choose-a-strong-password>';

-- Only the rights the application and Flyway migrations need, only on these two databases.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES
  ON siva_crackers.* TO 'siva_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES
  ON siva_crackers_test.* TO 'siva_app'@'localhost';

FLUSH PRIVILEGES;
