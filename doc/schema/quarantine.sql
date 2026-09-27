-- any-14-quarantine · 动物卫生监督（产地检疫与调运监管）· 建表 SQL
-- 字符集 utf8mb4，时区 Asia/Shanghai。create 阶段建好，模型只写业务代码，不碰建表。
-- 列名即契约：del_flag 由 @TableLogic 自动拼接（查询带 del_flag=0，删除置 1），
-- create_by/update_by/create_time/update_time 由 AutoFillMetaObjectHandler 自动填充，业务代码不要手写。
-- 主键 id 由应用侧雪花分配（IdType.INPUT），不依赖自增。

-- 1) 养殖场档案
CREATE TABLE IF NOT EXISTS t_farm (
    id          BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    farm_no     VARCHAR(32) NOT NULL COMMENT '养殖场编号，全局唯一（如 FM-2026-0001）',
    farm_name   VARCHAR(64) NOT NULL COMMENT '养殖场名称',
    owner_name  VARCHAR(64) DEFAULT NULL COMMENT '负责人姓名',
    phone       VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    address     VARCHAR(255) DEFAULT NULL COMMENT '场址',
    species     VARCHAR(16) NOT NULL COMMENT '养殖种类 PIG 猪 / CATTLE 牛 / SHEEP 羊 / POULTRY 禽',
    stock_qty   INT         NOT NULL DEFAULT 0 COMMENT '当前存栏数',
    status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE 在用 / SUSPENDED 停业 / CLOSED 注销',
    del_flag    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by   VARCHAR(64) DEFAULT NULL,
    create_time DATETIME    DEFAULT NULL,
    update_by   VARCHAR(64) DEFAULT NULL,
    update_time DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_farm_no (farm_no),
    KEY idx_species_status (species, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='养殖场档案';

-- 2) 畜禽耳标
CREATE TABLE IF NOT EXISTS t_ear_tag (
    id          BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    tag_no      VARCHAR(32) NOT NULL COMMENT '耳标号，全局唯一（如 ET-2026-000001）',
    farm_id     BIGINT      NOT NULL COMMENT '所属养殖场 id（t_farm.id）',
    species     VARCHAR(16) NOT NULL COMMENT '畜禽种类，随养殖场档案带出 PIG/CATTLE/SHEEP/POULTRY',
    issued_at   DATETIME    DEFAULT NULL COMMENT '发放时刻',
    worn_at     DATETIME    DEFAULT NULL COMMENT '佩戴时刻',
    status      VARCHAR(16) NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED 已发放待佩戴 / USED 已佩戴 / LOST 遗失 / DISABLED 停用',
    del_flag    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by   VARCHAR(64) DEFAULT NULL,
    create_time DATETIME    DEFAULT NULL,
    update_by   VARCHAR(64) DEFAULT NULL,
    update_time DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_tag_no (tag_no),
    KEY idx_farm (farm_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='畜禽耳标';

-- 3) 强制免疫登记
CREATE TABLE IF NOT EXISTS t_immunity (
    id             BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    immunity_no    VARCHAR(32) NOT NULL COMMENT '免疫单号，全局唯一（如 IM-2026-0001）',
    farm_id        BIGINT      NOT NULL COMMENT '养殖场 id（t_farm.id）',
    vaccine_name   VARCHAR(64) NOT NULL COMMENT '疫苗名称',
    vaccine_batch  VARCHAR(64) DEFAULT NULL COMMENT '疫苗批次',
    dose_required  INT         NOT NULL DEFAULT 0 COMMENT '应免数',
    dose_actual    INT         NOT NULL DEFAULT 0 COMMENT '实免数',
    immunized_at   DATETIME    DEFAULT NULL COMMENT '免疫时刻',
    valid_until    DATETIME    DEFAULT NULL COMMENT '免疫有效期至',
    operator       VARCHAR(64) DEFAULT NULL COMMENT '免疫员（登录账号）',
    del_flag       TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by      VARCHAR(64) DEFAULT NULL,
    create_time    DATETIME    DEFAULT NULL,
    update_by      VARCHAR(64) DEFAULT NULL,
    update_time    DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_immunity_no (immunity_no),
    KEY idx_farm (farm_id),
    KEY idx_valid (valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='强制免疫登记';

-- 4) 产地检疫申报
CREATE TABLE IF NOT EXISTS t_quarantine_apply (
    id            BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    apply_no      VARCHAR(32) NOT NULL COMMENT '申报单号，全局唯一（如 QN-2026-0001）',
    farm_id       BIGINT      NOT NULL COMMENT '养殖场 id（t_farm.id）',
    species       VARCHAR(16) NOT NULL COMMENT '畜禽种类 PIG/CATTLE/SHEEP/POULTRY',
    quantity      INT         NOT NULL DEFAULT 0 COMMENT '申报数量',
    purpose       VARCHAR(16) NOT NULL DEFAULT 'SLAUGHTER' COMMENT '用途 SLAUGHTER 屠宰 / BREEDING 种用 / OTHER 其他',
    destination   VARCHAR(255) DEFAULT NULL COMMENT '目的地',
    applied_at    DATETIME    DEFAULT NULL COMMENT '申报时刻',
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待受理 / QUALIFIED 检疫合格 / REJECTED 检疫不合格 / CANCELLED 已撤销',
    reject_reason VARCHAR(255) DEFAULT NULL COMMENT '不合格原因',
    inspector     VARCHAR(64) DEFAULT NULL COMMENT '受理官方兽医（登录账号）',
    del_flag      TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by     VARCHAR(64) DEFAULT NULL,
    create_time   DATETIME    DEFAULT NULL,
    update_by     VARCHAR(64) DEFAULT NULL,
    update_time   DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_apply_no (apply_no),
    KEY idx_farm (farm_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产地检疫申报';

-- 5) 检疫合格证明
CREATE TABLE IF NOT EXISTS t_quarantine_cert (
    id           BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    cert_no      VARCHAR(32) NOT NULL COMMENT '证明编号，全局唯一（如 QC-2026-00001）',
    apply_id     BIGINT      NOT NULL COMMENT '来源申报 id（t_quarantine_apply.id）',
    farm_id      BIGINT      NOT NULL COMMENT '养殖场 id（t_farm.id）',
    species      VARCHAR(16) NOT NULL COMMENT '畜禽种类 PIG/CATTLE/SHEEP/POULTRY',
    quantity     INT         NOT NULL DEFAULT 0 COMMENT '证明数量',
    issued_at    DATETIME    DEFAULT NULL COMMENT '签发时刻',
    valid_until  DATETIME    DEFAULT NULL COMMENT '有效期至',
    official_vet VARCHAR(64) DEFAULT NULL COMMENT '签发官方兽医（登录账号）',
    status       VARCHAR(16) NOT NULL DEFAULT 'VALID' COMMENT 'VALID 有效 / USED 已启运核销 / REVOKED 已撤销 / EXPIRED 已过期',
    del_flag     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by    VARCHAR(64) DEFAULT NULL,
    create_time  DATETIME    DEFAULT NULL,
    update_by    VARCHAR(64) DEFAULT NULL,
    update_time  DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_cert_no (cert_no),
    KEY idx_apply (apply_id),
    KEY idx_status (status),
    KEY idx_valid (valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检疫合格证明';

-- 6) 调运登记（启运与落地报告）
CREATE TABLE IF NOT EXISTS t_transport (
    id           BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    transport_no VARCHAR(32) NOT NULL COMMENT '调运单号，全局唯一（如 TP-2026-0001）',
    cert_id      BIGINT      NOT NULL COMMENT '所用证明 id（t_quarantine_cert.id）',
    farm_id      BIGINT      NOT NULL COMMENT '起运养殖场 id（t_farm.id）',
    vehicle_no   VARCHAR(16) NOT NULL COMMENT '运输车牌',
    driver_name  VARCHAR(64) DEFAULT NULL COMMENT '承运人',
    from_place   VARCHAR(255) DEFAULT NULL COMMENT '起运地',
    to_place     VARCHAR(255) DEFAULT NULL COMMENT '目的地',
    depart_at    DATETIME    DEFAULT NULL COMMENT '启运时刻',
    arrive_at    DATETIME    DEFAULT NULL COMMENT '到达时刻',
    reported_at  DATETIME    DEFAULT NULL COMMENT '落地报告时刻',
    status       VARCHAR(16) NOT NULL DEFAULT 'IN_TRANSIT' COMMENT 'IN_TRANSIT 在途 / ARRIVED 已到场 / CANCELLED 已取消',
    del_flag     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by    VARCHAR(64) DEFAULT NULL,
    create_time  DATETIME    DEFAULT NULL,
    update_by    VARCHAR(64) DEFAULT NULL,
    update_time  DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_transport_no (transport_no),
    KEY idx_cert (cert_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调运登记';

-- 7) 病死畜禽无害化处理
CREATE TABLE IF NOT EXISTS t_disposal (
    id           BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    disposal_no  VARCHAR(32) NOT NULL COMMENT '处理单号，全局唯一（如 DP-2026-0001）',
    farm_id      BIGINT      NOT NULL COMMENT '养殖场 id（t_farm.id）',
    species      VARCHAR(16) NOT NULL COMMENT '畜禽种类 PIG/CATTLE/SHEEP/POULTRY',
    quantity     INT         NOT NULL DEFAULT 0 COMMENT '处理数量',
    reason       VARCHAR(16) NOT NULL COMMENT '原因 DISEASE 疫病 / ACCIDENT 意外 / OTHER 其他',
    method       VARCHAR(16) NOT NULL COMMENT '方式 INCINERATE 焚烧 / BURY 深埋 / COMPOST 堆肥',
    handled_at   DATETIME    DEFAULT NULL COMMENT '处理时刻',
    handler      VARCHAR(64) DEFAULT NULL COMMENT '处理单位',
    del_flag     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by    VARCHAR(64) DEFAULT NULL,
    create_time  DATETIME    DEFAULT NULL,
    update_by    VARCHAR(64) DEFAULT NULL,
    update_time  DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_disposal_no (disposal_no),
    KEY idx_farm (farm_id),
    KEY idx_reason (reason)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病死畜禽无害化处理';
