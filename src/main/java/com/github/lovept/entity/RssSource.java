package com.github.lovept.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("rss_sources")
public class RssSource {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * RSS 订阅源 URL
     */
    @TableField("source_url")
    private String sourceUrl;

    /**
     * 订阅源名称
     */
    @TableField("source_name")
    private String sourceName;

    /**
     * 订阅源描述
     */
    @TableField("description")
    private String description;

    /**
     * 上次检查时间
     */
    @TableField("last_checked")
    private java.util.Date lastChecked;
}
