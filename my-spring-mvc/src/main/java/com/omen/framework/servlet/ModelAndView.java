package com.omen.framework.servlet;

import java.util.Map;

/**
 * @Description:
 * @Auther: xuzhoukai
 * @Date: 2019/4/14 22:21
 */
public class ModelAndView {
    private String viewName;
    private Map<String,Object>modelMap = null;

    public ModelAndView(String viewName, Map<String, Object> modelMap) {
        this.viewName = viewName;
        this.modelMap = modelMap;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Map<String, Object> getModelMap() {
        return modelMap;
    }

    public void setModelMap(Map<String, Object> modelMap) {
        this.modelMap = modelMap;
    }
}
