package com.lythen.kingkood.es.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * 在数据导入ES之前，对时间进行转换，转换面ES可识别的时间格式。
 *
 * @Author lythen
 * @date 2023/6/6 11:34
 **/
@Slf4j
public class LocaDateTimeEsConverter implements PropertyValueConverter {
    /**
     * Converts a property value to an elasticsearch value. If the converter cannot convert the value, it must return a
     * String representation.
     *
     * @param value the value to convert, must not be {@literal null}
     * @return The elasticsearch property value, must not be {@literal null}
     */
    @Override
    public Object write(Object value) {
        if(value instanceof LocalDateTime ) {
            LocalDateTime localDateTime = (LocalDateTime)value;
            ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime,ZoneId.of("UTC+8"));
            return zonedDateTime.toInstant().toString();
        }
        return  value;
    }

    /**
     * Converts an elasticsearch property value to a property value.
     *
     * @param value the elasticsearch property value to convert, must not be {@literal null}
     * @return The converted value, must not be {@literal null}
     */
    @Override
    public Object read(Object value) {
        try {
            String strDateTime = value.toString();
            Instant instant = null;
            if(value.getClass().getName().contains("Long")){
                long longTime = (Long) value;
                Date date = new Date(longTime);
                instant =date.toInstant();
            }
            else{
                instant = Instant.parse(strDateTime);
            }
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant,ZoneId.of("UTC+8"));
            return localDateTime;
        }catch (Exception e){
            log.error(e.getMessage(),e);
            return null;
        }
    }
}
