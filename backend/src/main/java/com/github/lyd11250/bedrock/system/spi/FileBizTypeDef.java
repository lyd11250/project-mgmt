package com.github.lyd11250.bedrock.system.spi;

/**
 * 文件业务类型定义：{@code value} 为存入 {@code sys_file.biz_type} 的技术串，{@code label} 为前端展示的中文名。
 *
 * <p>技术串三段式 {@code 模块:资源:文件用途}（如 {@code system:user:avatar}=用户头像、
 * {@code party:person:idcard}=人员身份证）。第三段表「这是什么文件」（用途/类别），
 * 区别于权限码 {@code 模块:资源:动作} 的第三段（操作动作）。
 *
 * <p>由各模块通过 {@link FileBizTypeProvider} 自行登记；基座只聚合，不内置任何业务标签。
 */
public record FileBizTypeDef(String value, String label) {
}
