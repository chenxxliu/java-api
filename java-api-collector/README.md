# Java API 收集工具

这是一个用于收集Java Web应用API信息的Java Agent工具。它可以通过两种方式获取API信息：
1. 在应用启动时加载（使用premain方法）
2. 在应用运行时动态加载（使用agentmain方法）

## 功能特点

- 自动收集Spring MVC Controller中的API信息
- 支持获取GET和POST请求参数
- 将API信息以JSON格式保存到本地文件
- 支持启动时和运行时两种加载方式

## 使用方法

### 1. 构建项目

```bash
mvn clean package
```

### 2. 启动时加载

在Java应用启动时添加以下JVM参数：

```bash
java -javaagent:path/to/java-api-collector.jar -jar your-application.jar
```

### 3. 运行时加载

使用Java Attach API动态加载agent：

```java
VirtualMachine vm = VirtualMachine.attach("pid");
vm.loadAgent("path/to/java-api-collector.jar");
vm.detach();
```

## 输出格式

收集的API信息将保存在`api_collection.json`文件中，格式如下：

```json
[
  {
    "path": "/api/endpoint",
    "method": "post",
    "parameters": [
      {
        "in": "query",
        "name": "paramName",
        "required": true,
        "schema": {
          "type": "string"
        }
      }
    ],
    "controller": "com.example.Controller",
    "responses": {
      "200": {
        "content": {
          "*/*": {
            "schema": {
              "type": "object"
            }
          }
        },
        "description": "ok"
      }
    }
  }
]
```

## 注意事项

1. 该工具主要用于收集Spring MVC框架的API信息
2. 需要确保有足够的权限来创建和写入JSON文件
3. 建议在开发/测试环境中使用，不建议在生产环境使用 