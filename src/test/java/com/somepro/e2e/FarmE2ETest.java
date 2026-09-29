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
 * 养殖场档案端到端：登记/改/查组合条件/分页稳定/校验/状态/注销。
 * 底账 data-h2.sql 里已有 3 条在场（FM-2026-0001/2/3）+ 1 条已软删（FM-2026-0007）。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FarmE2ETest extends AbstractE2ETest {

    private static long newFarmId;
    private static String newFarmNo;

    @Test
    @Order(1)
    void unauthenticatedIsRejected() {
        org.springframework.test.web.reactive.server.WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .get().uri("/api/farms")
                .exchange()
                .expectStatus().value(status ->
                        assertTrue(status == 401 || (status >= 300 && status < 400),
                                "未登录应被拦截，实际 " + status));
    }

    @Test
    @Order(2)
    void listShouldReadExistingLedgerAndHideSoftDeleted() {
        JsonNode page = list("/api/farms?pageNum=1&pageSize=50");
        // 底账 3 条在场；已软删的 FM-2026-0007 不出现
        assertEquals(3, page.path("total").asInt());
        boolean contains0001 = false;
        boolean containsSoftDeleted0007 = false;
        for (JsonNode row : page.path("content")) {
            if ("FM-2026-0001".equals(row.path("farmNo").asText())) {
                contains0001 = true;
            }
            if ("FM-2026-0007".equals(row.path("farmNo").asText())) {
                containsSoftDeleted0007 = true;
            }
            // 每行带场编号，方便对纸质台账
            assertTrue(row.path("farmNo").asText().startsWith("FM-"));
            // VO 白名单：不外泄 delFlag/审计人
            assertTrue(row.path("delFlag").isMissingNode());
            assertTrue(row.path("createBy").isMissingNode());
        }
        assertTrue(contains0001, "早先录入的底账必须读出来");
        assertTrue(!containsSoftDeleted0007, "软删场不该从名单里翻出来");
    }

    @Test
    @Order(3)
    void registerShouldAllocateNextNoAfterExistingMaxIncludingSoftDeleted() {
        String body = """
                {"farmName":"北岗家禽场","ownerName":"周七","phone":"13700004444",
                 "address":"北岗4号","species":"POULTRY","stockQty":1200}
                """;
        JsonNode vo = postOk("/api/farms", body).path("data");
        newFarmId = vo.path("id").asLong();
        newFarmNo = vo.path("farmNo").asText();
        // 已占用最大号是软删的 FM-2026-0007，下一个应是 0008，证明不回收、不撞号
        assertEquals("FM-2026-0008", newFarmNo);
        assertEquals("ACTIVE", vo.path("status").asText(), "新立默认在用");
        assertEquals("POULTRY", vo.path("species").asText());
        assertEquals("在用", vo.path("statusLabel").asText());
        assertNotNull(vo.path("createTime").asText(null));
    }

    @Test
    @Order(4)
    void updateShouldPartiallyChangeOnlyProvidedFields() {
        JsonNode vo = putOk("/api/farms/" + newFarmId,
                "{\"phone\":\"13700005555\"}").path("data");
        assertEquals("13700005555", vo.path("phone").asText());
        assertEquals("北岗家禽场", vo.path("farmName").asText(), "没传的场名不能被抹掉");
        assertEquals("北岗4号", vo.path("address").asText(), "没传的场址不能被抹掉");
        assertEquals(1200, vo.path("stockQty").asInt(), "没传的存栏不能被抹掉");
        assertEquals("POULTRY", vo.path("species").asText(), "种类不可改");
    }

    @Test
    @Order(5)
    void changeStatusAndCombinedQueriesAndStablePaging() {
        // 停业再复业（状态走专用 /status 端点）
        assertEquals("SUSPENDED", putOk("/api/farms/" + newFarmId + "/status",
                "{\"status\":\"SUSPENDED\"}").path("data").path("status").asText());
        assertEquals("ACTIVE", putOk("/api/farms/" + newFarmId + "/status",
                "{\"status\":\"ACTIVE\"}").path("data").path("status").asText());

        // 组合条件
        assertEquals(1, list("/api/farms?species=PIG").path("total").asInt());
        // WebTestClient 会自动做 URL 编码，这里直接给原始中文（手工再编码会双重转义）
        assertEquals(1, list("/api/farms?farmName=养猪").path("total").asInt(),
                "按场名模糊查：养猪");
        assertEquals(1, list("/api/farms?farmNo=FM-2026-0002").path("total").asInt());
        assertEquals(1, list("/api/farms?status=SUSPENDED&species=SHEEP").path("total").asInt(),
                "状态+种类组合");
        assertEquals(0, list("/api/farms?status=CLOSED").path("total").asInt(),
                "软删场不是 CLOSED 状态过滤能翻出来的");

        // 分页稳定不重行（4 条在场，每页 2）
        JsonNode p1 = list("/api/farms?pageNum=1&pageSize=2");
        JsonNode p2 = list("/api/farms?pageNum=2&pageSize=2");
        assertEquals(4, p1.path("total").asInt());
        assertEquals(2, p1.path("totalPages").asInt());
        assertEquals(2, p1.path("content").size());
        assertEquals(2, p2.path("content").size());
        for (JsonNode a : p1.path("content")) {
            for (JsonNode b : p2.path("content")) {
                assertTrue(a.path("id").asLong() != b.path("id").asLong(), "两页之间不能重行");
            }
        }
        assertEquals(0, list("/api/farms?pageNum=9&pageSize=2").path("content").size(), "越界页为空");
    }

    @Test
    @Order(6)
    void validationShouldRejectBadInput() {
        assertEquals(1, postFail("/api/farms", "{\"farmName\":\"\",\"species\":\"PIG\"}"),
                "空场名拒绝");
        assertEquals(1, postFail("/api/farms", "{\"farmName\":\"x场\",\"species\":\"DRAGON\"}"),
                "非法种类拒绝");
        assertEquals(1, postFail("/api/farms",
                "{\"farmName\":\"x场\",\"species\":\"PIG\",\"stockQty\":-1}"), "负存栏拒绝");
        assertEquals(1, putFail("/api/farms/" + newFarmId + "/status",
                "{\"status\":\"WAIT\"}"), "非法状态拒绝");
    }

    @Test
    @Order(7)
    void closeShouldSoftDeleteAndDisappearFromList() {
        // 看单条
        assertEquals("FM-2026-0008", getOk("/api/farms/" + newFarmId)
                .path("data").path("farmNo").asText());

        assertEquals(0, client.delete().uri("/api/farms/" + newFarmId)
                .exchange().expectBody(JsonNode.class).returnResult().getResponseBody()
                .path("code").asInt());

        assertEquals(1, client.get().uri("/api/farms/" + newFarmId)
                .exchange().expectBody(JsonNode.class).returnResult().getResponseBody()
                .path("code").asInt(), "销后再查应报业务错");

        assertEquals(3, list("/api/farms").path("total").asInt(), "名单回到 3 条");
    }

    // ---------- helpers ----------

    private JsonNode list(String uri) {
        return getOk(uri).path("data");
    }

    private JsonNode getOk(String uri) {
        return client.get().uri(uri)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
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

    private int postFail(String uri, String json) {
        return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody()
                .path("code").asInt();
    }

    private int putFail(String uri, String json) {
        return client.put().uri(uri).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody()
                .path("code").asInt();
    }
}
