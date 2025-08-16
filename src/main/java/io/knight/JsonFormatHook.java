package io.knight;

import org.apache.jmeter.gui.action.Load;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;

/**
 * JSON格式化钩子类
 * 实现JMX文件加载时的自动格式化功能
 * 通过自定义加载器实现JSON参数的自动美化
 */
public class JsonFormatHook {

    /**
     * 安装钩子方法
     * 创建自定义加载器实例并设置到JMeter中
     * 实现JMX文件加载时的自动JSON格式化
     */
    public static void installHook() {
        try {
            // 获取JMeter主类
            Class<?> jmeterClass = Class.forName("org.apache.jmeter.JMeter");
            
            // 获取设置自定义加载器的方法
            Method setLoadMethod = jmeterClass.getMethod("setLoad", Load.class);
            
            // 创建自定义加载器实例
            Load customLoad = new Load() {
                
                protected void loadFile(File file) {
                    try {
                        if (file != null && file.getName().endsWith(".jmx")) {
                            // 读取JMX文件内容
                            String jmxContent = readJMXFile(file);
                            
                            try {
                                // 格式化JMX中的JSON参数
                                String formattedContent = JMXJsonFormatter.formatJMX(jmxContent);
                                
                                // 写入临时文件
                                File tempFile = writeTempJMXFile(formattedContent);
                                
                                // 调用父类方法加载文件
                                Method loadFileMethod = Load.class.getMethod("loadFile", File.class);
                                loadFileMethod.invoke(this, tempFile);
                                
                                // 删除临时文件
                                if (tempFile.exists()) {
                                    tempFile.delete();
                                }
                            } catch (Exception ex) {
                                // 格式化失败时直接加载原始文件
                                Method loadFileMethod = Load.class.getMethod("loadFile", File.class);
                                loadFileMethod.invoke(this, file);
                            }
                        }
                    } catch (Exception ex) {
                        // 显示错误信息
                        showErrorMessage("Failed to process JMX file: " + ex.getMessage());
                    }
                }
                
                /**
                 * 读取JMX文件内容
                 * @param file 文件对象
                 * @return 文件内容字符串
                 * @throws IOException 读取文件失败
                 */
                private String readJMXFile(File file) throws IOException {
                    StringBuilder content = new StringBuilder();
                    try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
                        char[] buffer = new char[1024];
                        int read;
                        while ((read = reader.read(buffer)) > 0) {
                            content.append(buffer, 0, read);
                        }
                    }
                    return content.toString();
                }
                
                /**
                 * 写入临时JMX文件
                 * @param content 文件内容
                 * @return 临时文件对象
                 * @throws IOException 写入文件失败
                 */
                private File writeTempJMXFile(String content) throws IOException {
                    File tempFile = File.createTempFile("jmx_", ".jmx");
                    try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8")) {
                        writer.write(content);
                    }
                    return tempFile;
                }
                
                /**
                 * 显示错误信息
                 * @param message 错误消息
                 */
                private void showErrorMessage(String message) {
                    try {
                        // 使用反射调用JMeter的错误显示方法
                        Class<?> jmeterUtilsClass = Class.forName("org.apache.jmeter.gui.util.JMeterUtils");
                        Method reportErrorMethod = jmeterUtilsClass.getMethod("reportErrorToUser", String.class, String.class);
                        reportErrorMethod.invoke(null, "JSON格式化错误", message);
                    } catch (Exception e) {
                        // 备用错误显示方式
                        System.err.println("JSON格式化错误: " + message);
                    }
                }
            };
            
            // 设置自定义加载器
            setLoadMethod.invoke(null, customLoad);
            
        } catch (Exception e) {
            System.err.println("安装钩子失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}