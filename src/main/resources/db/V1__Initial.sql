CREATE TABLE IF NOT EXISTS `rss_sources` (
   `id` INT NOT NULL AUTO_INCREMENT  COMMENT '主键',
   `source_url` VARCHAR(2048) NOT NULL COMMENT 'RSS 订阅源 URL',
   `source_name` VARCHAR(2048) DEFAULT NULL COMMENT '订阅源名称',
   `description` LONGTEXT COMMENT '订阅源描述',
   `last_checked` DATETIME COMMENT '上次检查时间',
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT 'RSS 订阅源信息';

CREATE TABLE IF NOT EXISTS `rss_items` (
     `id` INT AUTO_INCREMENT COMMENT '主键',
     `source_id` INT NOT NULL COMMENT 'RSS 订阅源 id',
     `title` VARCHAR(2048) NOT NULL COMMENT '消息标题',
     `link` VARCHAR(2048) NOT NULL COMMENT '消息链接',
     `link_hash` CHAR(64) NOT NULL COMMENT 'link hash',
     `pub_date` DATETIME COMMENT '发布时间',
     `description` LONGTEXT COMMENT '消息描述',
     PRIMARY KEY (`id`),
     UNIQUE KEY `unique_link_hash` (`link_hash`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT 'RSS 消息信息';

CREATE TABLE IF NOT EXISTS `users` (
 `telegram_id` BIGINT NOT NULL COMMENT 'Telegram 用户 ID',
 `username` VARCHAR(100) DEFAULT NULL COMMENT '用户名',
 `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 PRIMARY KEY (`telegram_id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '用户信息';

CREATE TABLE IF NOT EXISTS `user_subscriptions` (
  `telegram_id` BIGINT NOT NULL COMMENT 'Telegram 用户 ID',
  `source_id` INT NOT NULL COMMENT '订阅源表主键',
  `subscribed_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '订阅时间',
  `notified_at` DATETIME DEFAULT NULL COMMENT '通知时间',
  KEY (`telegram_id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '用户订阅信息';
