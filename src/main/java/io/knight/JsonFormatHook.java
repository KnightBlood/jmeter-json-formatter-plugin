package io.knight;

import org.apache.jmeter.gui.action.Load;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class JsonFormatHook {

    public static void installHook() {
        // 创建自定义加载器实例
        Load customLoad = new Load() {
            
            protected void loadFile(File file) {
                try {
                    if (file != null && file.getName().endsWith(".jmx")) {
                        String jmxContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                        try {
                            jmxContent = JMXJsonFormatter.formatJMX(jmxContent);
                            
                            // 创建临时文件进行加载
                            File tempFile = File.createTempFile("jmx_", ".jmx");
                            Files.write(tempFile.toPath(), jmxContent.getBytes());
                            SaveService.loadTree(tempFile);
                        } catch (Exception ex) {
                            JMeterUtils.reportErrorToUser("Invalid JMX file format", ex.getMessage());
                        }
                    }
                } catch (IOException ex) {
                    JMeterUtils.reportErrorToUser("Failed to read JMX file", ex.getMessage());
                }
            }
        };
        
        // 设置自定义加载器（需要通过反射或配置实现）
        // 这里需要具体实现方式，取决于JMeter版本
    }
}