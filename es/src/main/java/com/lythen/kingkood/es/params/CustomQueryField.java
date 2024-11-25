package com.lythen.kingkood.es.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2023/11/9 10:05
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomQueryField{
    /**
     * 字段名
     */
    String fieldName;
    /**
     * 字段值
     */
    Object fieldValue;
    /**
     * 查询类别
     */
    QueryType queryType;
}
