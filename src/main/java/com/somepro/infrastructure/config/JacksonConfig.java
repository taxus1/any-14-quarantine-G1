package com.somepro.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 对 JSR-310 时间类型的定制（基础设施层）。
 *
 * application.yml 里的 spring.jackson.date-format 只对 java.util.Date 生效，
 * LocalDateTime 默认会走 ISO-8601（带 'T'）。这里统一成台账习惯的 yyyy-MM-dd HH:mm:ss：
 * - 出参：所有 LocalDateTime 序列化为 yyyy-MM-dd HH:mm:ss（时区 GMT+8 已在 yml 指定）；
 * - 入参：优先按 yyyy-MM-dd HH:mm:ss 解析，同时兼容 ISO-8601（2026-09-29T10:00:00）与纯日期。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(DATE_TIME_FORMATTER))
                .deserializers(new LenientLocalDateTimeDeserializer());
    }

    /**
     * 宽松的 LocalDateTime 反序列化：台账格式不行再退 ISO-8601 / 纯日期。
     * 继承 StdDeserializer 以声明 handledType —— Spring 的 Jackson2ObjectMapperBuilder
     * 会拒绝“Unknown handled type”的裸 JsonDeserializer。
     */
    private static class LenientLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

        LenientLocalDateTimeDeserializer() {
            super(LocalDateTime.class);
        }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getValueAsString();
            if (text == null || text.isBlank()) {
                return null;
            }
            String value = text.trim();
            try {
                return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
            } catch (Exception ignored) {
                // 落到下面的兼容格式
            }
            try {
                return LocalDateTime.parse(value);
            } catch (Exception ignored) {
                // 落到纯日期
            }
            try {
                // 只给了日期（如 2026-09-29）时按当天 00:00:00
                return LocalDate.parse(value).atStartOfDay();
            } catch (Exception e) {
                return (LocalDateTime) ctxt.handleWeirdStringValue(
                        LocalDateTime.class, text,
                        "时间格式应为 yyyy-MM-dd HH:mm:ss 或 ISO-8601");
            }
        }
    }
}
