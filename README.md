# JMeter JSON Formatter Plugin

## 简介
一个JMeter插件，用于格式化HTTP请求中的JSON参数，使其在参数列表中显示更美观、更易读。

## 功能特性
- 工具菜单一键格式化JSON参数
- 支持JSON对象和数组格式
- 自动识别JSON格式并美化显示
- 兼容JMeter 5.x及更高版本
- 支持JDK 1.8+

## 安装方法
1. 下载插件JAR文件
2. 将JAR文件复制到JMeter的`lib/ext`目录
3. 重启JMeter

## 使用方法
1. 打开JMeter测试计划
3. 点击顶部菜单栏"工具"->"JSON格式化"
4. 查看格式化后的JSON参数

> ⚠️ 注意事项：
> - 不要选中HTTP请求本身
> - 只有包含JSON格式值的参数才会被处理

## 构建说明
使用Maven构建：
```bash
mvn clean package
```
需要JDK 1.8+ 和 Apache Maven 3.8+

## 贡献指南
欢迎提交Issue和Pull Request，建议包含详细的使用场景描述和测试用例。

## 许可协议
MIT License