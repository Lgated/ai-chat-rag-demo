package com.example.demo.service.Impl;

import com.example.demo.service.Tool;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool implements Tool {

    @Override
    public String getName() {
        return "get_weather";
    }

    @Override
    public String getDescription() {
        return "获取指定城市的天气信息。输入格式：城市名称";
    }

    @Override
    public String execute(String input) {
        //TODO:调用真实的天气 API

        // 模拟天气查询
        return String.format("%s 的天气：晴天，温度 25°C", input);
    }
}
