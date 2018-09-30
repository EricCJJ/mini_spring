package com.evanchenjj.spring.demo.service.impl;

import com.evanchenjj.spring.demo.service.IUserService;
import com.evanchenjj.spring.mvcframework.annotation.MiniService;

@MiniService
public class UserServiceImpl implements IUserService {

    @Override
    public void print() {
        System.out.println("service执行...");
    }
}
