package io.knight;

import org.apache.jmeter.engine.StandardJMeterEngine;

/**
 * JMeter插件工厂类
 * 用于创建和初始化插件实例
 */
public class JMeterPluginFactory {
    /**
     * 初始化插件
     * @param engine JMeter引擎实例
     */
    public void initialize(StandardJMeterEngine engine) {
        // 创建插件实例并初始化
        JMeterPlugin plugin = new JMeterPlugin();
        plugin.initialize();
    }

    /**
     * JMeter插件入口点
     * @param engine JMeter引擎实例
     */
    public static void init(StandardJMeterEngine engine) {
        new JMeterPluginFactory().initialize(engine);
    }
}