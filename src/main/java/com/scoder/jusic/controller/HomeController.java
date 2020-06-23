package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.util.IPUtils;
import com.scoder.jusic.util.UUIDUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
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

    @RequestMapping("/house/add")
    @ResponseBody
    public Response addHouse(@RequestBody House house, HttpServletRequest accessor) {
        String sessionId = UUIDUtils.getUUID8Len(accessor.getSession().getId());
        if(house.getName() == null || house.getName() == ""){
           return Response.failure((Object) null, "房间名称不能为空");
        }
        if(house.getNeedPwd() != null && house.getNeedPwd() && (house.getPassword() == null || house.getPassword() == "")){
            return Response.failure((Object) null, "房间密码不能为空");
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
        house.setId(sessionId);
        house.setCreateTime(System.currentTimeMillis());
        house.setEnableStatus(false);
        house.setSessionId(sessionId);
        house.setRemoteAddress(ip);
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
            return Response.success(housePrimitive, "修改房间成功");
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
