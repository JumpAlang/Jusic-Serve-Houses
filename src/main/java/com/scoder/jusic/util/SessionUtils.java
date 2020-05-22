package com.scoder.jusic.util;

import org.springframework.web.socket.WebSocketSession;

/**
 * @author alang
 * @create 2020-05-20 23:07
 */
public class SessionUtils {
    public static String getAttributeValue(WebSocketSession session, String key,String defaultValue){
        Object keyValue = session.getAttributes().get(key);
        if(keyValue == null){
            return defaultValue;
        }else{
            String keyValueStr = (String)keyValue;
            return keyValueStr == ""?defaultValue:keyValueStr;
        }
    }
}
