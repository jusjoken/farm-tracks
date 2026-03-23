-- migrate_legacy_parent_ext_names.sql
-- Purpose:
--   Convert legacy stock.father_ext_name / stock.mother_ext_name values
--   into real external parent stock records, then link by father_id / mother_id.
--
-- Safety:
--   - Idempotent: safe to run multiple times.
--   - Only creates missing external parents.
--   - Only links rows that currently do not have father_id/mother_id.
--   - Clears ext-name fields after linking.
--
-- Expected DB: MariaDB/MySQL used by farm-tracks.

START TRANSACTION;

-- Keep a snapshot of external IDs that existed before this migration run.
DROP TEMPORARY TABLE IF EXISTS tmp_preexisting_external_ids;
CREATE TEMPORARY TABLE tmp_preexisting_external_ids (
    id INT PRIMARY KEY
) ENGINE=Memory
AS
SELECT s.id
FROM stock s
WHERE s.external = 1;

-- Collect all legacy parent-name references that still need linking.
DROP TEMPORARY TABLE IF EXISTS tmp_legacy_parent_refs;
CREATE TEMPORARY TABLE tmp_legacy_parent_refs (
    child_id INT NOT NULL,
    stock_type_id INT NOT NULL,
    parent_name VARCHAR(255) NOT NULL,
    parent_sex VARCHAR(8) NOT NULL,
    parent_role VARCHAR(8) NOT NULL,
    PRIMARY KEY (child_id, parent_role)
) ENGINE=Memory;

INSERT INTO tmp_legacy_parent_refs (child_id, stock_type_id, parent_name, parent_sex, parent_role)
SELECT s.id, s.stock_type_id, TRIM(s.father_ext_name), 'M', 'father'
FROM stock s
WHERE s.father_id IS NULL
  AND s.father_ext_name IS NOT NULL
  AND TRIM(s.father_ext_name) <> '';

INSERT INTO tmp_legacy_parent_refs (child_id, stock_type_id, parent_name, parent_sex, parent_role)
SELECT s.id, s.stock_type_id, TRIM(s.mother_ext_name), 'F', 'mother'
FROM stock s
WHERE s.mother_id IS NULL
  AND s.mother_ext_name IS NOT NULL
  AND TRIM(s.mother_ext_name) <> '';

-- Distinct parent candidates we need available in stock table.
DROP TEMPORARY TABLE IF EXISTS tmp_parent_candidates;
CREATE TEMPORARY TABLE tmp_parent_candidates (
    stock_type_id INT NOT NULL,
    parent_sex VARCHAR(8) NOT NULL,
    parent_name VARCHAR(255) NOT NULL,
    parent_name_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (stock_type_id, parent_sex, parent_name_key)
) ENGINE=Memory;

INSERT INTO tmp_parent_candidates (stock_type_id, parent_sex, parent_name, parent_name_key)
SELECT
    r.stock_type_id,
    r.parent_sex,
    MIN(r.parent_name) AS parent_name,
    LOWER(TRIM(r.parent_name)) AS parent_name_key
FROM tmp_legacy_parent_refs r
GROUP BY r.stock_type_id, r.parent_sex, LOWER(TRIM(r.parent_name));

-- Create missing external parent stock records.
DROP TEMPORARY TABLE IF EXISTS tmp_new_external_candidates;
CREATE TEMPORARY TABLE tmp_new_external_candidates (
  stock_type_id INT NOT NULL,
  parent_sex VARCHAR(8) NOT NULL,
  parent_name VARCHAR(255) NOT NULL,
  parent_name_key VARCHAR(255) NOT NULL,
  PRIMARY KEY (stock_type_id, parent_sex, parent_name_key)
) ENGINE=Memory
AS
SELECT c.stock_type_id, c.parent_sex, c.parent_name, c.parent_name_key
FROM tmp_parent_candidates c
LEFT JOIN stock s
     ON s.stock_type_id = c.stock_type_id
    AND s.sex = c.parent_sex
    AND s.external = 1
    AND LOWER(TRIM(s.name)) = c.parent_name_key
WHERE s.id IS NULL;

SET @stock_base_id := (SELECT next_not_cached_value FROM stock_seq FOR UPDATE);

DROP TEMPORARY TABLE IF EXISTS tmp_new_external_with_id;
CREATE TEMPORARY TABLE tmp_new_external_with_id (
  id INT NOT NULL PRIMARY KEY,
  stock_type_id INT NOT NULL,
  parent_sex VARCHAR(8) NOT NULL,
  parent_name VARCHAR(255) NOT NULL,
  parent_name_key VARCHAR(255) NOT NULL
) ENGINE=Memory
AS
SELECT
  @stock_base_id + ROW_NUMBER() OVER (ORDER BY c.stock_type_id, c.parent_sex, c.parent_name_key) - 1 AS id,
  c.stock_type_id,
  c.parent_sex,
  c.parent_name,
  c.parent_name_key
FROM tmp_new_external_candidates c;

SET @stock_insert_count := (SELECT COUNT(*) FROM tmp_new_external_with_id);

INSERT INTO stock (
  id,
  breeder,
  stock_type_id,
  sex,
  prefix,
  name,
  father_id,
  mother_id,
  father_ext_name,
  mother_ext_name,
  color,
  breed,
  weight,
  weight_date,
  dob,
  acquired,
  reg_no,
  champ_no,
  legs,
  notes,
  status,
  status_date,
  profile_image,
  litter_id,
  foster_litter_id,
  stock_value,
  external,
  created_date,
  last_modified_date,
  genotype,
  invoice_number,
  sale_status,
  tattoo
)
SELECT
  c.id,
  1 AS breeder,
  c.stock_type_id,
  c.parent_sex AS sex,
  '' AS prefix,
  c.parent_name AS name,
  NULL AS father_id,
  NULL AS mother_id,
  NULL AS father_ext_name,
  NULL AS mother_ext_name,
  '' AS color,
  '' AS breed,
  0 AS weight,
  NOW() AS weight_date,
  NULL AS dob,
  NULL AS acquired,
  '' AS reg_no,
  '' AS champ_no,
  '' AS legs,
  '' AS notes,
  'archived' AS status,
  NOW() AS status_date,
  NULL AS profile_image,
  NULL AS litter_id,
  NULL AS foster_litter_id,
  NULL AS stock_value,
  1 AS external,
  NOW() AS created_date,
  NOW() AS last_modified_date,
  '' AS genotype,
  '' AS invoice_number,
  'NONE' AS sale_status,
  '' AS tattoo
FROM tmp_new_external_with_id c;

UPDATE stock_seq
SET next_not_cached_value = @stock_base_id + @stock_insert_count;

-- Add archived status history for external parents created in this run.
SET @ssh_base_id := (SELECT next_not_cached_value FROM stock_status_history_seq FOR UPDATE);

DROP TEMPORARY TABLE IF EXISTS tmp_new_external_status_rows;
CREATE TEMPORARY TABLE tmp_new_external_status_rows (
  id INT NOT NULL PRIMARY KEY,
  stock_id INT NOT NULL
) ENGINE=Memory
AS
SELECT
  @ssh_base_id + ROW_NUMBER() OVER (ORDER BY s.id) - 1 AS id,
  s.id AS stock_id
FROM stock s
LEFT JOIN tmp_preexisting_external_ids p
     ON p.id = s.id
WHERE s.external = 1
  AND p.id IS NULL
  AND NOT EXISTS (
    SELECT 1
    FROM stock_status_history h
    WHERE h.stock_id = s.id
    AND LOWER(h.status_name) = 'archived'
  );

SET @ssh_insert_count := (SELECT COUNT(*) FROM tmp_new_external_status_rows);

INSERT INTO stock_status_history (
  id,
    stock_id,
    status_name,
    custom_date,
    note,
    created_date,
    last_modified_date
)
SELECT
  r.id,
  r.stock_id,
    'archived',
    NOW(),
    'Migration: created from legacy father_ext_name/mother_ext_name',
    NOW(),
    NOW()
FROM tmp_new_external_status_rows r;

UPDATE stock_status_history_seq
SET next_not_cached_value = @ssh_base_id + @ssh_insert_count;

-- Resolve each legacy parent ref to a concrete stock parent id.
DROP TEMPORARY TABLE IF EXISTS tmp_resolved_parent_links;
CREATE TEMPORARY TABLE tmp_resolved_parent_links (
    child_id INT NOT NULL,
    parent_role VARCHAR(8) NOT NULL,
    parent_id INT NOT NULL,
    PRIMARY KEY (child_id, parent_role)
) ENGINE=Memory;

INSERT INTO tmp_resolved_parent_links (child_id, parent_role, parent_id)
SELECT
    r.child_id,
    r.parent_role,
    MIN(s.id) AS parent_id
FROM tmp_legacy_parent_refs r
JOIN stock s
  ON s.stock_type_id = r.stock_type_id
 AND s.sex = r.parent_sex
 AND s.external = 1
 AND LOWER(TRIM(s.name)) = LOWER(TRIM(r.parent_name))
GROUP BY r.child_id, r.parent_role;

-- Link fathers.
UPDATE stock child
JOIN tmp_resolved_parent_links l
  ON l.child_id = child.id
 AND l.parent_role = 'father'
SET child.father_id = l.parent_id,
    child.father_ext_name = NULL,
    child.last_modified_date = NOW()
WHERE child.father_id IS NULL;

-- Link mothers.
UPDATE stock child
JOIN tmp_resolved_parent_links l
  ON l.child_id = child.id
 AND l.parent_role = 'mother'
SET child.mother_id = l.parent_id,
    child.mother_ext_name = NULL,
    child.last_modified_date = NOW()
WHERE child.mother_id IS NULL;

-- Cleanup any stale ext names where an id is already linked.
UPDATE stock
SET father_ext_name = NULL,
    last_modified_date = NOW()
WHERE father_id IS NOT NULL
  AND father_ext_name IS NOT NULL
  AND TRIM(father_ext_name) <> '';

UPDATE stock
SET mother_ext_name = NULL,
    last_modified_date = NOW()
WHERE mother_id IS NOT NULL
  AND mother_ext_name IS NOT NULL
  AND TRIM(mother_ext_name) <> '';

-- Optional verification outputs.
SELECT 'rows still needing father migration' AS metric, COUNT(*) AS value
FROM stock
WHERE father_id IS NULL
  AND father_ext_name IS NOT NULL
  AND TRIM(father_ext_name) <> '';

SELECT 'rows still needing mother migration' AS metric, COUNT(*) AS value
FROM stock
WHERE mother_id IS NULL
  AND mother_ext_name IS NOT NULL
  AND TRIM(mother_ext_name) <> '';

COMMIT;
