package com.github.lyd11250.bedrock.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 定制（Spring Boot 4 Web 默认使用 Jackson 3）。
 *
 * <p>把 {@code Long} 序列化为字符串：雪花算法主键约 19 位，超过 JavaScript
 * {@code Number} 安全整数上限 {@code 2^53}，若以 JSON 数字下发会在前端丢精度，
 * 导致 id 回传时与库中不一致（表现为「角色/用户不存在」）。
 * 仅作用于包装类型 {@code Long}；原始 {@code long}（如分页 total）仍按数字输出。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer longToStringCustomizer() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        return builder -> builder.addModule(module);
    }
}
