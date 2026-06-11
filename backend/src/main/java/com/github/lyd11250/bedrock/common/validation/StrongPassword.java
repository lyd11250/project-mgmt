package com.github.lyd11250.bedrock.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 密码强度校验（G4 初步）。
 * 规则见 {@link StrongPasswordValidator}：长度 8-64，至少含字母与数字，不含空白。
 * 用于所有「设置密码」入口（新建用户、重置密码、自助改密）。
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "密码需 8-64 位，且同时包含字母和数字，不含空格";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
