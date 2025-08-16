package io.knight;

import org.apache.jmeter.save.SaveService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * JMX文件处理器
 * 提供JMX文件中JSON参数的格式化功能
 * 支持HTTP请求参数的JSON内容美化显示
 * 
 * 该类主要用于处理JMeter的JMX文件，识别其中的JSON参数内容并进行格式化，
 * 使JSON数据具有更好的可读性，便于测试人员查看和调试接口测试脚本。
 */

public class JMXJsonFormatter {

    public static String formatJMX(String jmxContent) throws Exception {
        /**
         * 格式化JMX文件中的JSON参数
         * 
         * 处理流程：
         * 1. 验证输入参数
         * 2. 解析XML文档
         * 3. 查找所有HTTP请求元素
         * 4. 处理每个HTTP请求的参数
         * 5. 将修改后的文档转换回格式化的XML字符串
         * 
         * @param jmxContent JMX文件的原始XML内容
         * @return 格式化后的XML内容，如果格式化失败返回原内容
         * @throws Exception 解析XML或格式化JSON失败时抛出异常
         */
        
        if (jmxContent == null || jmxContent.trim().isEmpty()) {
            return jmxContent;
        }

        try {
            // 解析XML文档
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream stream = new ByteArrayInputStream(jmxContent.getBytes("UTF-8"));
            Document doc = builder.parse(stream);

            // 查找所有HTTP请求
            NodeList httpSamplers = doc.getElementsByTagName("HTTPSamplerProxy");
            for (int i = 0; i < httpSamplers.getLength(); i++) {
                Element sampler = (Element) httpSamplers.item(i);
                processSampler(sampler);
            }

            // 转换回XML字符串
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            // 解析失败返回原始内容
            return jmxContent;
        }
    }

    private static void processSampler(Element sampler) {
        /**
         * 处理单个HTTP采样器，格式化其请求体中的JSON参数
         * 
         * 处理逻辑：
         * 1. 查找所有stringProp元素
         * 2. 识别HTTP请求参数部分
         * 3. 遍历参数列表
         * 4. 提取参数值中的JSON内容
         * 5. 对JSON内容进行格式化
         * 6. 将格式化后的内容写回参数值
         * 
         * @param sampler HTTP采样器元素
         */
        
        if (sampler == null) {
            return;
        }

        // 查找请求体元素
        NodeList stringProps = sampler.getElementsByTagName("stringProp");
        for (int j = 0; j < stringProps.getLength(); j++) {
            Element prop = (Element) stringProps.item(j);
            String propName = prop.getAttribute("name");
            // 检查是否为请求体属性
            if ("HTTPSampler.arguments".equals(propName)) {
                Element arguments = (Element) prop.getParentNode();
                NodeList children = arguments.getElementsByTagName("elementProp");
                for (int k = 0; k < children.getLength(); k++) {
                    Element arg = (Element) children.item(k);
                    Element nameProp = (Element) arg.getElementsByTagName("stringProp").item(0);
                    if (nameProp != null && "Argument.name".equals(nameProp.getAttribute("name")) && nameProp.getTextContent().isEmpty()) {
                        Element valueProp = (Element) arg.getElementsByTagName("stringProp").item(1);
                        if (valueProp != null && "Argument.value".equals(valueProp.getAttribute("name"))) {
                            String jsonBody = valueProp.getTextContent();
                            String formatted = JSONFormatter.formatJSON(jsonBody);
                            if (!formatted.equals(jsonBody)) {
                                valueProp.setTextContent(formatted);
                            }
                        }
                    }
                }
            }
        }
    }
}