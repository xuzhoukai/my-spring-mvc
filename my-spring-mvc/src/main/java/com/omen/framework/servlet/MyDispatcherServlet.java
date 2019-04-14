package com.omen.framework.servlet;

import com.omen.framework.annotation.MyController;
import com.omen.framework.annotation.MyRequestMapping;
import com.omen.framework.annotation.MyRequestParam;
import com.omen.framework.context.MyApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @Description:
 * @Auther: xuzhoukai
 * @Date: 2019/4/7 16:36
 */
public class MyDispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = -2687870511000303221L;

    private final String localtion = "contextConfigLocation";

    private Map<String,Handler> handlerMapping = new HashMap<>();

    private Map<Handler,HandlerAdapter> adapterMap = new HashMap<>();

    private List<ViewResolver> viewResolvers = new ArrayList<>();

    Logger logger = Logger.getLogger(MyDispatcherServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        }catch (Exception e){
            resp.getWriter().write("500 Exception :" +e.getLocalizedMessage());
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        logger.info("MyDispatcherServlet init");
        String configLocation =config.getInitParameter(localtion);
        // 初始化ioc容器
        MyApplicationContext context = new MyApplicationContext(configLocation);
//        Map<String,Object> ioc = context.getAll();
//        System.out.println(ioc.get("userController"));
//        UserController userController = (UserController) ioc.get("userController");
//        System.out.println(userController);
        //
        initHandlerMappings(context);

        initHandlerAdapters(context);

        initViewResolvers(context);

    }

    /**
     * 视图渲染
     * @param context
     */
    private void initViewResolvers(MyApplicationContext context) {
        //获取模板文件路径
        String templateRoot = context.getConfig().getProperty("template");
        String dir = this.getClass().getClassLoader().getResource(templateRoot).getFile();
        File dirFile = new File(dir);
        for(File file :dirFile.listFiles()){
            ViewResolver view = new ViewResolver(file.getName(),file);
            viewResolvers.add(view);
        }
    }

    /**
     * 方法参数设定
     * @param context
     */
    private void initHandlerAdapters(MyApplicationContext context) {
        if(handlerMapping.isEmpty()){
            return;
        }
        // 参数的类型为key index为值
        Map<String,Integer> paramMapping = new HashMap<>();
        for(Map.Entry<String,Handler>entry:handlerMapping.entrySet()){
            Handler handler =entry.getValue();
            Class<?>[] paramsTypes = handler.method.getParameterTypes();
            // 参数有顺序 反射无法拿到参数的名字
            for(int i = 0;i< paramsTypes.length;i++){
                Class<?> type = paramsTypes[i];
//                paramMapping.put(type.getName(),i);
                if(type == HttpServletRequest.class || type == HttpServletResponse.class){
                    paramMapping.put(type.getName(),i);
                    continue;
                }
            }
            Annotation[][] annos = handler.method.getParameterAnnotations();
            for(int i = 0;i<annos.length;i++){
                for(Annotation a:annos[i]){
                    if(a instanceof MyRequestParam){
                        String param = ((MyRequestParam) a).value();
                        if(!"".equals(param)){
                            paramMapping.put(param,i);
                        }
                    }
                }
            }
            adapterMap.put(handler,new HandlerAdapter(paramMapping));
        }

    }

    /**
     * 方法和请求路径绑定
     * @param context
     */
    private void initHandlerMappings(MyApplicationContext context) {
        Map<String,Object>ioc = context.findAll();
        if(ioc.isEmpty()){
            return;
        }
        for(Map.Entry<String,Object> entry:ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(MyController.class)){
                return;
            }
            String url = "";
            if(clazz.isAnnotationPresent(MyRequestMapping.class)){
               MyRequestMapping myRequestMapping = clazz.getAnnotation(MyRequestMapping.class);
               url+=myRequestMapping.value();
            }

            Method[] methods = clazz.getMethods();
            for(Method method :methods){
                if(!method.isAnnotationPresent(MyRequestMapping.class)){
                    continue;
                }
                MyRequestMapping myRequestMapping = method.getAnnotation(MyRequestMapping.class);
                String mappingUrl = url+ myRequestMapping.value();
                handlerMapping.put(mappingUrl,new Handler(entry.getValue(),method));
            }
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        Handler handler = getHandler(req);
        if(handler == null){
            resp.getWriter().write("404 Not Found");
        }
        HandlerAdapter handlerAdapter = getHandlerAdapter(handler);
        ModelAndView modelAndView = handlerAdapter.handle(req,resp,handler);
        applyDefaultView(resp,modelAndView);

    }

    /**
     * 解析视图
     */
    private void applyDefaultView(HttpServletResponse resp,ModelAndView modelAndView) throws IOException {
        if(null == modelAndView){
            return;
        }
        for(ViewResolver viewResolver :viewResolvers){
            if(!viewResolver.viewName.equals(modelAndView.getViewName())){
                continue;
            }
            String result = viewResolver.paser();
            for(Map.Entry<String,Object> entry:modelAndView.getModelMap().entrySet()){
                result = result.replaceAll("@\\{"+entry.getKey()+"}", (String) entry.getValue());
            }
            resp.getWriter().write(result);
            break;
        }
    }

    private Handler getHandler(HttpServletRequest req) {
        if(handlerMapping.isEmpty()){
            return null;
        }
        String uri = req.getRequestURI();
        String context = req.getContextPath();
        uri = uri.replaceAll(context,"");
        return handlerMapping.get(uri);
    }
    private HandlerAdapter getHandlerAdapter(Handler handler){
        if(adapterMap.containsKey(handler)){
            return adapterMap.get(handler);
        }
        return null;
    }

    /**
     * 方法
     */
    class Handler{
        protected Object controller;
        protected Method method;
        public Handler(Object controller, Method method) {
            this.controller = controller;
            this.method = method;
        }
    }

    /**
     * 方法适配器
     */
    class HandlerAdapter{
        private Map<String, Integer> paramMapping;
        public HandlerAdapter(Map<String, Integer> paramMapping) {
            this.paramMapping = paramMapping;
        }

        public ModelAndView handle(HttpServletRequest req, HttpServletResponse resp, Handler handler) throws InvocationTargetException, IllegalAccessException {
            Class<?>[] paramTypes = handler.method.getParameterTypes();
            Object[] paramValues = new Object[paramTypes.length];
            Map<String,String[]> map = req.getParameterMap();
            for(Map.Entry<String,String[]> param:map.entrySet()){
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","")
                        .replaceAll(",\\s",",");
                if(!this.paramMapping.containsKey(param.getKey())){
                    continue;
                }
                int index = this.paramMapping.get(param.getKey());
                paramValues[index] = castStringValue(value,paramTypes[index]);
            }
            if(this.paramMapping.containsKey(HttpServletRequest.class.getName())){
                int reqIndex = this.paramMapping.get(HttpServletRequest.class.getName());
                paramValues[reqIndex] = req;
            }
            if(this.paramMapping.containsKey(HttpServletResponse.class.getName())){
                int respIndex = this.paramMapping.get(HttpServletResponse.class.getName());
                paramValues[respIndex] = resp;
            }
            Class returnType = handler.method.getReturnType();
            boolean isModelAndView = returnType == ModelAndView.class;
            Object result = handler.method.invoke(handler.controller,paramValues);
            if(isModelAndView){
                return (ModelAndView)result;
            }
            return null;
        }
    }

    private Object castStringValue(String value,Class<?> clazz){
        if(clazz == String.class){
            return value;
        }else if(clazz == Integer.class){
            return Integer.valueOf(value);
        }else if(clazz == int.class){
            return Integer.valueOf(value).intValue();
        }else{
            return null;
        }
    }

    /**
     * 视图
     */
    private class ViewResolver{
        /**模板名称*/
        private String viewName;
        /**模板文件*/
        private File file;

        public ViewResolver(String viewName, File file) {
            this.viewName = viewName;
            this.file = file;
        }

        public String getViewName() {
            return viewName;
        }

        public void setViewName(String viewName) {
            this.viewName = viewName;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        /**
         * 解析视图
         * @return
         */
        public String paser() throws IOException {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file,"r");
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = randomAccessFile.readLine())!=null){
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
