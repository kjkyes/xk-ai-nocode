package com.xk.xkainocode.aop;

import com.xk.xkainocode.annotation.AuthCheck;
import com.xk.xkainocode.exception.BusinessException;
import com.xk.xkainocode.exception.ErrorCode;
import com.xk.xkainocode.model.entity.User;
import com.xk.xkainocode.model.enums.UserRoleEnum;
import com.xk.xkainocode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限校验切面
 */
@Aspect
@Component
public class AuthInterceptor {

    static {
        System.out.println("=== User class loaded from: " +
                com.xk.xkainocode.model.entity.User.class.getProtectionDomain().getCodeSource().getLocation());
    }
    @Resource
    private UserService userService;

    /**
     * 权限校验
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 1.获取要求权限
        String mustRole = authCheck.mustRole();
        // 2.从线程本地变量中取出HttpServletRequest
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        // 3.获取当前登录用户
        User user = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 4.判断用户是否具有要求权限
        // 如果没有权限要求，则放行，继续原业务
        if (mustRole == null) {
            joinPoint.proceed();
        }
        // 如果有权限要求，则判断用户是否具有要求权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(user.getUserRole());
        // 当前用户没有权限
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 要求是管理员权限但用户权限不够
        if (UserRoleEnum.Admin.equals(mustRoleEnum) && !UserRoleEnum.Admin.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
