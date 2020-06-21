package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.SessionRepository;
import com.scoder.jusic.util.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author JumpAlang
 * @create 2020-06-22 1:15
 */
@Controller
@Slf4j
@CrossOrigin
public class UserController {
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private HouseContainer houseContainer;
    @RequestMapping("/user/get")
    @ResponseBody
    public Response getUsers(@RequestBody User user, HttpServletRequest accessor) {
        //TODO 权限认证
        String ip = IPUtils.getRemoteAddress(accessor);
        if("127.0.0.1|localhost|140.243.217.20|0:0:0:0:0:0:0:1".indexOf(ip) != -1){
            if(user.getHouseId() == null || "".equals(user.getHouseId())){
                List<House> houses = houseContainer.getHouses();
                Map<String,List> houseUser = new HashMap<>();
                for(House house : houses){
                    houseUser.put(house.getName()+":"+house.getId(),sessionRepository.getSession(house.getId()));
                }
                return Response.success(  houseUser, "获取用户成功");
            }else{
                return Response.success(  sessionRepository.getSession(user.getHouseId()), "获取用户成功");
            }
        }else{
            return Response.failure((Objects)null,"没有权限");
        }

    }
}
