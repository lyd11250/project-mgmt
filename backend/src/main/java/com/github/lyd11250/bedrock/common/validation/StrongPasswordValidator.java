package com.github.lyd11250.bedrock.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link StrongPassword} 的校验实现（G4 初步密码策略）。
 *
 * <p>规则（常量集中于此，后续可下沉到 application.yml 配置）：
 * <ul>
 *   <li>长度 8-64；</li>
 *   <li>至少同时包含字母与数字（拒绝纯数字、纯字母）；</li>
 *   <li>不含任何空白字符。</li>
 * </ul>
 *
 * <p>空值不在此处拦截（交由 {@code @NotBlank} 负责），避免重复报错。
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    /** 最小长度。 */
    private static final int MIN_LENGTH = 8;
    /** 最大长度。 */
    private static final int MAX_LENGTH = 64;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 空值放行：是否必填由 @NotBlank 决定
        if (value == null || value.isEmpty()) {
            return true;
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c)) {
                return false;
            }
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }
}
