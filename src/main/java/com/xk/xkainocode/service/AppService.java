package com.xk.xkainocode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.xk.xkainocode.model.dto.app.AppAddRequest;
import com.xk.xkainocode.model.dto.app.AppQueryRequest;
import com.xk.xkainocode.model.entity.App;
import com.xk.xkainocode.model.entity.User;
import com.xk.xkainocode.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/kjkyes">xk</a>
 */
public interface AppService extends IService<App> {

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 获取脱敏的应用
     *
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 构造应用查询条件
     *
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);


    /**
     * 获取脱敏的应用列表
     *
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 对话生成应用
     *
     * @param appId     应用 ID
     * @param message   提示词
     * @param loginUser 当前登录用户
     * @return
     */
    Flux<String> chatToGen(Long appId, String message, User loginUser);

    /**
     * 创建应用
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 部署应用
     *
     * @param appId     应用 ID
     * @param loginUser 当前登录用户
     * @return
     */
    String deployApp(Long appId, User loginUser);
}