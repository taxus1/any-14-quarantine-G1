-- 集成测试用 H2（MySQL 兼容模式）建表脚本。
-- 真实环境的建表 SQL 在 doc/schema/quarantine.sql（MySQL），本文件只用于本机无 MySQL 时跑端到端测试。
-- 结构与 quarantine.sql 中 t_farm / t_ear_tag 保持一致；
-- H2 不支持 MySQL 的 ENGINE/CHARSET 子句与 TINYINT 个别写法，这里换成 H2 等价写法。

CREATE TABLE t_farm (
    id          BIGINT       NOT NULL PRIMARY KEY,
    farm_no     VARCHAR(32)  NOT NULL,
    farm_name   VARCHAR(64)  NOT NULL,
    owner_name  VARCHAR(64)  DEFAULT NULL,
    phone       VARCHAR(20)  DEFAULT NULL,
    address     VARCHAR(255) DEFAULT NULL,
    species     VARCHAR(16)  NOT NULL,
    stock_qty   INT          NOT NULL DEFAULT 0,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    del_flag    TINYINT      NOT NULL DEFAULT 0,
    create_by   VARCHAR(64)  DEFAULT NULL,
    create_time DATETIME     DEFAULT NULL,
    update_by   VARCHAR(64)  DEFAULT NULL,
    update_time DATETIME     DEFAULT NULL,
    CONSTRAINT uk_farm_no UNIQUE (farm_no)
);
CREATE INDEX idx_farm_species_status ON t_farm (species, status);

CREATE TABLE t_ear_tag (
    id          BIGINT       NOT NULL PRIMARY KEY,
    tag_no      VARCHAR(32)  NOT NULL,
    farm_id     BIGINT       NOT NULL,
    species     VARCHAR(16)  NOT NULL,
    issued_at   DATETIME     DEFAULT NULL,
    worn_at     DATETIME     DEFAULT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ISSUED',
    del_flag    TINYINT      NOT NULL DEFAULT 0,
    create_by   VARCHAR(64)  DEFAULT NULL,
    create_time DATETIME     DEFAULT NULL,
    update_by   VARCHAR(64)  DEFAULT NULL,
    update_time DATETIME     DEFAULT NULL,
    CONSTRAINT uk_tag_no UNIQUE (tag_no)
);
CREATE INDEX idx_tag_farm ON t_ear_tag (farm_id);
CREATE INDEX idx_tag_status ON t_ear_tag (status);
