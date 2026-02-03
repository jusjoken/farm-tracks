/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  birch
 * Created: Feb 1, 2026
 */
INSERT IGNORE INTO `stock_type` (`id`, `name`, `female_name`, `male_name`, `non_breeder_name`, `default_type`, `breeder_name`, `name_singular`, `image_file_name`) VALUES
(-2, 'Goats',	'Doe',	'Buck',	'Other',	CONVERT(b'0', UNSIGNED),	'Breeders',	'Goat', 'goat_blank.jpg'),
(-1, 'Rabbits',	'Doe',	'Buck',	'Kits',	CONVERT(b'1', UNSIGNED),	'Breeders',	'Rabbit', 'rabbit_blank.jpg');
