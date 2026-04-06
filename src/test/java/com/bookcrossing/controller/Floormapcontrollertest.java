package com.bookcrossing.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FloorMapController")
class FloorMapControllerTest {

    @InjectMocks FloorMapController floorMapController;

    private Model model() { return mock(Model.class); }

    @Test
    @DisplayName("floor=2 — .png, возвращает floor-map")
    void floor2_png() {
        Model m = model();
        String view = floorMapController.floorMap(2, m);

        assertThat(view).isEqualTo("floor-map");
        verify(m).addAttribute(eq("selectedFloor"), eq(2));
        verify(m).addAttribute(eq("imageUrl"), eq("/images/floar/fl_2.png"));
    }

    @Test
    @DisplayName("floor=4 — .jpg (единственный JPG)")
    void floor4_jpg() {
        Model m = model();
        floorMapController.floorMap(4, m);

        verify(m).addAttribute(eq("imageUrl"), eq("/images/floar/fl_4.jpg"));
    }

    @Test
    @DisplayName("floor=9 — .png")
    void floor9_png() {
        Model m = model();
        floorMapController.floorMap(9, m);

        verify(m).addAttribute(eq("imageUrl"), eq("/images/floar/fl_9.png"));
    }

    @Test
    @DisplayName("floor=1 (< 2) — нормализуется до 2")
    void floorTooLow_normalizesTo2() {
        Model m = model();
        floorMapController.floorMap(1, m);

        verify(m).addAttribute(eq("selectedFloor"), eq(2));
        verify(m).addAttribute(eq("imageUrl"), eq("/images/floar/fl_2.png"));
    }

    @Test
    @DisplayName("floor=10 (> 9) — нормализуется до 2")
    void floorTooHigh_normalizesTo2() {
        Model m = model();
        floorMapController.floorMap(10, m);

        verify(m).addAttribute(eq("selectedFloor"), eq(2));
    }

    @Test
    @DisplayName("floors добавляется в модель")
    void floors_addedToModel() {
        Model m = model();
        floorMapController.floorMap(3, m);

        verify(m).addAttribute(eq("floors"), any());
    }
}