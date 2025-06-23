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

public class JMXJsonFormatter {

    public static String formatJMX(String jmxContent) throws Exception {
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
    }

    private static void processSampler(Element sampler) {
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