package io.github.shangor.util;

import org.springframework.ai.model.Media;
import org.springframework.http.MediaType;


public class IntegrationUtils {
    private IntegrationUtils() {}

    public static Media convertDataUrlToMedia(String dataUrl) {
        // 解析 data URL
        String[] parts = dataUrl.split(",");
        String metaData = parts[0]; // 例如 "data:image/png;base64"
        String base64Data = parts[1]; // 实际的 base64 数据

        // 解析 MIME 类型
        String mimeType = metaData.split(";")[0].split(":")[1]; // 例如 "image/png"

        // 将 base64 数据解码为字节数组
        byte[] data = java.util.Base64.getDecoder().decode(base64Data);

        // 创建 Media 对象
        return Media.builder().data(data).mimeType(MediaType.parseMediaType(mimeType)).build();
    }
}
