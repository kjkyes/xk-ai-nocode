package com.xk.xkainocode.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *  实体类。
 *
 * @author <a href="https://github.com/kjkyes">xk</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("upvote")
public class Upvote implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 点赞 id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 用户 id
     */
    @Column("userId")
    private Long userId;

    /**
     * 应用 id
     */
    @Column("appId")
    private Long appId;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

}
