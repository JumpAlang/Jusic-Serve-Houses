package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import com.scoder.jusic.util.IPUtils;
import com.scoder.jusic.util.UUIDUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin
public class HomeController {
    @Autowired
    private SessionService sessionService;
    @Autowired
    private HouseContainer houseContainer;
    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private MusicService musicService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private ConfigRepository configRepository;

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
            if(matchHouse.getNeedPwd() &&  (!matchHouse.getPassword().equals(house.getPassword()) || matchHouse.getSessionId().equals(sessionId))){
             return Response.failure((Object) null, "请输入正确的房间密码");
            }
        }
       return Response.success(sessionId, "进入房间成功");
    }


    @RequestMapping("/house/search")
    @ResponseBody
    public Response searchHouse(HttpServletRequest accessor) {
        String sessionId = UUIDUtils.getUUID8Len(accessor.getSession().getId());
        CopyOnWriteArrayList<House> houses = houseContainer.getHouses();
        ArrayList<House> housesSimple = new ArrayList<>();
        for(House house : houses){
            House houseSimple = new House();
            houseSimple.setName(house.getName());
            houseSimple.setId(house.getId());
            houseSimple.setDesc(house.getDesc());
            houseSimple.setCreateTime(house.getCreateTime());
            houseSimple.setNeedPwd(house.getNeedPwd());
            housesSimple.add(houseSimple);
        }
        return Response.success(housesSimple, "房间列表");
    }

    /**
     * 房间留存与否
     * @param accessor
     */
    @MessageMapping("/house/retain/{retain}")
    public void houseRetain(@DestinationVariable boolean retain, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String)accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId,houseId);
        if (!"root".equals(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"),houseId);
        } else {
            houseContainer.get(houseId).setEnableStatus(retain);
            sessionService.send(sessionId,MessageType.NOTICE, Response.success((Object) null, "设置房间留存与否成功"),houseId);
        }
    }

}
