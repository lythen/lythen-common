package com.lythen.kingkood.core.time;

import java.time.format.DateTimeFormatter;

/**
 * <p>本地时间格式化公共类</p>
 *
 * @author 赖仁良
 * @date : 2022-05-25 16:41
 **/
public class LocalDateTimeFormater {
    /**
     * 常规时间
     */
    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 常日期
     */
    public static DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * 中文时间
     */
    public static DateTimeFormatter dateTimeFormatter_cn = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时mm分ss秒");
    /**
     * 中文日期
     */
    public static DateTimeFormatter dateformatter_cn = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    /**
     * 格式化为全数字，到秒
     */
    public static DateTimeFormatter dateformatter_long = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /**
     * 格式化为全数字，到毫秒
     */
    public static DateTimeFormatter dateformatter_long2 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
}
