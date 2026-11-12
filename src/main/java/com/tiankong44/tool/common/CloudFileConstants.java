package com.tiankong44.tool.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 公共云端文件的大小和媒体类型约束。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
public final class CloudFileConstants {
    /** 单个图片或动态片段的上传体积上限，与各运行环境 multipart 配置一致。 */
    public static final long MAX_FILE_BYTES = 10L * 1024L * 1024L;
    /** 共享文件服务接受的动态片段媒体类型白名单。 */
    public static final Set<String> VIDEO_CONTENT_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "video/mp4", "video/quicktime", "video/x-m4v", "video/3gpp")));

    /** 禁止实例化公共文件常量类。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    private CloudFileConstants() {
    }
}
