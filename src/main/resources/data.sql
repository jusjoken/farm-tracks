/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  birch
 * Created: Feb 1, 2026
 */

CREATE TABLE IF NOT EXISTS `app_user` (
	`id` INT NOT NULL AUTO_INCREMENT,
	`username` VARCHAR(100) NOT NULL,
	`password_hash` VARCHAR(255) NOT NULL,
	`enabled` BIT(1) NOT NULL,
	PRIMARY KEY (`id`),
	UNIQUE KEY `uk_app_user_username` (`username`)
);

CREATE TABLE IF NOT EXISTS `app_user_role` (
	`user_id` INT NOT NULL,
	`role_name` VARCHAR(30) NOT NULL,
	PRIMARY KEY (`user_id`, `role_name`),
	CONSTRAINT `fk_app_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `persistent_logins` (
	`username` VARCHAR(100) NOT NULL,
	`series` VARCHAR(64) NOT NULL,
	`token` VARCHAR(64) NOT NULL,
	`last_used` TIMESTAMP NOT NULL,
	PRIMARY KEY (`series`)
);

ALTER TABLE `app_user` ADD COLUMN IF NOT EXISTS `display_name` VARCHAR(150) NULL;

-- Ensure task.type accepts all known task short names, including Breeding.
ALTER TABLE `task` DROP CONSTRAINT IF EXISTS `type`;

ALTER TABLE `task`
	MODIFY COLUMN `type` VARCHAR(255) DEFAULT NULL CHECK (`type` IN (
		'Butcher',
		'Wean',
		'Breed',
		'Rebreed',
		'Remove Nestbox',
		'Pregnancy Check',
		'Nestbox',
		'Medical',
		'Custom',
		'Birth',
		'Clean Nestbox',
		'Due Date',
		'Breeding'
	));

INSERT IGNORE INTO `stock_type` (`id`, `name`, `female_name`, `male_name`, `non_breeder_name`, `default_type`, `breeder_name`, `name_singular`, `image_file_name`) VALUES
(-2, 'Goats',   'Doe',  'Buck', 'Other', CONVERT(b'0', UNSIGNED), 'Breeders', 'Goat',   'goat_blank.jpg'),
(-1, 'Rabbits', 'Doe',  'Buck', 'Kits',  CONVERT(b'1', UNSIGNED), 'Breeders', 'Rabbit', 'rabbit_blank.jpg');

INSERT INTO `app_settings` (
	`id`,
	`farm_name`,
	`farm_address_line1`,
	`farm_address_line2`,
	`farm_email`,
	`farm_prefix`,
	`default_litter_prefix`
)
SELECT
	1,
	'Breza Homestead & Rabbitry',
	'RR5 Site 3 Box 21',
	'Rimbey Alberta T0C 2J0',
	'equidanes@hotmail.ca',
	'Breza''s',
	'BHR'
WHERE NOT EXISTS (SELECT 1 FROM `app_settings`);

INSERT INTO `app_settings_seq` (
	`next_not_cached_value`,
	`minimum_value`,
	`maximum_value`,
	`start_value`,
	`increment`,
	`cache_size`,
	`cycle_option`,
	`cycle_count`
)
SELECT
	51,
	1,
	9223372036854775806,
	1,
	50,
	0,
	0,
	0
WHERE NOT EXISTS (SELECT 1 FROM `app_settings_seq`);

-- Idempotent seed: INSERT IGNORE alone is not enough here because this table has no
-- uniqueness constraint on (id, genotypes). Use NOT EXISTS per row to prevent duplicates.
INSERT INTO `stock_genotypes` (`id`, `genotypes`)
SELECT seed.`id`, seed.`genotypes`
FROM (
	SELECT -1 AS `id`, 'A,at,a,_' AS `genotypes`
	UNION ALL SELECT -1, 'B,b,_'
	UNION ALL SELECT -1, 'C,cchd,cchl,c,_'
	UNION ALL SELECT -1, 'D,d,_'
	UNION ALL SELECT -1, 'E,Es,ej,e,_'
	UNION ALL SELECT -1, 'En,en,_'
	UNION ALL SELECT -1, 'V,v,_'
	UNION ALL SELECT -1, 'W,w,_'
	UNION ALL SELECT -1, 'DU,du,_'
	UNION ALL SELECT -1, 'Si,si,si1,si2,si3,_'
) AS seed
WHERE NOT EXISTS (
	SELECT 1
	FROM `stock_genotypes` existing
	WHERE existing.`id` = seed.`id`
	  AND existing.`genotypes` = seed.`genotypes`
);
-- 2026-03-22 21:29:38 UTC

