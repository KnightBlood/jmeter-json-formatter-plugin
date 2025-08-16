package io.knight;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

/**
 * JSON格式化工具类
 * 提供JSON字符串的美化功能
 */
public class JSONFormatter {

    /**
     * 格式化JSON字符串（支持JSON对象和数组格式）
     * @param jsonString 需要格式化的JSON字符串
     * @return 格式化后的JSON字符串，如果格式化失败返回原字符串
     * @note FastJSON2的parse()方法可处理数组格式（如[{\"aaa\":1},{\"aaa\":2}]），
     *       parseObject()仅支持对象格式（{key:value}结构）
     */
    public static String formatJSON(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return jsonString;
        }
        
        try {
            // 使用FastJSON2的通用解析方法（JSON.parse）支持数组格式
            // parseObject()仅支持对象格式（{key:value}），会导致数组格式解析失败
            Object jsonObject = JSON.parse(jsonString);
            return JSON.toJSONString(jsonObject, new JSONWriter.Feature[] {
                    JSONWriter.Feature.WriteMapNullValue,
                    JSONWriter.Feature.WriteNullListAsEmpty,
                    JSONWriter.Feature.PrettyFormat
            });
        } catch (Exception e) {
            // 日志记录可选：
            // LoggerFactory.getLogger(JSONFormatter.class).warn("JSON格式化失败", e);
            // 解析失败返回原始内容，保证程序健壮性
            return jsonString;
        }
    }
}