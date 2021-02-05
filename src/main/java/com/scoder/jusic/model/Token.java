package com.scoder.jusic.model;

import io.micrometer.core.instrument.util.StringUtils;

import java.util.Date;

/**
 * @author JumpAlang
 * @create 2021-02-05 17:14
 */

public class Token {

    private String token;   //token
    private long expires;         //token有效时间

    private long tokenTime;       //token产生时间
    private int redundance = 10*1000;  //冗余时间，提前10秒就去请求新的token

    /**
     * 得到access token
     */
    public String getToken(){
        return this.token;
    }

    public void setToken(String token){
        this.token = token;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    public long getTokenTime() {
        return tokenTime;
    }

    public void setTokenTime(long tokenTime) {
        this.tokenTime = tokenTime;
    }

    /**
     * 得到有效时间
     */
    public long getExpires() {
        return expires;
    }
    /**
     * accessToken 是否有效
     * @return true:有效，false: 无效
     */
    public boolean isValid(){
        //黑名单判定法
        if(StringUtils.isBlank(this.token))
            return false;
        if(this.expires <= 0)
            return false;
        //过期
        if(isExpire())
            return false;
        return true;
    }

    /**
     * 是否过期
     * @return true 过期 false：有效
     */
    private boolean isExpire(){
        Date currentDate = new Date();
        long currentTime = currentDate.getTime();
        long expiresTime = expires * 1000 - redundance;
        //判断是否过期
        if((tokenTime + expiresTime) > currentTime)
            return false;
        return true;
    }
}
