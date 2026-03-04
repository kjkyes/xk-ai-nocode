package com.xk.xkainocode.util;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.xk.xkainocode.exception.BusinessException;
import com.xk.xkainocode.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

@Slf4j
public class WebScreenshotUtils {
    private static final String CHROME_DRIVER_PATH =
            System.getProperty("user.dir") + "/src/main/resources/web_drivers/chromedriver.exe";
    private static final ThreadLocal<WebDriver> webDriverThreadLocal = ThreadLocal.withInitial(() -> {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        return initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    });

    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // 自动管理 ChromeDriver
//            System.setProperty("wdm.chromeDriverMirrorUrl", "https://registry.npmmirror.com/binary.html?path=chromedriver");

            // 检查 ChromeDriver 文件是否存在
            File driverFile = new File(CHROME_DRIVER_PATH);
            if (!driverFile.exists()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "ChromeDriver 未找到，请确保文件存在于: " + CHROME_DRIVER_PATH);
            }
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);

//            WebDriverManager.chromedriver().useMirror().setup();

            // 配置 Chrome 选项
            ChromeOptions options = getChromeOptions(width, height);
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }

    /**
     * 浏览器配置项所需参数
     *
     * @param width
     * @param height
     * @return
     */
    private static ChromeOptions getChromeOptions(int width, int height) {
        ChromeOptions options = new ChromeOptions();
        // 无头模式
        options.addArguments("--headless");
        // 禁用GPU（在某些环境下避免问题）
        options.addArguments("--disable-gpu");
        // 禁用沙盒模式（Docker环境需要）
        options.addArguments("--no-sandbox");
        // 禁用开发者shm使用
        options.addArguments("--disable-dev-shm-usage");
        // 设置窗口大小
        options.addArguments(String.format("--window-size=%d,%d", width, height));
        // 禁用扩展
        options.addArguments("--disable-extensions");
        // 设置用户代理
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        return options;
    }

    /**
     * 保存原始图片到指定目录
     *
     * @param imgBytes
     * @param imgFilePath
     */
    public static void saveImage(byte[] imgBytes, String imgFilePath) {
        try {
            FileUtil.writeBytes(imgBytes, imgFilePath);
            log.info("图片保存完成，路径为：{}", imgFilePath);
        } catch (Exception e) {
            log.error("图片保存失败: {}", imgFilePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败");
        }
    }

    /**
     * 压缩图片到指定目录
     *
     * @param originalImgPath
     * @param compressedImgPath
     */
    public static void compressImage(String originalImgPath, String compressedImgPath) {
        // 压缩质量（控制图片清晰度，越清晰的图片内存越大）
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originalImgPath),
                    FileUtil.file(compressedImgPath),
                    COMPRESSION_QUALITY
            );
            log.info("图片压缩完成，压缩质量为：{}", COMPRESSION_QUALITY);
        } catch (IORuntimeException e) {
            log.error("图片压缩失败: {} -> {}", originalImgPath, compressedImgPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片压缩失败");
        }
    }

    /**
     * 等待页面加载完成
     */
    private static void waitForPageLoad(WebDriver driver) {
        try {
            // 创建等待页面加载对象
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // 等待 document.readyState 为complete
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver)
                            .executeScript("return document.readyState")
                            .equals("complete")
            );
            // 额外等待一段时间，确保动态内容加载完成
            Thread.sleep(2000);
            log.info("页面加载完成");
        } catch (Exception e) {
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }

    /**
     * 生成网页截图
     *
     * @param webUrl 网页URL
     * @return 压缩后的截图文件路径，失败返回null
     */
    public static String saveWebPageScreenshot(String webUrl) {
        // 参数校验
        if (StrUtil.isBlank(webUrl)) {
            return null;
        }
        WebDriver driver = null;
        try {
            // 从ThreadLocal中获取当前线程的WebDriver实例
            driver = webDriverThreadLocal.get();
            
            // 创建临时目录
            String rootPath = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "screenshots"
                    + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);
            // 图片后缀
            final String IMAGE_SUFFIX = ".png";
            // 原始截图文件路径
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;
            // 访问网页
            driver.get(webUrl);
            // 等待页面加载完成
            waitForPageLoad(driver);
            // 截图
            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            // 保存原始图片
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始截图保存成功: {}", imageSavePath);
            // 压缩图片
            final String COMPRESSION_SUFFIX = "_compressed.jpg";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESSION_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩图片保存成功: {}", compressedImagePath);
            // 删除原始图片，只保留压缩图片
            FileUtil.del(imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            log.error("网页截图失败: {}", webUrl, e);
            return null;
        }
//        finally {
//            // 关闭当前 WebDriver 实例
//            if (driver != null) {
//                try {
//                    driver.quit();
//                } catch (Exception e) {
//                    log.warn("关闭 WebDriver 时出现异常", e);
//                }
//            }
//        }
    }

    /**
     * 在消息队列模型中，每个消费者线程会重复使用 ThreadLocal 中的 WebDriver 实例，而不是每次都创建新的。WebDriver 实例会在整个应用生命周期中保持活跃，所以目前不需要频繁关闭。
     * 不过， 为了资源管理考虑 ，我应该在消费者处理完任务后调用 closeWebDriver() 来释放资源。但这需要谨慎处理，因为频繁创建和销毁 WebDriver 实例会影响性能。
     * 建议 ：暂时不调用该方法，因为 Spring AMQP 的消费者线程会持续运行，重复使用 WebDriver 实例是更高效的做法。这个 closeWebDriver() 方法可以作为应用关闭时的钩子来处理
     */
    @PreDestroy
    public static void closeWebDriver() {
        WebDriver driver = webDriverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("WebDriver已关闭");
            } catch (Exception e) {
                log.error("关闭WebDriver失败", e);
            } finally {
                webDriverThreadLocal.remove();
            }
        }
    }
}