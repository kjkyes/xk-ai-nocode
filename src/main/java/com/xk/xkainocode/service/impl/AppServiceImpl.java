package com.xk.xkainocode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xk.xkainocode.ai.AiCodeGenTypeRoutingService;
import com.xk.xkainocode.ai.AiCodeGenTypeRoutingServiceFactory;
import com.xk.xkainocode.constant.AppConstant;
import com.xk.xkainocode.constant.UpvoteConstant;
import com.xk.xkainocode.core.AiCodeGeneratorFacade;
import com.xk.xkainocode.core.builder.VueProjectBuilder;
import com.xk.xkainocode.core.handler.StreamHandlerExecutor;
import com.xk.xkainocode.exception.BusinessException;
import com.xk.xkainocode.exception.ErrorCode;
import com.xk.xkainocode.exception.ThrowUtils;
import com.xk.xkainocode.mapper.AppMapper;
import com.xk.xkainocode.model.dto.app.AppAddRequest;
import com.xk.xkainocode.model.dto.app.AppQueryRequest;
import com.xk.xkainocode.model.entity.App;
import com.xk.xkainocode.model.entity.User;
import com.xk.xkainocode.model.enums.ChatHistoryMessageTypeEnum;
import com.xk.xkainocode.model.enums.CodeGenTypeEnum;
import com.xk.xkainocode.model.vo.AppVO;
import com.xk.xkainocode.model.vo.UserVO;
import com.xk.xkainocode.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/kjkyes">xk</a>
 */
@Service
@Slf4j
//@RequiredArgsConstructor
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Resource
    @Lazy
    private UpvoteService upvoteService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 对话生成应用
     *
     * @param appId     应用 ID
     * @param message   提示词
     * @param loginUser 当前登录用户
     * @return AI生成代码流
     */
    @Override
    public Flux<String> chatToGen(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 无效");
        ThrowUtils.throwIf(StringUtils.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 2. 获取应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        // 3. 权限校验（只有应用创建者可以对话）
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有应用创建者可以对话");
        }
        // 4. 获取代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum genTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(genTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的代码生成类型");
        // 5. 用户调用ai时，保存用户提示词
        chatHistoryService.addChatMessage(appId, loginUser.getId(), message, ChatHistoryMessageTypeEnum.USER.getValue());
        // 6. 调用ai生成代码（流式）
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveStream(message, genTypeEnum, appId);
        // 7. 返回ai响应内容，并在完成后记录ai的响应内容到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, genTypeEnum);
    }

    /**
     * 创建应用
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型（多例模式）
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        // todo 注意这里因为选用chatglm模型，输出包含<think>，我们在此临时处理一下
        String row = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        String clean = row.replaceAll("(?s)<think>.*?</think>", "").trim();
        CodeGenTypeEnum selectedCodeGenType = CodeGenTypeEnum.valueOf(clean);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 点赞数默认为0
        app.setUpvoteCount(0);
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        // 将应用创建时间写入redis
        String appCreateTimeKey = UpvoteConstant.APP_CREATE_TIME_KEY_PREFIX.formatted(app.getId());
        DateTime nowDate = DateUtil.date();
        int year = DateUtil.year(nowDate);
        int month = DateUtil.month(nowDate) + 1;
        redisTemplate.opsForValue().set(appCreateTimeKey, String.format("%d:%d", year, month));
        return app.getId();
    }

    /**
     * 部署应用（nginx部署）
     *
     * @param appId     应用 ID
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 无效");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        // 3. 权限校验（只有应用创建者可以部署）
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有应用创建者可以部署");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 如果没有，则生成 6 位deployKey（字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，获取原始代码生成路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. Vue项目的部署需要特殊处理
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        if(codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT){
            // 构建 Vue 项目
            // todo vue项目在对话生成的时候就已经构建过了，所以这里可以test一下是否有必要再部署时再构建一次
//            boolean successBuild = vueProjectBuilder.buildProject(sourceDirPath);
//            ThrowUtils.throwIf(!successBuild, ErrorCode.SYSTEM_ERROR, "构建 Vue 项目失败，请重试");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDir, "dist");
            ThrowUtils.throwIf(distDir == null || !distDir.exists(), ErrorCode.SYSTEM_ERROR, "构建 Vue 项目成功，但dist文件未生成");
            sourceDir = distDir;
            log.info("Vue项目构建成功，将部署dist文件：{}", sourceDir);
        }
        // 8. 复制文件到部署目录（这里的部署文件名就设置为deployKey）
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败，请重试" + e.getMessage());
        }
        // 9. 更新数据库（更新应用的 deployKey 和部署时间）
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean isUpdate = this.updateById(updateApp);
        ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败，请重试");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }


    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 调用带有appId的截图服务，截图服务会将任务发送到消息队列
        // 消费者完成截图后会自动更新应用封面
        screenshotService.generateAndUploadScreenshot(appUrl, appId);
    }


    /**
     * 获取脱敏的应用
     *
     * @param app
     * @return
     */
    @Override
    public AppVO getAppVO(App app, User loginUser) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询点赞信息
//        QueryWrapper queryWrapper = QueryWrapper.create()
//                .eq(Upvote::getAppId, app.getId())
//                .eq(Upvote::getUserId, loginUser.getId());
//        Upvote upvote = upvoteService.getOne(queryWrapper);
//        if(upvote != null){
//            appVO.setIsUpvote(true);
//        }
        Boolean hasUpvote = upvoteService.hasUpvote(app.getId(), loginUser.getId());
        appVO.setIsUpvote(hasUpvote);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    /**
     * 构造应用的查询条件
     *
     * @param appQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 获取脱敏的应用列表
     *
     * @param appList
     * @return
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList, User loginUser) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app, loginUser);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    /**
     * 删除应用时，删除当前应用的对话历史
     *
     * @param id 应用id
     * @return
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        Long appId = Long.valueOf(id.toString());
        if(appId == null){
            return false;
        }
        // 删除当前应用的对话历史
        try {
            chatHistoryService.deleteChatMessage(appId);
        } catch (Exception e) {
            log.error("删除对话历史失败" + e.getMessage());
        }
        // 删除当前应用（容错设计，即使删除应用对话历史失败，也能正常删除应用）
        return super.removeById(id);
    }

}
