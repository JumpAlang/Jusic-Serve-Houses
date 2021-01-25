package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.service.MusicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author JumpAlang
 * @create 2021-01-24 22:51
 */
@Controller
@Slf4j
public class NetEaseController {

    @Autowired
    private MusicService musicService;

    @RequestMapping("/netease/loginByPhone")
    @ResponseBody
    public Response loginByPhone(String phone, @RequestParam(required = false) String pwd, @RequestParam(required = false) String md5Pwd, @RequestParam(required = false) String countryCode) {
        String rs = musicService.netEaseLoginByPhone(phone,pwd,md5Pwd,countryCode);
        return Response.success(rs);
    }

    @RequestMapping("/netease/loginByEmail")
    @ResponseBody
    public Response loginByEmail(String email, @RequestParam(required = false) String pwd, @RequestParam(required = false) String md5Pwd) {

        return Response.success(musicService.netEaseLoginByEmail(email,pwd,md5Pwd));
    }

    @RequestMapping("/netease/loginRefresh")
    @ResponseBody
    public Response loginRefresh() {
        musicService.netEaseLoginRefresh();
        return Response.success();
    }

    @RequestMapping("/netease/setCookie")
    @ResponseBody
    public Response setCookie(String cookie) {
        musicService.setNetEaseCookie(cookie);
        return Response.success();
    }
}
