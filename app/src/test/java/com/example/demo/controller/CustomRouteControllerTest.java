package com.example.demo.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.model.CustomRoute;
import com.example.demo.service.CustomRouteService;

@ExtendWith(MockitoExtension.class)
class CustomRouteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomRouteService customRouteService;

    @BeforeEach
    void setUp() {
        CustomRouteController controller = new CustomRouteController();
        ReflectionTestUtils.setField(controller, "customRouteService", customRouteService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldRedirectToConfiguredTargetForVisibleRoute() throws Exception {
        CustomRoute route = new CustomRoute();
        route.setSlug("tin-tuc");
        route.setTargetPath("/blogs/all");
        route.setVisible(true);

        when(customRouteService.findVisibleBySlug("tin-tuc")).thenReturn(Optional.of(route));

        mockMvc.perform(get("/custom-routes/tin-tuc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/blogs/all"));
    }

    @Test
    void shouldNormalizeRelativeTargetPathToAbsolutePath() throws Exception {
        CustomRoute route = new CustomRoute();
        route.setSlug("trang-chu-blog");
        route.setTargetPath("blogs/all");
        route.setVisible(true);

        when(customRouteService.findVisibleBySlug("trang-chu-blog")).thenReturn(Optional.of(route));

        mockMvc.perform(get("/custom-routes/trang-chu-blog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/blogs/all"));
    }
}
