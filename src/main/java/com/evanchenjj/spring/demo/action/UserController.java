package com.evanchenjj.spring.demo.action;

import com.evanchenjj.spring.demo.service.IUserService;
import com.evanchenjj.spring.mvcframework.annotation.MiniAutowired;
import com.evanchenjj.spring.mvcframework.annotation.MiniController;
import com.evanchenjj.spring.mvcframework.annotation.MiniRequestMapping;
import com.evanchenjj.spring.mvcframework.annotation.MiniRequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MiniController
@MiniRequestMapping("/user")
public class UserController {

    @MiniAutowired
    private IUserService userService;


    @MiniRequestMapping("/add")
    public void add(HttpServletResponse resp, @MiniRequestParam("name") String name) throws IOException {
        userService.print();
        resp.getWriter().println(name+"add方法执行.....");
    }

    @MiniRequestMapping("/delete")
    public void delete(HttpServletResponse resp, @MiniRequestParam("name") String name) throws IOException {
        userService.print();
        resp.getWriter().println(name+"delete方法执行.....");
    }

}
