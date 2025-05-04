package io.github.shangor.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.ai.content.Media;

@Slf4j
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

    public static String convertMediaToDataUrl(Media media) {
        if (media == null) {
            return null;
        } else {
            String mimeType = media.getMimeType().toString();
            var obj = media.getData();
            if (obj instanceof byte[] data) {
                String base64Data = java.util.Base64.getEncoder().encodeToString(data);
                return "data:" + mimeType + ";base64," + base64Data;
            } else if (obj instanceof String data) {
                return "data:" + mimeType + ";base64," + data;
            } else {
                log.error("Unsupported media type: {}", media.getMimeType());
                return null;
            }
        }
    }
}
