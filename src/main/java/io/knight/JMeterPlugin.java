package io.knight;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeModel;
import java.util.Enumeration;
import java.lang.reflect.Method;
import org.apache.jmeter.gui.plugin.MenuCreator;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;

/**
 * JMeter插件主类，提供JSON格式化功能
 * 该插件在JMeter的工具菜单中添加JSON格式化选项，
 * 可对HTTP请求中的JSON请求体进行格式化操作
 */
public class JMeterPlugin implements MenuCreator {
    /**
     * 初始化插件功能
     * 包括安装钩子和添加菜单项
     */
    public void initialize() {
        // 初始化JSON格式化钩子
        JsonFormatHook.installHook();
        
        // 通过反射注册菜单创建者
        try {
            Class<?> pluginManagerClass = Class.forName("org.apache.jmeter.gui.plugin.JMeterPluginManager");
            Method registerMenuCreatorMethod = pluginManagerClass.getMethod("registerMenuCreator", MenuCreator.class);
            registerMenuCreatorMethod.invoke(null, this);
        } catch (Exception e) {
            System.err.println("菜单创建者注册失败: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 获取JMeter主窗口的工具菜单
     * 使用反射机制获取主窗口实例和菜单栏
     * @return 工具菜单对象，如果找不到则返回null
     */
    private JMenu getToolMenu() {
        try {
            // 获取 JMeter 主窗口实例
            Object gui = Class.forName("org.apache.jmeter.gui.MainFrame").getMethod("getInstance").invoke(null);
            if (gui == null) {
                System.err.println("无法获取 JMeter 主窗口实例");
                return null;
            }

            // 获取菜单栏
            JMenuBar menuBar = (JMenuBar) gui.getClass().getMethod("getJMenuBar").invoke(gui);
            if (menuBar == null) {
                System.err.println("无法获取菜单栏");
                return null;
            }

            // 动态获取工具菜单名称
            String toolMenuName = org.apache.jmeter.util.JMeterUtils.getResString("menu_tools");

            // 遍历菜单项，查找工具菜单
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu != null && toolMenuName.equals(menu.getText())) {
                    return menu;
                }
            }

            System.err.println("未找到工具菜单");
        } catch (Exception ex) {
            System.err.println("获取工具菜单时发生错误: " + ex.getMessage());
            ex.printStackTrace();
        }

        return null;
    }

    private void formatJsonRequests() {
        //System.out.println("格式化JSON请求");
        try {
            // 获取GuiPackage实例
            Object guiPackage = Class.forName("org.apache.jmeter.gui.GuiPackage").getMethod("getInstance").invoke(null);
            if (guiPackage == null) {
                System.err.println("无法获取GuiPackage实例");
                return;
            }
            
            // 获取树模型
            Object treeModel = guiPackage.getClass().getMethod("getTreeModel").invoke(guiPackage);
            if (treeModel == null) {
                System.err.println("无法获取树模型");
                return;
            }
            
            // 获取根节点
            Method getRootMethod = treeModel.getClass().getMethod("getRoot");
            Object rootNode = getRootMethod.invoke(treeModel);
            
            // 创建深度优先遍历枚举器
            Method depthFirstEnumerationMethod = rootNode.getClass().getMethod("depthFirstEnumeration");
            Enumeration<?> nodes = (Enumeration<?>) depthFirstEnumerationMethod.invoke(rootNode);
            
            while (nodes.hasMoreElements()) {
                Object node = nodes.nextElement();
                
                // 检查是否为JMeterTreeNode
                if (node != null && Class.forName("org.apache.jmeter.gui.tree.JMeterTreeNode").isAssignableFrom(node.getClass())) {
                    // 获取测试元素
                    Method getTestElementMethod = node.getClass().getMethod("getTestElement");
                    Object element = getTestElementMethod.invoke(node);
                    
                    if (isSamplerWithArguments(element)) {
                        try {
                            // 使用反射获取getArguments方法
                            Method getArgumentsMethod = element.getClass().getMethod("getArguments");
                            // 获取参数集合
                            Object arguments = getArgumentsMethod.invoke(element);
                            //System.out.println("获取到参数集合: " + arguments.getClass().getName());

                            // 检查参数是否为空
                            Method iteratorMethod = arguments.getClass().getMethod("iterator");
                            Object rawIterator = iteratorMethod.invoke(arguments);
                            boolean hasElements = (Boolean) rawIterator.getClass().getMethod("hasNext").invoke(rawIterator);
                            //System.out.println("参数集合是否为空: " + !hasElements);
                            
                            if (hasElements) {
                                // 创建新的参数集合用于替换
                                CollectionProperty newArgs = new CollectionProperty();
                                boolean modified = false;
                                
                                // 重置迭代器
                                rawIterator = iteratorMethod.invoke(arguments);
                                int argCount = 0;
                                while ((Boolean) rawIterator.getClass().getMethod("hasNext").invoke(rawIterator)) {
                                    Object arg = rawIterator.getClass().getMethod("next").invoke(rawIterator);
                                    argCount++;
                                    //System.out.println("处理第" + argCount + "个参数, 类型: " + arg.getClass().getName());
                                    
                                    // 检查是否为HTTP参数
                                    if (arg.getClass().getSimpleName().endsWith("Argument")) {
                                        String body = (String) arg.getClass().getMethod("getValue").invoke(arg);
                                        //System.out.println("原始参数值: " + (body != null ? body.substring(0, Math.min(50, body.length())) : "null"));
                                        
                                        if (body != null && (body.trim().startsWith("{") || body.trim().startsWith("["))) {
                                            String formattedJson = JSONFormatter.formatJSON(body);
                                            if (!formattedJson.equals(body)) {
                                                // 创建新参数并添加到集合
                                                Object newArg = arg.getClass().getConstructor().newInstance();
                                                newArg.getClass().getMethod("setValue", String.class).invoke(newArg, formattedJson);
                                                
                                                // 将新参数转换为JMeterProperty类型
                                                Class<?> jmeterPropertyClass = Class.forName("org.apache.jmeter.testelement.property.JMeterProperty");
                                                Method castMethod = jmeterPropertyClass.getMethod("getProperty", Object.class);
                                                Object jmeterProp = castMethod.invoke(null, newArg);
                                                
                                                // 强制类型转换
                                                if (jmeterProp instanceof org.apache.jmeter.testelement.property.JMeterProperty) {
                                                    newArgs.addProperty((org.apache.jmeter.testelement.property.JMeterProperty) jmeterProp);
                                                    modified = true;
                                                    System.out.println("成功创建格式化后的参数");
                                                }
                                            } else {
                                                System.out.println("JSON格式化无变化");
                                            }
                                        } else {
                                            System.out.println("参数值不是有效的JSON内容");
                                        }
                                    } else if (arg.getClass().getSimpleName().endsWith("TestElementProperty")) {
                                        // 处理TestElementProperty类型
                                        Object testElement = arg.getClass().getMethod("getObjectValue").invoke(arg);
                                        if (testElement != null && testElement.getClass().getSimpleName().endsWith("Argument")) {
                                            String body = (String) testElement.getClass().getMethod("getValue").invoke(testElement);
                                            //System.out.println("TestElement参数值: " + (body != null ? body.substring(0, Math.min(50, body.length())) : "null"));
                                            
                                            if (body != null && (body.trim().startsWith("{") || body.trim().startsWith("["))) {
                                                String formattedJson = JSONFormatter.formatJSON(body);
                                                if (!formattedJson.equals(body)) {
                                                    // 直接修改原始参数值
                                                    testElement.getClass().getMethod("setValue", String.class).invoke(testElement, formattedJson);
                                                    // 使用正确的setValue方法更新属性（继承自父类）
                                                    //arg.getClass().getMethod("setValue", Class.forName("org.apache.jmeter.testelement.TestElement"))
                                                        //.invoke(arg, testElement);
                                            
                                                    modified = true;
                                                    //System.out.println("成功更新TestElement参数值");
                                                }
                                            }
                                        }
                                    } else {
                                        System.out.println("跳过非Argument类型的参数");
                                    }
                                }
                                
                                if (modified) {
                                    // 替换原有参数集合
                                    //Method setArgumentsMethod = arguments.getClass().getMethod("setArguments", CollectionProperty.class);
                                    //setArgumentsMethod.invoke(arguments, newArgs);
                                    
                                    // 标记元素为已修改
                                    Method setRunningVersionMethod = element.getClass().getMethod("setRunningVersion", boolean.class);
                                    setRunningVersionMethod.invoke(element, true);
                                    
                                    // 强制刷新UI
                                    GuiPackage.getInstance().getMainFrame().repaint();
                                    System.out.println("成功格式化JSON请求");
                                } else {
                                    System.out.println("未检测到需要格式化的JSON参数");
                                }
                            } else {
                                System.out.println("参数集合为空");
                            }

                        } catch (Exception e) {
                            System.err.println("反射调用失败: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取类信息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查测试元素是否为支持参数的采样器
     */
    private boolean isSamplerWithArguments(Object element) {
        if (element == null) {
            return false;
        }
        
        try {
            // 检查是否存在getArguments方法
            Method getArgumentsMethod = element.getClass().getMethod("getArguments");
            return getArgumentsMethod != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION location) {
        if (location == MENU_LOCATION.TOOLS) {
            JMenuItem formatItem = new JMenuItem("JSON格式化");
            formatItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    //System.out.println("JSON格式化菜单项被点击");
                    formatJsonRequests();
                }
            });
            return new JMenuItem[] { formatItem };
        }
        return new JMenuItem[0];
    }

    @Override
    public JMenu[] getTopLevelMenus() {
        return new JMenu[0];
    }

    @Override
    public boolean localeChanged(MenuElement menu) {
        return false;
    }

    @Override
    public void localeChanged() {}

    /**
     * 判断测试元素是否是带有参数的采样器
     * @param element 测试元素
     * @return 是否是带有参数的采样器
     */
    private boolean isSamplerWithArguments(TestElement element) {
        // 检查是否实现了getArguments方法
        try {
            return element.getClass().getMethod("getArguments") != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * JMeter插件入口点
     * @param engine JMeter引擎实例
     */
    public static void init(org.apache.jmeter.engine.StandardJMeterEngine engine) {
        new JMeterPlugin().initialize();
    }
}