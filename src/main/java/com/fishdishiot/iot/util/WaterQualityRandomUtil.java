package com.fishdishiot.iot.util;

import java.util.Random;

public class WaterQualityRandomUtil {
    private static final Random RANDOM = new Random();
    // 上一次的值，初始值可自定义
    private static double lastDissolvedOxygen = 7.0; // mg/L
    private static double lastAmmoniaNitrogen = 0.5; // mg/L
    private static double lastConductivity = 300.0; // μS/cm

    // 溶解氧
    public static double getNextDissolvedOxygen() {
        lastDissolvedOxygen = nextSmoothValue(lastDissolvedOxygen, 6.0, 7.2, 0.05);
        return round(lastDissolvedOxygen, 2);
    }

    // 氨氮
    public static double getNextAmmoniaNitrogen() {
        lastAmmoniaNitrogen = nextSmoothValue(lastAmmoniaNitrogen, 0.45, 0.52, 0.005);
        return round(lastAmmoniaNitrogen, 2);
    }

    // 电导率
    public static double getNextConductivity() {
        lastConductivity = nextSmoothValue(lastConductivity, 280.0, 310.0, 1);
        return round(lastConductivity, 2);
    }


    // 平滑变化的随机数生成
    private static double nextSmoothValue(double last, double min, double max, double maxStep) {
        double step = (RANDOM.nextDouble() * 2 - 1) * maxStep;
        double next = last + step;
        if (next < min) next = min + RANDOM.nextDouble() * (max - min) * 0.1;
        if (next > max) next = max - RANDOM.nextDouble() * (max - min) * 0.1;
        return next;
    }

    private static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
} 