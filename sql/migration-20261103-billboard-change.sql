CREATE TABLE IF NOT EXISTS biz_billboard_change (
  id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  billboard_id   BIGINT UNSIGNED NOT NULL COMMENT '广告牌ID',
  change_type    VARCHAR(20) NOT NULL COMMENT '新建/拆除/移位/信息变更',
  change_date    DATE NOT NULL COMMENT '变动日期',
  before_value   VARCHAR(500) DEFAULT NULL COMMENT '变动前情况',
  after_value    VARCHAR(500) DEFAULT NULL COMMENT '变动后情况',
  reason         VARCHAR(500) DEFAULT NULL COMMENT '变动原因',
  operator       VARCHAR(50) NOT NULL COMMENT '登记人',
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_billboard_change_billboard (billboard_id),
  KEY idx_billboard_change_date (change_date)
) ENGINE=InnoDB COMMENT='广告牌变动记录';

