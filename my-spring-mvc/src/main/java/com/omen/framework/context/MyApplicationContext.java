package com.omen.framework.context;

import com.omen.framework.annotation.MyAutoWired;
import com.omen.framework.annotation.MyController;
import com.omen.framework.annotation.MyService;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:
 * @Auther: xuzhoukai
 * @Date: 2019/4/7 16:31
 */
public class MyApplicationContext {
    private Map<String,Object> instanceMapping = new ConcurrentHashMap<>();

    private List<String> classCache = new ArrayList<>();

    private Properties config = new Properties();

    public Properties getConfig() {
        return config;
    }

    public Map<String,Object> findAll(){
        return instanceMapping;
    }

    public MyApplicationContext(String location) {
        //定位 载入 注册 初始化 注入
        InputStream is = null;
        try {
            // 定位
            is = this.getClass().getClassLoader().getResourceAsStream(location);
            // 载入
            config = new Properties();
            config.load(is);

            // 注册
            String packageName = config.getProperty("scanPackage");
            doRegister(packageName);

            //初始化
            doCreateBean();
            //注入
            populate();

        }catch (Exception e){

        }
    }

    private void doRegister(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/"+packageName.replaceAll("\\.","/"));
        File dir = new File(url.getFile());
        for (File file:dir.listFiles()) {
            if(file.isDirectory()){
                doRegister(packageName+"/"+file.getName());
            }else{
                classCache.add(packageName.replace("/",".")+"."+file.getName().replace(".class",""));
            }
        }
    }

    /**
     * 创建bean
     */
    private void doCreateBean() {
        if(classCache.isEmpty()){
            return;
        }
        try {
            for (String className:classCache) {
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(MyController.class)){
                    instanceMapping.put(lowerFirstName(clazz.getSimpleName()),clazz.newInstance());
                }else if(clazz.isAnnotationPresent(MyService.class)){
                    MyService myService = clazz.getAnnotation(MyService.class);
                    String id = myService.value();
                    if(!"".equals(id.trim())){
                        instanceMapping.put(id,clazz.newInstance());
                    }
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for(Class i:interfaces){
                        instanceMapping.put(i.getName(),clazz.newInstance());
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void populate() {
        if(classCache.isEmpty()){
            return;
        }
        for(Map.Entry<String,Object> entry:instanceMapping.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field :fields){
                if(!field.isAnnotationPresent(MyAutoWired.class)){
                    continue;
                }
                MyAutoWired myAutoWired = field.getAnnotation(MyAutoWired.class);
                String id = myAutoWired.value().trim();
                if("".equals(id)){
                    id = field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),instanceMapping.get(id));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String lowerFirstName(String name){
        char[] chars = name.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    public Map<String,Object> getAll() {
        return instanceMapping;
    }
}
