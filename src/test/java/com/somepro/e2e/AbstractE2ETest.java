package com.somepro.e2e;

import com.somepro.SomeProApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import java.time.Duration;

/**
 * 端到端集成测试基类：真实起 WebFlux 服务 + H2(MySQL 兼容模式) + MyBatis-Plus + PageHelper，
 * 用 admin 登录后的 SESSION cookie 打真实 HTTP 链路。
 *
 * 底账来自 data-h2.sql，模拟“库里已有早先录入数据”（含已软删行）。
 *
 * 数据源用 @SpringBootTest properties 钉死 —— 它的优先级高于 OS 环境变量，
 * 避免本机/容器里存在 DB_URL、DB_USERNAME 等环境变量时把测试数据源顶掉。
 */
@ActiveProfiles("h2test")
@SpringBootTest(classes = SomeProApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:quarantine;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:schema-h2.sql",
                "spring.sql.init.data-locations=classpath:data-h2.sql"
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractE2ETest {

    protected WebTestClient client;

    @LocalServerPort
    protected int port;

    @BeforeAll
    void login() {
        // 先用一个临时 client 登录取 SESSION，再构建带默认 cookie 的 client
        WebTestClient unauth = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(30))
                .build();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", "admin");
        form.add("password", "admin123");

        EntityExchangeResult<byte[]> loginResult = unauth.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectBody().returnResult();

        String session = loginResult.getResponseCookies().getFirst("SESSION").getValue();

        this.client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultCookie("SESSION", session)
                .responseTimeout(Duration.ofSeconds(30))
                .build();
    }
}
