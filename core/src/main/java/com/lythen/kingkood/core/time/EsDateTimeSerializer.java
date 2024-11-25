package com.lythen.kingkood.core.time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 常夫时间序列化，用于json注解
 * @author 赖仁良
 * @date : 2021-12-02 23:14
 **/
public class EsDateTimeSerializer extends JsonSerializer<Date> {

    /**
     * Method that can be called to ask implementation to serialize
     * values of type this serializer handles.
     *
     * @param value       Value to serialize; can <b>not</b> be null.
     * @param gen         Generator used to output resulting Json content
     * @param serializers Provider that can be used to get serializers for
     */
    @Override
    public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        Instant instant = value.toInstant();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("GMT+8"));
        gen.writeString(localDateTime.format(LocalDateTimeFormater.dateTimeFormatter));
    }
}
