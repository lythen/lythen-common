package com.lythen.kingkood.es.entity;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lythen.kingkood.core.time.EsDateTimeSerializer;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2023/6/1 10:13
 **/
@Data
public class BaseEsEntity implements Serializable {
    /**
     * ID，ES的ID，唯一值
     */
    @Id
    protected String id = UUID.fastUUID().toString();
    /**
     * 业务ID，关联业务的实际ID
     */
    @Field(value = "ywid")
    protected String ywid;
    /**
     * 创建时间，用于排序
     */
    @Field(value = "@timestamp", store = true, type = FieldType.Date)
    @JsonSerialize(using= EsDateTimeSerializer.class)
    protected Date date = new Date();
    @Field(ignoreFields = "{highlightFields}")
    Map<String, List<String>> highlightFields;
}
