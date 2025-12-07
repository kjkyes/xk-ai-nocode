package com.xk.xkainocode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.xk.xkainocode.model.dto.user.UserQueryRequest;
import com.xk.xkainocode.model.entity.User;
import com.xk.xkainocode.model.vo.LoginUserVO;
import com.xk.xkainocode.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author <a href="https://github.com/kjkyes">xk</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 注册成功返回用户id，注册失败返回-1
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 加密
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptedPassword(String userPassword);

    /**
     * 获取已脱敏的当前登录用户
     *
     * @return 当前登录用户
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的管理用户
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的管理用户列表
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 转换请求为 QueryWrapper对象
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
}