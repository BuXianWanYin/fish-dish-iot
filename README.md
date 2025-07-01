# Fish Dish IoT

## 项目简介

**Fish Dish IoT** 是一个基于 Spring Boot 的物联网（IoT）平台，主要用于农业/渔业场景下的设备与传感器数据采集、控制与管理。系统支持多种传感器的自动轮询、数据解析、设备控制、告警推送等功能，并通过串口与设备通信，支持 MQTT、RabbitMQ 等协议扩展。

---

## 主要功能

- **设备管理**：支持多类型农业/渔业设备的注册、状态监控与控制。
- **传感器数据采集**：自动轮询传感器，采集气象、水质等多维度数据。
- **设备控制**：通过串口下发控制指令，实现设备远程开关等操作。
- **数据处理与存储**：对采集到的数据进行解析、处理和持久化。
- **告警与阈值管理**：支持设备异常、数据超限等多种告警机制。
- **定时任务**：支持设备状态检查、环境趋势分析等定时任务。
- **接口服务**：提供 RESTful API，便于前端或第三方系统集成。

---

## 技术栈

- **Java 17**
- **Spring Boot 2.6.13**
- **Spring Data JPA & MyBatis-Plus**（数据持久化）
- **MySQL**（数据库）
- **MQTT**（物联网消息协议）
- **RabbitMQ**（可选，消息队列）
- **jSerialComm**（串口通信）
- **Swagger**（API 文档）
- **Lombok**（简化代码）
- **Springfox**（Swagger 集成）

---

## 目录结构

```
src/
  main/
    java/
      com.fishdishiot.iot/
        config/         # 配置类（如MQTT、Web等）
        constant/       # 常量定义
        controller/     # 控制器，REST API入口
        domain/         # 领域模型（设备、传感器、告警等）
        gateway/        # MQTT等网关适配
        mapper/         # MyBatis-Plus数据访问层
        service/        # 业务服务（含impl实现）
        task/           # 定时/异步任务
        util/           # 工具类
        FishDishIotApplication.java # 应用主入口
    resources/
      application.yml         # 通用配置
      application-dev.yml     # 开发环境配置
      ...
pom.xml                      # Maven依赖与构建配置
```

---

## 主要配置说明

- **application.yml**：全局配置（如MQTT、串口、MyBatis-Plus、RabbitMQ等）。
- **application-dev.yml**：开发环境下的数据库、端口、MQTT、串口等具体参数。
- **串口配置**：`serial.port-name`、`serial.baud-rate`，需根据实际硬件环境调整。
- **MQTT配置**：`mqtt.server-uri`、`mqtt.client-id`等，支持本地与云端MQTT服务器。

---

## 启动方式

1. **环境准备**
   - 安装 JDK 17+
   - 安装并启动 MySQL，导入相关表结构
   - （可选）安装并启动 RabbitMQ、MQTT Broker

2. **配置调整**
   - 修改 `src/main/resources/application-dev.yml`，配置数据库、串口、MQTT等参数

3. **编译与运行**
   ```bash
   mvn clean package
   java -jar target/fish-dish-iot-0.0.1-SNAPSHOT.jar
   ```
   或直接在 IDE 中运行 `FishDishIotApplication.java`

---

## 主要模块说明
- **DeviceOperationController**：设备操作控制器，提供设备开关、指令下发等接口。
- **SensorCommunicationService**：核心服务，负责传感器的周期性通信、数据采集与解析。
- **AgricultureDevice**：设备领域模型，描述设备的基本属性、指令、状态等。
- **定时任务**：如 `EnvironmentTrendAnalysisTask`、`AgricultureDeviceStatusCheckTask`，实现环境趋势分析、设备状态检查等功能。

---

## 贡献与开发
欢迎提交 issue 和 PR，建议先阅读代码结构和主要业务逻辑。开发前请确保本地环境配置正确，数据库和串口设备可用。