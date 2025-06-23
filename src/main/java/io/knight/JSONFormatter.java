package io.knight;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

/**
 * JSON格式化工具类
 * 提供JSON字符串的美化功能
 */
public class JSONFormatter {

    /**
     * 格式化JSON字符串
     * @param jsonString 需要格式化的JSON字符串
     * @return 格式化后的JSON字符串，如果格式化失败返回原字符串
     */
    public static String formatJSON(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return jsonString;
        }
        
        try {
            // 使用FastJSON2进行格式化
            Object jsonObject = JSON.parseObject(jsonString);
            return JSON.toJSONString(jsonObject, JSONWriter.Feature.PrettyFormat);
        } catch (Exception e) {
            // 如果解析失败，返回原始内容
            return jsonString;
        }
    }
}