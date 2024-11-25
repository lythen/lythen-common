package com.lythen.kingkood.core.time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 *
 * @author 赖仁良
 * @date : 2021-12-02 23:14
 **/
public class LocalDateSerializerCN extends JsonSerializer<LocalDateTime> {

    /**
     * Method that can be called to ask implementation to serialize
     * values of type this serializer handles.
     * 中文日期格式化 yyyy年MM月dd日
     * @param value       Value to serialize; can <b>not</b> be null.
     * @param gen         Generator used to output resulting Json content
     * @param serializers Provider that can be used to get serializers for
     */
    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.format(LocalDateTimeFormater.dateformatter_cn));
    }
}
