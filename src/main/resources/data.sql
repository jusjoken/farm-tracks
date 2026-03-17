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

