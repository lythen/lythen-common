package com.lythen.kingkood.core.time;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author 赖仁良
 * @date 2021/12/7
 */
public class LocalDateDeserializer extends JsonDeserializer<LocalDateTime> {
	/**
	 * 常规日期格式化 yyyy-MM-dd
	 * @param json    Parsed used for reading JSON content
	 * @param ctxt Context that can be used to access information about
	 *             this deserialization activity.
	 * @return Deserialized value
	 */
	@Override
	public LocalDateTime deserialize(JsonParser json, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDateTime localDateTime = LocalDateTime.now();
		String strTime = json.getValueAsString();
		if(StrUtil.isNotBlank(strTime)){
			try{
				localDateTime = LocalDate.parse(strTime,LocalDateTimeFormater.dateformatter).atStartOfDay();
			}catch (Exception e){

			}
		}
		return localDateTime;
	}
}
