-- =====================================================================
-- 公路广告牌管理系统  数据库结构脚本（MySQL 8.x）
-- 字符集 utf8mb4，金额一律 DECIMAL(14,2)，日期 DATE / 时间 DATETIME
-- =====================================================================
CREATE DATABASE IF NOT EXISTS billboard DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE billboard;

-- ---------------------------------------------------------------------
-- 1. 系统用户
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  username    VARCHAR(50)  NOT NULL UNIQUE      COMMENT '登录账号',
  password    VARCHAR(100) NOT NULL             COMMENT '密码（{noop} 前缀为明文演示，生产请改 BCrypt）',
  name        VARCHAR(50)  NOT NULL             COMMENT '姓名',
  role        VARCHAR(20)  NOT NULL             COMMENT '角色：admin/archivist/finance/leader',
  status      VARCHAR(10)  NOT NULL DEFAULT '启用' COMMENT '启用/停用',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='系统用户';

-- ---------------------------------------------------------------------
-- 2. 数据字典
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_dict;
CREATE TABLE sys_dict (
  id    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  type  VARCHAR(50) NOT NULL UNIQUE COMMENT '字典类型编码',
  name  VARCHAR(50) NOT NULL        COMMENT '字典名称'
) ENGINE=InnoDB COMMENT='数据字典类型';

DROP TABLE IF EXISTS sys_dict_item;
CREATE TABLE sys_dict_item (
  id        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  dict_type VARCHAR(50) NOT NULL COMMENT '所属字典类型',
  label     VARCHAR(100) NOT NULL COMMENT '选项名称',
  sort      INT NOT NULL DEFAULT 0 COMMENT '排序',
  KEY idx_dict_type (dict_type)
) ENGINE=InnoDB COMMENT='数据字典项';

-- ---------------------------------------------------------------------
-- 3. 操作日志
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
  id        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  username  VARCHAR(50)  NOT NULL COMMENT '操作账号',
  name      VARCHAR(50)  NOT NULL COMMENT '操作人姓名',
  action    VARCHAR(50)  NOT NULL COMMENT '动作（新增/修改/删除/收款登记/登录...）',
  detail    VARCHAR(500) NOT NULL COMMENT '操作内容',
  KEY idx_time (time)
) ENGINE=InnoDB COMMENT='操作日志';

-- ---------------------------------------------------------------------
-- 4. 路段档案（路段 → 地区 → 运营单位）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS biz_section;
CREATE TABLE biz_section (
  id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(50)  NOT NULL UNIQUE COMMENT '路段编号',
  name       VARCHAR(100) NOT NULL        COMMENT '路段名称',
  region     VARCHAR(100) NOT NULL        COMMENT '所属地区',
  operator   VARCHAR(100) NOT NULL        COMMENT '运营单位',
  mileage    VARCHAR(100) DEFAULT NULL    COMMENT '里程范围',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='路段档案';

-- ---------------------------------------------------------------------
-- 5. 广告牌档案（三个桩号分别记录 + 合规证件）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS biz_billboard;
CREATE TABLE biz_billboard (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code              VARCHAR(50)  NOT NULL UNIQUE COMMENT '广告牌编号',
  section_id        BIGINT UNSIGNED NOT NULL     COMMENT '所属路段',
  side              VARCHAR(20)  DEFAULT NULL    COMMENT '位置侧别：左侧/右侧/中央分隔带',
  stake_contract    VARCHAR(50)  DEFAULT NULL    COMMENT '合同桩号',
  stake_actual      VARCHAR(50)  DEFAULT NULL    COMMENT '现场实际桩号',
  stake_approval    VARCHAR(50)  DEFAULT NULL    COMMENT '路政批复桩号',
  spec              VARCHAR(50)  DEFAULT NULL    COMMENT '规格（字典 spec）',
  width             DECIMAL(6,1) DEFAULT NULL    COMMENT '版面宽(米)',
  height            DECIMAL(6,1) DEFAULT NULL    COMMENT '版面高(米)',
  ownership         VARCHAR(50)  DEFAULT NULL    COMMENT '产权（字典 ownership）',
  land_nature       VARCHAR(50)  DEFAULT NULL    COMMENT '用地性质（字典 landNature）',
  builder           VARCHAR(100) DEFAULT NULL    COMMENT '建设单位',
  build_year        INT          DEFAULT NULL    COMMENT '建成年份',
  status            VARCHAR(10)  NOT NULL DEFAULT '空闲' COMMENT '在租/空闲/维护中/停用',
  permit_no         VARCHAR(100) DEFAULT NULL    COMMENT '路政许可证编号',
  permit_expire     DATE         DEFAULT NULL    COMMENT '路政许可有效期至',
  insurance_no      VARCHAR(100) DEFAULT NULL    COMMENT '保险单号',
  insurance_expire  DATE         DEFAULT NULL    COMMENT '保险有效期至',
  annual_check_no   VARCHAR(100) DEFAULT NULL    COMMENT '年审记录编号',
  annual_check_date DATE         DEFAULT NULL    COMMENT '最近年审日期',
  annual_check_expire DATE       DEFAULT NULL    COMMENT '年审有效期至',
  photos            VARCHAR(1000) DEFAULT NULL   COMMENT '照片路径，逗号分隔',
  remark            VARCHAR(500) DEFAULT NULL    COMMENT '备注',
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_section (section_id),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='广告牌档案';

-- 广告牌新建、拆除、移位等档案变动记录（审批流程待甲方确认）
DROP TABLE IF EXISTS biz_billboard_change;
CREATE TABLE biz_billboard_change (
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

-- ---------------------------------------------------------------------
-- 6. 商家档案
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS biz_merchant;
CREATE TABLE biz_merchant (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(100) NOT NULL        COMMENT '商家名称',
  contact      VARCHAR(50)  DEFAULT NULL    COMMENT '联系人',
  phone        VARCHAR(30)  DEFAULT NULL    COMMENT '联系电话',
  address      VARCHAR(200) DEFAULT NULL    COMMENT '地址',
  tax_no       VARCHAR(50)  DEFAULT NULL    COMMENT '纳税人识别号',
  bank_name    VARCHAR(100) DEFAULT NULL    COMMENT '开户行',
  bank_account VARCHAR(50)  DEFAULT NULL    COMMENT '银行账号',
  blacklisted  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否黑名单：0否 1是',
  remark       VARCHAR(500) DEFAULT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='商家档案';

-- ---------------------------------------------------------------------
-- 7. 合同（一份合同绑定多块广告牌）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS biz_contract;
CREATE TABLE biz_contract (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code         VARCHAR(50)   NOT NULL UNIQUE COMMENT '合同编号',
  merchant_id  BIGINT UNSIGNED NOT NULL      COMMENT '签约商家',
  sign_date    DATE          DEFAULT NULL    COMMENT '签订日期',
  start_date   DATE          NOT NULL        COMMENT '起租日期',
  end_date     DATE          NOT NULL        COMMENT '到期日期',
  years        INT           NOT NULL        COMMENT '年限',
  total_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '合同总额（年价合计×年限）',
  status       VARCHAR(10)   NOT NULL DEFAULT '草稿' COMMENT '草稿/生效/到期/解除',
  remark       VARCHAR(500)  DEFAULT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_merchant (merchant_id),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='租赁合同';

DROP TABLE IF EXISTS biz_contract_item;
CREATE TABLE biz_contract_item (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  contract_id  BIGINT UNSIGNED NOT NULL COMMENT '合同ID',
  billboard_id BIGINT UNSIGNED NOT NULL COMMENT '广告牌ID',
  yearly_price DECIMAL(14,2)  NOT NULL  COMMENT '该牌每年租金',
  KEY idx_contract (contract_id),
  KEY idx_billboard (billboard_id)
) ENGINE=InnoDB COMMENT='合同明细（年×牌价格）';

-- ---------------------------------------------------------------------
-- 8. 应收账单（合同生效时按 年×牌 拆分生成）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS biz_receivable;
CREATE TABLE biz_receivable (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  contract_id  BIGINT UNSIGNED NOT NULL COMMENT '合同ID',
  merchant_id  BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  billboard_id BIGINT UNSIGNED NOT NULL COMMENT '广告牌ID',
  year         INT            NOT NULL  COMMENT '应收年度',
  amount       DECIMAL(14,2)  NOT NULL  COMMENT '应收金额',
  received     DECIMAL(14,2)  NOT NULL DEFAULT 0 COMMENT '已收金额',
  status       VARCHAR(10)    NOT NULL DEFAULT '未收' COMMENT '未收/部分/已收',
  KEY idx_contract (contract_id),
  KEY idx_merchant (merchant_id),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='应收账单（应收账）';

-- ---------------------------------------------------------------------
-- 9. 资金流水（实收实付账，与应收分开记）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS biz_transaction;
CREATE TABLE biz_transaction (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tx_date     DATE          NOT NULL COMMENT '收付日期',
  direction   VARCHAR(10)   NOT NULL COMMENT '收入/支出',
  category    VARCHAR(50)   NOT NULL COMMENT '类别（字典 txCategory）',
  amount      DECIMAL(14,2) NOT NULL COMMENT '金额',
  merchant_id BIGINT UNSIGNED DEFAULT NULL COMMENT '相关商家（可空=挂账）',
  contract_id BIGINT UNSIGNED DEFAULT NULL COMMENT '相关合同（可空）',
  summary     VARCHAR(200)  DEFAULT NULL   COMMENT '摘要',
  operator    VARCHAR(50)   DEFAULT NULL   COMMENT '经办人',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_date (tx_date),
  KEY idx_direction (direction)
) ENGINE=InnoDB COMMENT='资金流水（实收实付账）';
