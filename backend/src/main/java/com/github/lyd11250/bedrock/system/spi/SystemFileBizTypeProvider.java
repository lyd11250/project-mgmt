package com.github.lyd11250.bedrock.system.spi;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * system 域自报旗下文件业务类型（如用户头像）。新增系统级 bizType 在此追加。
 */
@Component
public class SystemFileBizTypeProvider implements FileBizTypeProvider {

    @Override
    public List<FileBizTypeDef> bizTypes() {
        return List.of(
                new FileBizTypeDef("system:user:avatar", "用户头像")
        );
    }
}
