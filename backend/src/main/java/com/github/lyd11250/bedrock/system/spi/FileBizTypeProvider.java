package com.github.lyd11250.bedrock.system.spi;

import java.util.List;

/**
 * 文件业务类型 SPI：上层模块（含 system 自身）各实现一个 {@code @Component} 自报旗下 bizType 的中文标签。
 *
 * <p>基座 {@code FileBizTypeRegistry} 通过注入 {@code List<FileBizTypeProvider>} 聚合全部实现，
 * 据此把 {@code sys_file.biz_type} 技术串翻成中文。基座代码不出现任何具体业务标签，
 * 新增业务模块只需在自身包内追加一个本接口实现，基座零改动（接入规范第 0 节「基座纯净」）。
 */
public interface FileBizTypeProvider {

    /** 本模块登记的文件业务类型清单。 */
    List<FileBizTypeDef> bizTypes();
}
