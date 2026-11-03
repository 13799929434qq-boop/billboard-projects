-- 已部署数据库升级：补全年审编号和有效期字段。
-- 全新安装无需执行本文件，schema.sql 已包含这些字段。
ALTER TABLE biz_billboard
  ADD COLUMN annual_check_no VARCHAR(100) DEFAULT NULL COMMENT '年审记录编号' AFTER insurance_expire,
  ADD COLUMN annual_check_expire DATE DEFAULT NULL COMMENT '年审有效期至' AFTER annual_check_date;

