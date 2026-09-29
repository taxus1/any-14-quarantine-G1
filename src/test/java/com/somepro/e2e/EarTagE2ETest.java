package com.somepro.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 耳标端到端：登记（种类随场走）/改挂场/状态流转/组合查询/软删/续号。
 * 底账：ET-2026-000001（PIG 场 1001 ISSUED）、000002（CATTLE 场 1002 USED）、
 * 已软删 000004（最大号，用来验证新号续到 000005）。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EarTagE2ETest extends AbstractE2ETest {

    private static long newTagId;
    private static String newTagNo;

    @Test
    @Order(1)
    void listReadsExistingLedgerWithFarmNoFilledIn() {
        JsonNode page = list("/api/ear-tags?pageNum=1&pageSize=50");
        assertEquals(2, page.path("total").asInt(), "底账 2 条，软删的 000004 不出现");
        for (JsonNode row : page.path("content")) {
            assertTrue(row.path("tagNo").asText().startsWith("ET-"), "每行带耳标号");
            assertNotNull(row.path("farmNo").asText(null), "每行回填所属场编号");
            assertTrue(row.path("delFlag").isMissingNode(), "VO 不外泄 delFlag");
            // 种类与所属场一致
            String expected = row.path("farmNo").asText().equals("FM-2026-0001") ? "PIG" : "CATTLE";
            assertEquals(expected, row.path("species").asText());
        }
    }

    @Test
    @Order(2)
    void issueNewTagInheritsSpeciesFromFarmAndContinuesNumber() {
        JsonNode vo = postOk("/api/ear-tags", "{\"farmId\":1001}").path("data");
        newTagId = vo.path("id").asLong();
        newTagNo = vo.path("tagNo").asText();
        assertEquals("ET-2026-000005", newTagNo, "含软删最大号 000004，续 000005");
        assertEquals("PIG", vo.path("species").asText(), "种类照猪场走，不需要传");
        assertEquals("ISSUED", vo.path("status").asText(), "新发默认已发放待戴");
        assertEquals("FM-2026-0001", vo.path("farmNo").asText(), "带出所属场编号");
        assertNotNull(vo.path("issuedAt").asText(null), "发放时刻默认当前");
        assertTrue(vo.path("wornAt").isNull() || vo.path("wornAt").isMissingNode(),
                "未戴没有佩戴时刻（null 字段在 non_null 下不出现）");
    }

    @Test
    @Order(3)
    void issueWornTagGoesUsed() {
        JsonNode vo = postOk("/api/ear-tags",
                "{\"farmId\":1001,\"wornAt\":\"2026-09-01 09:30:00\"}").path("data");
        assertEquals("ET-2026-000006", vo.path("tagNo").asText());
        assertEquals("USED", vo.path("status").asText());
        assertEquals("2026-09-01 09:30:00", vo.path("wornAt").asText());
    }

    @Test
    @Order(4)
    void reattachToAnotherFarmChangesSpeciesAlong() {
        // 改挂到牛场 1002，种类应随新场变 CATTLE
        JsonNode vo = putOk("/api/ear-tags/" + newTagId, "{\"farmId\":1002}").path("data");
        assertEquals(1002, vo.path("farmId").asLong());
        assertEquals("CATTLE", vo.path("species").asText(), "耳标种类随新场走");
        assertEquals("FM-2026-0002", vo.path("farmNo").asText());
    }

    @Test
    @Order(5)
    void statusTransitionsKeepWornAtConsistent() {
        JsonNode vo = putOk("/api/ear-tags/" + newTagId,
                "{\"status\":\"USED\",\"wornAt\":\"2026-09-20 10:00:00\"}").path("data");
        assertEquals("USED", vo.path("status").asText());
        assertEquals("2026-09-20 10:00:00", vo.path("wornAt").asText());

        vo = putOk("/api/ear-tags/" + newTagId, "{\"status\":\"LOST\"}").path("data");
        assertEquals("LOST", vo.path("status").asText());
        assertTrue(vo.path("wornAt").isNull() || vo.path("wornAt").isMissingNode(),
                "离开 USED 必须清掉佩戴时刻");

        // 只传佩戴时刻不传状态 → 视为登记佩戴
        vo = putOk("/api/ear-tags/" + newTagId,
                "{\"wornAt\":\"2026-09-25 08:00:00\"}").path("data");
        assertEquals("USED", vo.path("status").asText());
        assertEquals("2026-09-25 08:00:00", vo.path("wornAt").asText());

        // USED 不传 wornAt：领域补发时间，不报错
        vo = putOk("/api/ear-tags/" + newTagId, "{\"status\":\"DISABLED\"}").path("data");
        assertEquals("DISABLED", vo.path("status").asText());
    }

    @Test
    @Order(6)
    void combinedQueries() {
        assertTrue(list("/api/ear-tags?farmId=1002").path("total").asInt() >= 2, "按场查");
        assertTrue(list("/api/ear-tags?status=USED").path("total").asInt() >= 2, "按状态查");
        assertTrue(list("/api/ear-tags?species=CATTLE&status=USED").path("total").asInt() >= 1,
                "种类+状态组合");
        assertEquals(1, list("/api/ear-tags?tagNo=ET-2026-000001").path("total").asInt(),
                "按耳标号精确查");

        // 分页稳定
        JsonNode p1 = list("/api/ear-tags?pageNum=1&pageSize=2");
        JsonNode p2 = list("/api/ear-tags?pageNum=2&pageSize=2");
        for (JsonNode a : p1.path("content")) {
            for (JsonNode b : p2.path("content")) {
                assertTrue(a.path("id").asLong() != b.path("id").asLong(), "两页不重行");
            }
        }
    }

    @Test
    @Order(7)
    void rejectIssueToMissingOrClosedFarm() {
        // 不存在
        int code = client.post().uri("/api/ear-tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"farmId\":999999}")
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody()
                .path("code").asInt();
        assertEquals(1, code, "挂不存在的场必须拒绝");

        // 缺 farmId
        code = client.post().uri("/api/ear-tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody()
                .path("code").asInt();
        assertEquals(1, code, "缺 farmId 必须拒绝");

        // 非法状态
        code = client.put().uri("/api/ear-tags/" + newTagId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"status\":\"GHOST\"}")
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody()
                .path("code").asInt();
        assertEquals(1, code, "非法状态拒绝");
    }

    @Test
    @Order(8)
    void deleteShouldSoftRemove() {
        assertEquals(0, client.delete().uri("/api/ear-tags/" + newTagId)
                .exchange().expectBody(JsonNode.class).returnResult().getResponseBody()
                .path("code").asInt());
        // 已删再删/再查 → 业务错
        assertEquals(1, client.get().uri("/api/ear-tags/" + newTagId)
                .exchange().expectBody(JsonNode.class).returnResult().getResponseBody()
                .path("code").asInt());
        // 名单里没有
        JsonNode page = list("/api/ear-tags?pageNum=1&pageSize=100");
        for (JsonNode row : page.path("content")) {
            assertTrue(row.path("id").asLong() != newTagId, "软删耳标不该出现在名单");
        }
    }

    // ---------- helpers ----------

    private JsonNode list(String uri) {
        return client.get().uri(uri)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody()
                .path("data");
    }

    private JsonNode postOk(String uri, String json) {
        return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    private JsonNode putOk(String uri, String json) {
        return client.put().uri(uri).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }
}
