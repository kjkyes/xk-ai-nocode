# 目录
+ 项目介绍
+ 核心能力
+ 调研分析
+ 技术选型
+ 业务流程
+ 用户模块
+ 管理员模块
+ 智能体开发的三种实现方式
+ 项目中遇到的问题

# 🌍项目介绍
这是一套以AI开发 + 后端架构设计为核心的AI应用，基于Spring Boot 3 + LangChain4j + LangGraph4j开发的AI代码生成平台，深度探究目前火热的AI智能体开发、AI工作流等前沿技术。

# 🗒️核心能力
1. 智能代码生成：用户输入需求描述，AI自动分析并选择合适的生成策略，通过工具调用生成代码文件，采用流式输出方便前端实时展示AI的执行过程。
2. 可视化修改：生成应用的所有内容将交由用户任意编辑，直到满意。后台基于文件操作、网页搜索、网页抓取等工具和AI对话接口来快速修改页面，满足用户的个性化需求。
3. 源码下载分享：调用部署接口时自动截取网站页面做为封面图，支持下载网站完整源码。
4. 企业及管理：提供用户管理、应用管理、系统监控、业务指标监控等后台功能，管理员可以设置精选应用、监控AI调用情况和系统性能。

# 🗯️调研分析
调研学习过同类型其他平台，如美团的[NoCode](https://nocode.cn)和百度[秒哒](https://www.miaoda.cn/?track_id=promolink-9lgly6hor8jk)，了解一些前端开发的基本流程和常用技术（开发框架、常用组件库、构建工具、包管理器），于是就可以开发自己的无代码生成应用平台啦。

这里以NoCode为例：

1. 输入用户提示词。

<!-- 这是一张图片，ocr 内容为： -->
![1.png](PictureResource%2F1.png)

2. 实时预览应用生成过程和AI思考、工具调用过程，应用生成后可部署、下载源码。

<!-- 这是一张图片，ocr 内容为： -->
![2.png](PictureResource%2F2.png)

# 🔧技术选型
<!-- 这是一张图片，ocr 内容为： -->
![3.png](PictureResource%2F3.png)

# 🧾业务流程
<!-- 这是一张图片，ocr 内容为： -->
![4.png](PictureResource%2F4.png)

# 🙋用户模块
<!-- 这是一张图片，ocr 内容为： -->
![5.png](PictureResource%2F5.png)

# 🙇管理员模块
<!-- 这是一张图片，ocr 内容为： -->
![6.png](PictureResource%2F6.png)

# 🎯智能体开发的三种实现方式
**<font style="color:rgba(0, 0, 0, 0.9);">智能体（AI Agent）</font>**<font style="color:rgba(0, 0, 0, 0.9);">是一种能够</font>**<font style="color:rgba(0, 0, 0, 0.9);">自主感知环境、制定决策并采取行动</font>**<font style="color:rgba(0, 0, 0, 0.9);">以实现特定目标的AI系统 。</font><font style="color:rgba(0, 0, 0, 0.9);">最简洁的定义：</font>**<font style="color:rgba(0, 0, 0, 0.9);">Agent = 大模型（LLM）+ 规划（Planning）+ 记忆（Memory）+ 工具使用（Tool Use）。</font>**<font style="color:rgba(0, 0, 0, 0.9);">相比于传统AI助手，它能自主循环执行、多步骤规划+执行、主动调用搜索引擎、代码执行等、长短期记忆结合。</font>

<font style="color:rgba(0, 0, 0, 0.9);">该项目用三种方式实现智能体应用：</font>

+ LangChain4j基础特性原生实现：仅使用LangChain4j的工具调用、对话消息模型等基础特性，以及Prompt工程，实践了CoT思维链、Agent Loop、ReAct模式（Think-Act-Observe）等主流Agent实现技术。从0实现对话历史排布逻辑，保证智能体工作有序进行。
+ LangChain4j高级特性实现：使用**LangChain4j**的AiService（AiService已实现上述方式的ReAct模式）、工具调用、流式响应、结构化输出、对话记忆等高级特性以及多种设计模式、数据中间件、工具库实现无代码生成应用平台的各项功能。
+ LangGraph4j AI工作流实现：<font style="color:rgba(0, 0, 0, 0.9);">通过</font>**<font style="color:rgba(0, 0, 0, 0.9);">LangGraph4j</font>**<font style="color:rgba(0, 0, 0, 0.9);">的基本特性（StateGraph、AgentState、Nodes、Edges）和高级特性（SSE流式输出、可视化工作流结构图、并发）。对应用Java8的CompletableFuture</font>**<font style="color:rgba(0, 0, 0, 0.9);">（方案一）</font>**<font style="color:rgba(0, 0, 0, 0.9);">、LangGraph4j的Parallel Branch特性</font>**<font style="color:rgba(0, 0, 0, 0.9);">（方案二）</font>**<font style="color:rgba(0, 0, 0, 0.9);">和子图特性</font>**<font style="color:rgba(0, 0, 0, 0.9);">（方案三）</font>**<font style="color:rgba(0, 0, 0, 0.9);"> -- 这三种并发搜集应用图片方案的性能进行比较。性能上，LangGraph4j的并发实现相较于并发前、方案一、方案三分别优化了440997ms、1966ms、17738ms。</font>

## 并发前
并发前性能：<!-- 这是一张图片，ocr 内容为： -->
![7.png](PictureResource%2F7.png)

效果展示：

<!-- 这是一张图片，ocr 内容为： -->
![8.png](PictureResource%2F8.png)

## 并发方案一：应用Java8的Complable Future
性能：

<!-- 这是一张图片，ocr 内容为： -->
![9.png](PictureResource%2F9.png)

架构图：

<!-- 这是一张图片，ocr 内容为： -->
![10.png](PictureResource%2F10.png)

效果展示：

<!-- 这是一张图片，ocr 内容为： -->
![11.png](PictureResource%2F11.png)

## 并发方案二：LangGraph4j的并发实现
性能：

<!-- 这是一张图片，ocr 内容为： -->
![12.png](PictureResource%2F12.png)

架构图：

<!-- 这是一张图片，ocr 内容为： -->
![13.png](PictureResource%2F13.png)

效果展示：

<!-- 这是一张图片，ocr 内容为： -->
![14.png](PictureResource%2F14.png)

## 并发方案三：LangGraph4j的子图实现
性能：

<!-- 这是一张图片，ocr 内容为： -->
![15.png](PictureResource%2F15.png)

架构图：<!-- 这是一张图片，ocr 内容为： -->
![16.png](PictureResource%2F16.png)

效果展示：<!-- 这是一张图片，ocr 内容为： -->
![17.png](PictureResource%2F17.png)

# 🎯项目中遇到的问题
## 问题一：结构化输出
### 问题
1. 刚接入AI后，我用LangChain4j的AI Service特性开发了AI接口，提示词中写了严格依照JSON格式输出，并给出了示例数据，但测试时报错了，报错信息显示“输出解析异常”，说明AI并没有依照提示词返回JSON格式的数据。

### 解决方案
1. 参照[DeepSeek官方文档](https://api-docs.deepseek.com/zh-cn/guides/json_mode)的建议，调大了max_tokens参数（调到上限 -> 8192），防止JSON字符串被中途截断；设置response_format参数为 json_object。

<!-- 这是一张图片，ocr 内容为： -->
![18.png](PictureResource%2F18.png)

2. 参照[LangChain4j官方文档](https://docs.langchain4j.dev/tutorials/structured-outputs/#adding-description-1)中结构化输出部分的建议，给要返回的类和属性添加了Description标签，让AI更好的理解我们需要的输出。

<!-- 这是一张图片，ocr 内容为： -->
![19.png](PictureResource%2F19.png)

## 问题二：下载浏览器驱动超时
### 问题
配置Selenium自动化框架时，应用WebDriverManger下载浏览器驱动超时。

### 解决方案
选择国内镜像，在程序启动前设置系统属性

```java
System.setProperty("wdm.chromeDriverMirrorUrl", "https://registry.npmmirror.com/binary.html?path=chromedriver");
WebDriverManager.chromedriver().useMirror().setup();
```

### 新问题
采用国内镜像源后依旧失败，分析认为是国内镜像源更新慢，而浏览器驱动更新快，导致使用本地镜像源有时会失效。

### 新解决方案
这时选择下载驱动到本地，然后将驱动文件配置到 resources 目录下，修改原本截图工具类的驱动初始化部分：

```java
private static final String CHROME_DRIVER_PATH =
            System.getProperty("user.dir") + "/src/main/resources/web_drivers/chromedriver.exe";

private static WebDriver initChromeDriver(int width, int height) {
        try {
            File driverFile = new File(CHROME_DRIVER_PATH);
            if (!driverFile.exists()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "ChromeDriver 未找到，请确保文件存在于: " + CHROME_DRIVER_PATH);
            }
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
```

<!-- 这是一张图片，ocr 内容为： -->
![20.png](PictureResource%2F20.png)

## 问题三：redis缓存精选应用信息时报反序列化错误
### 问题
在对**精选应用信息**应用redis缓存时，配置**缓存管理器**时，将**value**的序列化器设置为**json序列化器**，结果在使用过程中，在反序列化时，redis无法识别我们自定义的Java类“BaseResponse”导致造型错误。

<!-- 这是一张图片，ocr 内容为： -->
![21.png](PictureResource%2F21.png)

### 解决方案
设置ObjectMapper的**activateDefaultTyping**，配置序列化时将**对象类型**以**字段**的形式包含在json内，这样反序列化时也能恢复为对应类型。

> 这里有三个参数：  
1.类型验证器：验证什么样的类型可以被反序列化，这里我们选择最宽松的验证器，允许所有类型（生产环境建议自定义类型验证器，限制只允许反序列化特定包下的类）；  
2.允许被加入到JSON的类型：这里我们设置成将**非final（Integer、String）**的类型（比如我们自定义的类）才会被加入到**json**中；  
3.类型以什么形式存储：这里我们让**自定义的类**以JSON中的**属性**形式反序列化。当然，除了属性外，还可以以对象、数组等方式反序列化。
>

```java
private ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
}
```

效果如下：

<!-- 这是一张图片，ocr 内容为： -->
![22.png](PictureResource%2F22.png)

