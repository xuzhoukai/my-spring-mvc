package com.omen.business.controller;

import com.omen.business.service.UserService;
import com.omen.business.service.UserService2;
import com.omen.framework.annotation.*;
import com.omen.framework.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * @Auther: xuzhoukai
 * @Date: 2019/4/7 19:34
 */
@MyController
public class UserController {

    @MyAutoWired("userService")
    private UserService userService;

    @MyAutoWired
    private UserService2 userService2;

    @MyRequestMapping("/hello.json")
    @MyResponeBody
    public String hello(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("name") String name){
        return "Hello "+ name;
    }

    @MyRequestMapping("/hello2.json")
    @MyResponeBody
    public void hello2(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("name") String name){
        out(response,"hello2 "+name);
    }

    public void out(HttpServletResponse resp,String str){
        try {
            resp.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MyRequestMapping("/hello3.json")
    public ModelAndView hello3(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("name") String name){
        Map<String,Object>modelMap = new HashMap<>();
        modelMap.put("name",name);
        ModelAndView modelAndView = new ModelAndView("template1.myf",modelMap);
        return modelAndView;
    }
}
