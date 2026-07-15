package com.zeta.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "zeta.upload")
public class UploadProperties {

    /** 上传文件根目录（相对或绝对路径） */
    private String storageDir = "./data/uploads";

    /** 对外访问 URL 前缀 */
    private String publicPathPrefix = "/uploads";

    /** 视频存储基准目录；留空时自动使用 JAR 所在目录 */
    private String videoStorageDir;

    /** 学习资料存储基准目录；留空时自动使用 JAR 所在目录 */
    private String resourceStorageDir;
}
