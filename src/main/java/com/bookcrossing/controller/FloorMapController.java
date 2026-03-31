package com.bookcrossing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class FloorMapController {

    // Этажи: номер -> подпись
    private static final Map<Integer, String> FLOORS = new LinkedHashMap<>();

    // fl_4 — .jpg, все остальные — .png
    private static final int JPG_FLOOR = 4;

    static {
        for (int i = 2; i <= 9; i++) {
            FLOORS.put(i, i + " этаж");
        }
    }

    @GetMapping("/floor-map")
    public String floorMap(
            @RequestParam(defaultValue = "2") int floor,
            Model model) {

        // Защита от недопустимого значения
        if (floor < 2 || floor > 9) {
            floor = 2;
        }

        String extension = (floor == JPG_FLOOR) ? ".jpg" : ".png";
        String imageUrl = "/images/floar/fl_" + floor + extension;

        model.addAttribute("floors", FLOORS);
        model.addAttribute("selectedFloor", floor);
        model.addAttribute("imageUrl", imageUrl);

        return "floor-map";
    }
}