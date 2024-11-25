package com.lythen.kingkood.core.time;

import cn.hutool.core.util.StrUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author 赖仁良
 * @date 2021/10/22
 */
public class LocalDateTimeHepler {
	/**
	 * 获取本地时间，专门指中国的时间，避免服务器的时区问题导致时间不对的问题。
	 * @return
	 */
	public static LocalDateTime getLocalDateTime(){
		return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
	}

	/**
	 * 时间格式化
	 * @param strDateTime
	 * @param patern
	 * @return
	 */
	public static LocalDateTime getLocalDateTime(String strDateTime,String patern){
		if(StrUtil.isBlank(patern)){
			patern = "yyyy-MM-dd HH:mm:ss";
		}
		return LocalDateTime.parse(strDateTime, DateTimeFormatter.ofPattern(patern));
	}

	/**
	 * 时期类转为时间类,支持-分隔和-分隔
	 * @param strDate
	 * @return
	 */
	public static LocalDateTime getLocalDateTimeFromDate(String strDate){
		if(strDate.contains("-")){
			return LocalDate.parse(strDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
		}
		else{
			return LocalDate.parse(strDate, DateTimeFormatter.ofPattern("yyyy/MM/dd")).atStartOfDay();
		}
	}

	/***
	 * 将LocalDateTime转换为标准的UTC时间字符串。
	 * @param localDateTime
	 * @param defaultZoneId 可为null，为null则默认输入时间为东八区时间
	 * @return
	 */
	public static String TransToUTCString(LocalDateTime localDateTime,ZoneId defaultZoneId){
		if(defaultZoneId==null){
			defaultZoneId = ZoneId.of("UTC+8");
		}
		ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime,ZoneId.of("UTC+8"));
		return zonedDateTime.toInstant().toString();
	}
}
