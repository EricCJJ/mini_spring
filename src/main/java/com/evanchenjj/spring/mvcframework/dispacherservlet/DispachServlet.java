package com.evanchenjj.spring.mvcframework.dispacherservlet;

import com.evanchenjj.spring.mvcframework.annotation.MiniAutowired;
import com.evanchenjj.spring.mvcframework.annotation.MiniController;
import com.evanchenjj.spring.mvcframework.annotation.MiniRequestMapping;
import com.evanchenjj.spring.mvcframework.annotation.MiniService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;


public class DispachServlet extends HttpServlet {
    private Properties properties = new Properties();
    private List<String> classNameList = new ArrayList<>();
    private Map<String, Object> ioc = new HashMap<>();
    private Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispach(req, resp);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void doDispach(HttpServletRequest req, HttpServletResponse resp) throws InvocationTargetException, IllegalAccessException {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        uri = uri.replace(contextPath, "");
        if (handlerMapping.containsKey(uri)) {
            Method method = handlerMapping.get(uri);
            String name = lowerFirstCase(method.getDeclaringClass().getSimpleName());
            Object o = ioc.get(name);
            Map<String, String[]> params = req.getParameterMap();
            method.invoke(o, new Object[]{resp, params.get("name")[0]});
        }

    }


    @Override
    public void init(ServletConfig config) throws ServletException {
//        1.加载配置文件
        String contextConfig = config.getInitParameter("contextConfig");
        doLoadConfig(contextConfig);

//        2.读取配置文件，扫描所有的文件

        doScanner(properties.getProperty("scannerPackage"));

//        3.对加了相应注解的类进行实例化
        doInstance();
//        4.进行自动注入
        doAutowires();
//        5.关联相关映射，即handlermapping
        doHandlerMapping();
    }

    private void doHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        String baseUrl = "";
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (clazz.isAnnotationPresent(MiniController.class)) {
                MiniRequestMapping requestMapping = clazz.getAnnotation(MiniRequestMapping.class);
                baseUrl = requestMapping.value().trim();

            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(MiniRequestMapping.class)) {
                    MiniRequestMapping requestMapping = method.getAnnotation(MiniRequestMapping.class);
                    String url = (baseUrl + "/" + requestMapping.value().trim()).replaceAll("/+", "/");
                    handlerMapping.put(url, method);
                    System.out.println("mapped: url " + url + " method: " + method);
                }
            }
        }
    }

    private void doAutowires() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(MiniAutowired.class)) {
                    MiniAutowired autowired = field.getAnnotation(MiniAutowired.class);
                    String beanName = autowired.value().trim();
                    if (beanName.equals("")) {
                        beanName = field.getType().getName();
                    }
                    field.setAccessible(true);
                    try {
                        field.set(entry.getValue(), ioc.get(beanName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    private void doInstance() {
        if (classNameList.isEmpty()) {
            return;
        }
        for (String className : classNameList) {
            try {
                Class<?> clazz = Class.forName(className);
//                判断是否加了MiniController或者MiniService注解
                if (clazz.isAnnotationPresent(MiniController.class)) {
//                    因为一般controller上不加了value值，这里也就不获取value值了，直接采用类名首字母小写；
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    if (ioc.containsKey(beanName)) {
                        throw new Exception("bean name has exist");
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(MiniService.class)) {
//                    获取service上注解的值
                    MiniService service = clazz.getAnnotation(MiniService.class);
                    String beanName = service.value().trim();
                    if (beanName.equals("")) {
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }
                    if (ioc.containsKey(beanName)) {
                        throw new Exception("bean name has exis");
                    }
                    ioc.put(beanName, clazz.newInstance());

//                    如果没写value值，MiniAutowired是按类型注入的，光放入类名小写可能娶不到值
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        ioc.put(anInterface.getName(), clazz.newInstance());
                    }

                } else {
                    continue;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    //  首字母+32即可，利用大小写数值差
    private String lowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String scannerPackage) {
        URL url = this.getClass().getClassLoader().getResource(scannerPackage.replaceAll("\\.", "/"));
        File classDir = new File(url.getFile());
        File[] files = classDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                doScanner(scannerPackage + "." + file.getName());
            } else {
                String className = scannerPackage + "." + file.getName().replace(".class", "");
                classNameList.add(className);
            }
        }

    }

    private void doLoadConfig(String contextConfig) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfig);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null == inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
