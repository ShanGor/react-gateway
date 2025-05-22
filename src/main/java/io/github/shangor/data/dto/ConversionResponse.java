package io.github.shangor.data.dto;

/**
 * 文件转换响应对象
 * 用于表示PDF转文本等转换操作的结果
 */
public record ConversionResponse(int statusCode, String message,  Object data) {
    
    /**
     * 创建成功响应
     * @return 成功响应对象
     */
    public static ConversionResponse success() {
        return new ConversionResponse(200, "Success", null);
    }
    /**
     * 创建成功响应
     * @param data 成功数据
     * @return 成功响应对象
     */
    public static ConversionResponse success(Object data) {
        return new ConversionResponse(200, "Success", data);
    }
    
    /**
     * 创建错误响应
     * @param code 错误码
     * @param message 错误信息
     * @return 错误响应对象
     */
    public static ConversionResponse error(int code, String message) {
        return new ConversionResponse(code, message,null);
    }
    
    /**
     * 判断是否成功
     * @return 如果状态码在200-299之间返回true，否则返回false
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
} 