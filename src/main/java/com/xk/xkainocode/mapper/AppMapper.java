package com.xk.xkainocode.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xk.xkainocode.model.entity.App;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 应用 映射层。
 *
 * @author <a href="https://github.com/kjkyes">xk</a>
 */
public interface AppMapper extends BaseMapper<App> {

    /**
     * 批量获取点赞数
     * @param countMap
     */
    void batchUpdateUpvoteCount(@Param("countMap")Map<Long, Long> countMap);
}
