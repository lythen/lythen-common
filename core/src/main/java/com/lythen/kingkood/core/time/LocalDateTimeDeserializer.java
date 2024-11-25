package com.lythen.kingkood.core.time;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author 赖仁良
 * @date 2021/12/7
 */
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
	/**
	 * 时间反序列化，用于json
	 * @param json    Parsed used for reading JSON content
	 * @param ctxt Context that can be used to access information about
	 *             this deserialization activity.
	 * @return Deserialized value
	 */
	@Override
	public LocalDateTime deserialize(JsonParser json, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		LocalDateTime localDateTime = null;
		String strTime = json.getValueAsString();
		if(StrUtil.isNotBlank(strTime)){
			try{
				localDateTime = LocalDateTime.parse(strTime,LocalDateTimeFormater.dateTimeFormatter);
			}catch (Exception e){

			}
			if(localDateTime==null){
				try{
					LocalDate localDate = LocalDate.parse(strTime,LocalDateTimeFormater.dateformatter);
					if(localDate!=null){
						localDateTime = localDate.atStartOfDay();
					}
				}catch (Exception e){

				}
			}
			if(localDateTime==null){
				try{
					localDateTime = LocalDateTime.parse(strTime,LocalDateTimeFormater.dateTimeFormatter_cn);
				}catch (Exception e){

				}
			}
			if(localDateTime==null){
				try{
					localDateTime = LocalDateTime.parse(strTime,LocalDateTimeFormater.dateformatter_cn);
				}catch (Exception e){

				}
			}
			if(localDateTime==null){
				try{
					localDateTime = LocalDateTime.parse(strTime,LocalDateTimeFormater.dateformatter_long);
				}catch (Exception e){

				}
			}
			if(localDateTime==null){
				try{
					localDateTime = LocalDateTime.parse(strTime,LocalDateTimeFormater.dateformatter_long2);
				}catch (Exception e){

				}
			}
			if(localDateTime==null){
				try{
					localDateTime = LocalDateTime.parse(strTime,LocalDateTimeFormater.dateformatter);
				}catch (Exception e){

				}
			}
		}
		return localDateTime;
	}
}
