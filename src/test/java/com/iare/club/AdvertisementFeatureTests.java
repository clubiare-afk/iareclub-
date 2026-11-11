package com.iare.club;

import com.iare.club.repository.AdvertisementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdvertisementFeatureTests {
    @Autowired MockMvc mvc;
    @Autowired AdvertisementRepository advertisements;

    @BeforeEach
    void clearAdvertisements() {
        advertisements.deleteAll();
    }

    @Test
    void adminCanManageAdvertisementAndUserCanFetchLatestActive() throws Exception {
        mvc.perform(post("/admin/advertisements")
                        .with(user("admin@gmail.com").roles("ADMIN")).with(csrf())
                        .param("imageUrl", "https://example.com/campaign.jpg")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/advertisements"));

        long id = advertisements.findAllByOrderByUpdatedAtDesc().get(0).getId();
        mvc.perform(get("/api/advertisements/active").with(user("investor@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/campaign.jpg"));

        mvc.perform(post("/admin/advertisements/" + id + "/toggle")
                        .with(user("admin@gmail.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/advertisements/active").with(user("investor@example.com").roles("USER")))
                .andExpect(status().isNoContent());

        mvc.perform(post("/admin/advertisements/" + id + "/delete")
                        .with(user("admin@gmail.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertFalse(advertisements.existsById(id));
    }

    @Test
    void advertisementEndpointsAreRoleProtectedAndInvalidUrlsAreRejected() throws Exception {
        mvc.perform(get("/admin/advertisements")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/advertisements").with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/advertisements/active"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/advertisements/active").with(user("admin@gmail.com").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/admin/advertisements")
                        .with(user("admin@gmail.com").roles("ADMIN")).with(csrf())
                        .param("imageUrl", "javascript:void(0)")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a valid HTTP or HTTPS image URL")));
        org.junit.jupiter.api.Assertions.assertEquals(0, advertisements.count());
    }
}
