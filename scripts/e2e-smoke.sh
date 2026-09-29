#!/usr/bin/env bash
# 养殖场 + 耳标两模块的端到端冒烟脚本。
# 用法: scripts/e2e-smoke.sh [BASE_URL]
# 依赖：curl、jq
set -u
BASE="${1:-http://localhost:8080}"
J=/tmp/somepro-cookie.txt
rm -f "$J"
PASS=0; FAIL=0

req() { # method path [json]
  local m="$1" p="$2" body="${3:-}"
  if [ -n "$body" ]; then
    curl -s -c "$J" -b "$J" -X "$m" -H 'Content-Type: application/json' -d "$body" "$BASE$p"
  else
    curl -s -c "$J" -b "$J" -X "$m" "$BASE$p"
  fi
}

check() { # description expected actual
  if [ "$2" = "$3" ]; then echo "PASS: $1"; PASS=$((PASS+1));
  else echo "FAIL: $1 (expected [$2] got [$3])"; FAIL=$((FAIL+1)); fi
}

echo "== 登录"
req POST /login "username=admin&password=admin123" >/dev/null

echo "== 1. 登记养殖场"
R=$(req POST /api/farms '{"farmName":"东山养猪场","ownerName":"张三","phone":"13800001111","address":"东山脚下1号","species":"PIG","stockQty":500}')
echo "$R" | jq .
FID=$(echo "$R" | jq -r '.data.id')
FNO=$(echo "$R" | jq -r '.data.farmNo')
check "新场默认 ACTIVE" "ACTIVE" "$(echo "$R" | jq -r '.data.status')"
check "种类带出 PIG" "PIG" "$(echo "$R" | jq -r '.data.species')"
check "场编号形如 FM-2026-xxxx" "$(echo "$FNO" | grep -Ec '^FM-[0-9]{4}-[0-9]{4}$')" "1"

echo "== 2. 登记第二、三个场（验顺序号递增）"
R2=$(req POST /api/farms '{"farmName":"西坡牛场","species":"CATTLE","stockQty":30}')
echo "$R2" | jq -c '.data | {farmNo,farmName,species,status}'
FID2=$(echo "$R2" | jq -r '.data.id')
check "第二号在第一号之后递增" "1" "$( [ "$(echo "$R2" | jq -r '.data.farmNo')" \> "$FNO" ] && echo 1 )"
req POST /api/farms '{"farmName":"南湖羊场","species":"SHEEP","stockQty":80}' >/dev/null
req POST /api/farms '{"farmName":"北苑禽场","species":"POULTRY","stockQty":2000}' >/dev/null

echo "== 3. 改档案（只改电话，其它保持）"
R=$(req PUT "/api/farms/$FID" '{"phone":"13900002222"}')
check "电话改了" "13900002222" "$(echo "$R" | jq -r '.data.phone')"
check "场名没被抹掉" "东山养猪场" "$(echo "$R" | jq -r '.data.farmName')"
check "存栏没被抹掉" "500" "$(echo "$R" | jq -r '.data.stockQty')"

echo "== 4. 状态变更（停业）"
R=$(req PUT "/api/farms/$FID/status" '{"status":"SUSPENDED"}')
check "状态变 SUSPENDED" "SUSPENDED" "$(echo "$R" | jq -r '.data.status')"
R=$(req PUT "/api/farms/$FID/status" '{"status":"ACTIVE"}')
check "状态可回 ACTIVE" "ACTIVE" "$(echo "$R" | jq -r '.data.status')"

echo "== 5. 组合查询 + 空条件分页"
R=$(req GET '/api/farms?species=PIG')
check "按种类查到东山" "东山养猪场" "$(echo "$R" | jq -r '.data.content[0].farmName')"
R=$(req GET '/api/farms?farmName=%E7%8C%AA')
check "按场名模糊查" "东山养猪场" "$(echo "$R" | jq -r '.data.content[0].farmName')"
R=$(req GET '/api/farms?status=ACTIVE&species=PIG')
check "种类+状态组合" "1" "$(echo "$R" | jq -r '.data.total>=1')"
R=$(req GET '/api/farms?pageNum=1&pageSize=2')
check "分页第1页2条" "2" "$(echo "$R" | jq -r '.data.content|length')"
check "total=4" "4" "$(echo "$R" | jq -r '.data.total')"
IDS_P1=$(echo "$R" | jq -c '[.data.content[].id]')
R=$(req GET '/api/farms?pageNum=2&pageSize=2')
IDS_P2=$(echo "$R" | jq -c '[.data.content[].id]')
check "第2页2条" "2" "$(echo "$R" | jq -r '.data.content|length')"
OVERLAP=$(jq -nr --argjson a "$IDS_P1" --argjson b "$IDS_P2" '($a-$b|length)+($b-$a|length) == ($a|length)+($b|length)')
check "两页不重行" "true" "$OVERLAP"
R=$(req GET '/api/farms?pageNum=99&pageSize=2')
check "越界页空" "0" "$(echo "$R" | jq -r '.data.content|length')"

echo "== 6. 参数校验"
R=$(req POST /api/farms '{"farmName":"","species":"PIG"}')
check "空场名被拒" "1" "$(echo "$R" | jq -r '.code != 0')"
R=$(req POST /api/farms '{"farmName":"x场","species":"DRAGON"}')
check "非法种类被拒" "1" "$(echo "$R" | jq -r '.code != 0')"
R=$(req POST /api/farms '{"farmName":"y场","species":"PIG","stockQty":-3}')
check "负存栏被拒" "1" "$(echo "$R" | jq -r '.code != 0')"

echo "== 7. 耳标登记（种类随场走）"
R=$(req POST /api/ear-tags "{\"farmId\":$FID}")
echo "$R" | jq -c '.data | {tagNo,farmNo,species,status,issuedAt}'
TID=$(echo "$R" | jq -r '.data.id')
check "耳标默认 ISSUED" "ISSUED" "$(echo "$R" | jq -r '.data.status')"
check "耳标种类随场 PIG" "PIG" "$(echo "$R" | jq -r '.data.species')"
check "带出所属场编号" "$FNO" "$(echo "$R" | jq -r '.data.farmNo')"
check "耳标号形如 ET-2026-xxxxxx" "1" "$(echo "$R" | jq -r '.data.tagNo' | grep -Ec '^ET-[0-9]{4}-[0-9]{6}$')"
R=$(req POST /api/ear-tags "{\"farmId\":$FID2,\"wornAt\":\"2026-09-01 09:30:00\"}")
TID2=$(echo "$R" | jq -r '.data.id')
check "带佩戴时刻登记即 USED" "USED" "$(echo "$R" | jq -r '.data.status')"
check "牛场耳标种类 CATTLE" "CATTLE" "$(echo "$R" | jq -r '.data.species')"
check "佩戴时刻写入" "2026-09-01 09:30:00" "$(echo "$R" | jq -r '.data.wornAt')"

echo "== 8. 耳标改挂场（种类随新场）"
R=$(req PUT "/api/ear-tags/$TID" "{\"farmId\":$FID2}")
check "改挂到牛场 farmId" "$FID2" "$(echo "$R" | jq -r '.data.farmId')"
check "种类随新场变 CATTLE" "CATTLE" "$(echo "$R" | jq -r '.data.species')"

echo "== 9. 耳标状态流转"
R=$(req PUT "/api/ear-tags/$TID" '{"status":"USED","wornAt":"2026-09-20 10:00:00"}')
check "转 USED" "USED" "$(echo "$R" | jq -r '.data.status')"
check "USED 有佩戴时刻" "2026-09-20 10:00:00" "$(echo "$R" | jq -r '.data.wornAt')"
R=$(req PUT "/api/ear-tags/$TID" '{"status":"LOST"}')
check "转 LOST" "LOST" "$(echo "$R" | jq -r '.data.status')"
check "离开 USED 清佩戴时刻" "null" "$(echo "$R" | jq -r '.data.wornAt')"

echo "== 10. 耳标查询"
R=$(req GET "/api/ear-tags?farmId=$FID2")
check "按场查耳标总数>=2" "1" "$(echo "$R" | jq -r '.data.total>=2')"
check "每行带耳标号" "1" "$(echo "$R" | jq -r '[.data.content[]|select(.tagNo!=null)]|length==.data.content|length')"
R=$(req GET '/api/ear-tags?status=USED')
check "按状态 USED 查到" "$TID2" "$(echo "$R" | jq -r '.data.content[0].id')"
R=$(req GET '/api/ear-tags?species=CATTLE&status=USED')
check "种类+状态组合" "1" "$(echo "$R" | jq -r '.data.total>=1')"

echo "== 11. 耳标挂不存在的场"
R=$(req POST /api/ear-tags '{"farmId":999999999999}')
check "拒绝并给业务提示" "1" "$(echo "$R" | jq -r '.code != 0')"

echo "== 12. 销场（软删，名单消失）"
R=$(req DELETE "/api/farms/$FID2")
check "销场成功" "0" "$(echo "$R" | jq -r '.code')"
R=$(req GET "/api/farms/$FID2")
check "再查不到" "1" "$(echo "$R" | jq -r '.code != 0')"
R=$(req GET '/api/farms')
check "名单少一条(total=3)" "3" "$(echo "$R" | jq -r '.data.total')"
check "名单里没有已销场" "0" "$(echo "$R" | jq -r "[.data.content[]|select(.id==$FID2)]|length")"
R=$(req DELETE "/api/ear-tags/$TID2")
check "销耳标成功" "0" "$(echo "$R" | jq -r '.code')"
R=$(req GET '/api/ear-tags')
check "耳标名单少一条" "1" "$(echo "$R" | jq -r '[.data.content[]|select(.id=='"$TID2"')]|length==0')"

echo
echo "==== PASS=$PASS FAIL=$FAIL ===="
[ "$FAIL" -eq 0 ]
