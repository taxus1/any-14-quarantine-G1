-- 测试底账：模拟“库里躺着早先录进去的数据”，验证不是当空库使。
-- 每个用例类跑前重建（spring.sql.init.mode=always + schema 先 DROP-less 重建靠内存库特性）。

-- 在场 5 条：3 条正常、1 条停业、1 条已软删（且软删的编号最大，用来证明续号不回收、名单不翻出）
INSERT INTO t_farm (id, farm_no, farm_name, owner_name, phone, address, species, stock_qty, status, del_flag, create_by, create_time, update_by, update_time) VALUES
 (1001, 'FM-2026-0001', '东山养猪场', '张三', '13800001111', '东山脚下1号', 'PIG', 500, 'ACTIVE', 0, 'admin', '2026-01-05 09:00:00', 'admin', '2026-01-05 09:00:00'),
 (1002, 'FM-2026-0002', '西坡牛场',   '李四', '13800002222', '西坡村2号',     'CATTLE', 30, 'ACTIVE', 0, 'admin', '2026-01-06 09:00:00', 'admin', '2026-01-06 09:00:00'),
 (1003, 'FM-2026-0003', '南湖羊场',   '王五', '13800003333', '南湖边3号',     'SHEEP', 80, 'SUSPENDED', 0, 'admin', '2026-01-07 09:00:00', 'admin', '2026-01-07 09:00:00'),
 (9009, 'FM-2026-0007', '已注销旧场', '赵六', '13800009999', '老地址9号',     'POULTRY', 0, 'CLOSED', 1, 'admin', '2026-01-02 09:00:00', 'admin', '2026-02-01 09:00:00');

-- 耳标底账：两条正常、一条已软删且编号最大
INSERT INTO t_ear_tag (id, tag_no, farm_id, species, issued_at, worn_at, status, del_flag, create_by, create_time, update_by, update_time) VALUES
 (2001, 'ET-2026-000001', 1001, 'PIG', '2026-02-01 08:00:00', NULL, 'ISSUED', 0, 'admin', '2026-02-01 08:00:00', 'admin', '2026-02-01 08:00:00'),
 (2002, 'ET-2026-000002', 1002, 'CATTLE', '2026-02-02 08:00:00', '2026-02-03 08:00:00', 'USED', 0, 'admin', '2026-02-02 08:00:00', 'admin', '2026-02-03 08:00:00'),
 (9002, 'ET-2026-000004', 1001, 'PIG', '2026-01-10 08:00:00', NULL, 'DISABLED', 1, 'admin', '2026-01-10 08:00:00', 'admin', '2026-01-10 08:00:00');
