package com.scoder.jusic.controller;

import com.alibaba.fastjson.JSONObject;
import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.RetainKey;
import com.scoder.jusic.model.Token;
import com.scoder.jusic.util.IPUtils;
import com.scoder.jusic.util.StringUtils;
import com.scoder.jusic.util.UUIDUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author JumpAlang
 * @create 2020-06-10 10:36
 */
@Controller
@Slf4j
//@CrossOrigin
public class HomeController {
    @Autowired
    private HouseContainer houseContainer;
    @Autowired
    private JusicProperties jusicProperties;

    public static Token TOKEN;

    @RequestMapping("/house/add")
    @ResponseBody
    public Response addHouse(@RequestBody House house, HttpServletRequest accessor) {
        String sessionId = UUIDUtils.getUUID8Len(accessor.getSession().getId());
        if(house.getName() == null || house.getName() == "" || house.getName().length() > 33){
           return Response.failure((Object) null, "房间名称不能为空且不能超过33个字符");
        }
        if(house.getDesc() != null && house.getDesc().length() > 133){
            return Response.failure((Object) null, "房间描述不能超过133个字符");
        }
        if(house.getNeedPwd() != null && house.getNeedPwd()){
            if(house.getPassword() == null || "".equals(house.getPassword().trim())){
                return Response.failure((Object) null, "房间密码不能为空");
            }else if(StringUtils.isUrlSpecialCharacter(house.getPassword())){
                return Response.failure((Object) null, "密码不能有如下字符：空格、?、%、#、&、=、+");
            }
            house.setPassword(house.getPassword().trim());
        }
        if(houseContainer.contains(sessionId)){
            return Response.failure((Object) null, "你已经创建过一个房间，待其被自动腾空方可再创建");
        }
        if(houseContainer.size() >= jusicProperties.getHouseSize()){
            return Response.failure((Object) null, "暂时不能新增房间，待其他空房间被自动腾空方可创建。");
        }
        String ip = IPUtils.getRemoteAddress(accessor);
        if(houseContainer.isBeyondIpHouse(ip,jusicProperties.getIpHouse())){
            return Response.failure((Object) null, "该网络暂时不能新增房间，待其他空房间被自动腾空方可创建。");
        }
        if(house.getEnableStatus() != null && house.getEnableStatus()){
            if(house.getRetainKey() == null || "".equals(house.getRetainKey().trim())){
                return Response.failure((Object) null, "订单号不能为空");
            }
            RetainKey key = houseContainer.getRetainKey(house.getRetainKey());
            if(key == null){
                return Response.failure((Object) null, "订单号不存在或须等待3分钟后系统才能生效，如3分钟后还不存在，请加q群：672905926");
            }else if(key.getIsUsed()){
                    return Response.failure((Object) null, "订单号已被使用");
            }else if(key.getExpireTime() != null && key.getExpireTime() < System.currentTimeMillis()){
                    return Response.failure((Object) null, "订单号已过期");
            }
            key.setHouseId(sessionId);
            key.setRemoteAddress(ip);
            key.setUsedTime(System.currentTimeMillis());
            key.setIsUsed(true);
            houseContainer.updateRetainKey(key);
        }
        house.setId(sessionId);
        house.setCreateTime(System.currentTimeMillis());
//        house.setEnableStatus(false);
        house.setSessionId(sessionId);
        house.setRemoteAddress(ip);
        house.setAdminPwd(jusicProperties.getRoleAdminPassword());
        houseContainer.add(house);
        return Response.success(sessionId,"创建房间成功");
    }
    @RequestMapping("/house/enter")
    @ResponseBody
    public Response enterHouse(@RequestBody House house, HttpServletRequest accessor) {
        String sessionId = UUIDUtils.getUUID8Len(accessor.getSession().getId());
        if(!houseContainer.contains(house.getId())){
           return Response.failure((Object) null, "房间已经不存在");
        }else{
            House matchHouse = houseContainer.get(house.getId());
            if(matchHouse.getNeedPwd() &&  !matchHouse.getPassword().equals(house.getPassword())){// || !matchHouse.getSessionId().equals(sessionId)
             if(jusicProperties.getSessions(house.getId()).size() == 0 && !matchHouse.getEnableStatus()) {
                 houseContainer.destroy(house.getId());
             }
             return Response.failure((Object) null, "请输入正确的房间密码");
            }
        }
       return Response.success(sessionId, "进入房间成功");
    }


    @RequestMapping("/house/search")
    @ResponseBody
    public Response searchHouse(HttpServletRequest accessor) {
        CopyOnWriteArrayList<House> houses = houseContainer.getHouses();
        ArrayList<House> housesSimple = new ArrayList<>();
        for(House house : houses){
            House houseSimple = new House();
            houseSimple.setName(house.getName());
            houseSimple.setId(house.getId());
            houseSimple.setDesc(house.getDesc());
            houseSimple.setCreateTime(house.getCreateTime());
            houseSimple.setNeedPwd(house.getNeedPwd());
            houseSimple.setPopulation(jusicProperties.getSessions(house.getId()).size());
            housesSimple.add(houseSimple);
        }
        return Response.success(housesSimple, "房间列表");
    }


    @RequestMapping("/house/edit")
    @ResponseBody
    public Response edit(@RequestBody House house, HttpServletRequest accessor) {
        // TODO  权限认证
//        String ip = IPUtils.getRemoteAddress(accessor);
            House housePrimitive = houseContainer.get(house.getId());
            if(housePrimitive == null){
                return Response.failure((Object)null,"当前房间不存在");
            }
            if(house.getCanDestroy() != null && house.getCanDestroy()){
                houseContainer.destroy(house.getId());
                if(housePrimitive.getEnableStatus() != null && housePrimitive.getEnableStatus()){
                    houseContainer.refreshHouses();
                }
                return Response.success(housePrimitive, "销毁房间成功");
            }
            if(house.getForbiddenModiPwd() != null){
                housePrimitive.setForbiddenModiPwd(house.getForbiddenModiPwd());
            }
            if(house.getNeedPwd() != null){
                housePrimitive.setNeedPwd(house.getNeedPwd());
            }
            if(house.getPassword() != null){
                housePrimitive.setPassword(house.getPassword());
            }
            if(house.getName() != null){
                housePrimitive.setName(house.getName());
            }
            if(house.getDesc() != null){
                housePrimitive.setDesc(house.getDesc());
            }
            if(house.getEnableStatus() != null){
                housePrimitive.setEnableStatus(house.getEnableStatus());
            }
            if(house.getAdminPwd() != null){
                housePrimitive.setAdminPwd(house.getAdminPwd());
            }
            if(housePrimitive.getEnableStatus() != null && housePrimitive.getEnableStatus()){
                houseContainer.refreshHouses();
            }
            return Response.success(housePrimitive, "修改房间成功");
    }

    @RequestMapping("/house/setSize/{size}")
    @ResponseBody
    public Response setSize(@PathVariable Integer size, HttpServletRequest accessor) {
        // TODO  权限认证
        if(size == null){
            return Response.failure((Object)jusicProperties.getHouseSize(),"当前可创建房间总数"+(jusicProperties.getHouseSize()-houseContainer.size()));
        }else{
            jusicProperties.setHouseSize(size);
            return Response.success(jusicProperties.getHouseSize(), "当前可创建房间总数"+(jusicProperties.getHouseSize()-houseContainer.size()));
        }
    }

    @RequestMapping("/house/addRetainKey/{key}")
    @ResponseBody
    public Response addRetainKey(@PathVariable String key, HttpServletRequest accessor) {
        if(key == null){
            return Response.failure((Object)null,"订单号不能为空");
        }else{
            RetainKey retainKey = houseContainer.getRetainKey(key);
            if(retainKey != null){
                return Response.failure(retainKey,"订单号已经存在");
            }
            RetainKey retainKeyNew = new RetainKey();
            retainKeyNew.setIsUsed(false);
            retainKeyNew.setCreateTime(System.currentTimeMillis());
            retainKeyNew.setKey(key);
            houseContainer.addRetainKey(retainKeyNew);
            return Response.success((Object) null, "添加成功");
        }
    }

    @RequestMapping("/house/showRetainKey")
    @ResponseBody
    public Response showRetainKey(HttpServletRequest accessor) {
            return Response.success(houseContainer.showRetainKey(), "所有订单号");
    }

    @RequestMapping("/house/removeRetainKey/{key}")
    @ResponseBody
    public Response removeRetainKey(@PathVariable String key, HttpServletRequest accessor) {
        if(key == null){
            return Response.failure((Object)null,"订单号不能为空");
        }else{
            houseContainer.removeRetainKey(key);
            return Response.success((Object) null, "移除成功");
        }
    }

    @RequestMapping("/house/getMiniCode")
    @ResponseBody
    public Response getMiniCode(@RequestBody House house, HttpServletRequest accessor) throws IOException {
        House housePrimitive = houseContainer.get(house.getId());
        if(housePrimitive == null){
            housePrimitive = house;
        }
        String token = getToken();
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("path","pages/player/player?houseId="+housePrimitive.getId()+"&housePwd="+housePrimitive.getPassword());
        map.put("width",280);
//        HttpResponse<InputStream> response = null;
        HttpResponse<byte[]> response = null;
        try {
//            response = Unirest.post("https://api.weixin.qq.com/wxa/getwxacode?access_token="+token).body(JSONObject.toJSONString(map)).asBinary();
            response = Unirest.post("https://api.weixin.qq.com/wxa/getwxacode?access_token="+token).body(JSONObject.toJSONString(map)).asBytes();

        } catch (UnirestException e) {
            throw new RuntimeException(e.getMessage());
        }
        return Response.success(inputStreamToBase64(response.getBody()),"获取成功");

    }


//    @RequestMapping("/house/getMiniCode")
//    @ResponseBody
//    public Response getMiniCode(@RequestBody House house, HttpServletRequest accessor) throws IOException {
//        House housePrimitive = houseContainer.get(house.getId());
//        if(housePrimitive == null){
//            housePrimitive = house;
//        }
//        String token = getToken();
//        Map<String,Object> map = new LinkedHashMap<>();
//        map.put("scene","?houseId="+housePrimitive.getId()+"&housePwd="+housePrimitive.getPassword());
//        map.put("page","pages/player/player");
//        map.put("width",280);
//        HttpResponse<InputStream> response = null;
//        try {
//            response = Unirest.post("https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token="+token).body(JSONObject.toJSONString(map)).asBinary();
//        } catch (UnirestException e) {
//            throw new RuntimeException(e.getMessage());
//        }
//        return Response.success(inputStreamToBase64(response.getBody()),"获取成功");
//
//    }

    public static String inputStreamToBase64(InputStream inputStream) throws IOException {
        return Base64.getEncoder().encodeToString(inputStreamToBytes(inputStream));
    }

    public static String inputStreamToBase64(byte[] bytes) throws IOException {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * inputStream 转化成 bytes
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static byte[] inputStreamToBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[4096];
        int index = 0;
        while ((index = inputStream.read(buff, 0, 4096)) > 0) {
            baos.write(buff, 0, index);
        }
        return baos.toByteArray();

    }


        private Token requestToken() {
        try{
            HttpResponse<String> response = Unirest.get("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="+jusicProperties.getMiniId()+"&secret="+jusicProperties.getMiniSecrect()).asString();
            JSONObject jsonObject = JSONObject.parseObject(response.getBody());
            if(jsonObject.containsKey("errcode")){
                throw new RuntimeException("获取小程序accessToken失败"+jsonObject.getString("errcode")+jsonObject.getString("errmsg"));
            }else{
                String accessToken = jsonObject.getString("access_token");
                Long expires = jsonObject.getLong("expires_in");
                Token token = new Token();
                token.setToken(accessToken);
                token.setExpires(expires);
                token.setTokenTime(System.currentTimeMillis());
                return token;
            }
        }catch (UnirestException e){
            throw new RuntimeException(e.getMessage());
        }
    }

    private String getToken() {
        Token token = TOKEN;
        if(token == null || !token.isValid()){
            Token newToken = requestToken();
            token = newToken;
            return newToken.getToken();
        }else {
            return token.getToken();
        }
    }

    @RequestMapping("/house/get")
    @ResponseBody
    public Response get(@RequestBody House house, StompHeaderAccessor accessor) {
        House housePrimitive = houseContainer.get(house.getId());
        House houseSimple = new House();
        if(housePrimitive != null){
            houseSimple.setName(housePrimitive.getName());
            houseSimple.setId(housePrimitive.getId());
            houseSimple.setDesc(housePrimitive.getDesc());
            houseSimple.setCreateTime(housePrimitive.getCreateTime());
            houseSimple.setNeedPwd(housePrimitive.getNeedPwd());
            return Response.success(houseSimple, "房间详情");
        }else{
            return Response.failure(houseSimple, "房间已经不存在了。");
        }
    }


}
