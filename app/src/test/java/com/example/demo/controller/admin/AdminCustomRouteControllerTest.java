package com.example.demo.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.model.CustomRoute;
import com.example.demo.service.CustomRouteService;

@ExtendWith(MockitoExtension.class)
class AdminCustomRouteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomRouteService customRouteService;

    @BeforeEach
    void setUp() {
        AdminCustomRouteController controller = new AdminCustomRouteController(customRouteService);
        ReflectionTestUtils.setField(controller, "customRouteService", customRouteService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldListCustomRoutes() throws Exception {
        CustomRoute route = new CustomRoute();
        route.setSlug("tin-tuc");
        route.setName("Tin tức");
        route.setTargetPath("/blogs/all");
        when(customRouteService.getAllForAdmin()).thenReturn(List.of(route));

        mockMvc.perform(get("/api/admin/custom-routes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("tin-tuc")));
    }

    @Test
    void shouldCreateCustomRoute() throws Exception {
        CustomRoute route = new CustomRoute();
        route.setSlug("tin-tuc");
        route.setName("Tin tức");
        route.setTargetPath("/blogs/all");

        when(customRouteService.create(any(CustomRoute.class))).thenReturn(route);

        mockMvc.perform(post("/api/admin/custom-routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"tin-tuc\",\"name\":\"Tin tức\",\"targetPath\":\"/blogs/all\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetCustomRouteById() throws Exception {
        CustomRoute route = new CustomRoute();
        route.setId(10L);
        route.setSlug("trang-chu-blog");
        route.setName("Trang chủ blog");
        route.setTargetPath("/blogs/all");

        when(customRouteService.getById(10L)).thenReturn(java.util.Optional.of(route));

        mockMvc.perform(get("/api/admin/custom-routes/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("trang-chu-blog")));
    }
}
