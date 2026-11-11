package com.iare.club;

import com.iare.club.entity.CarouselBanner;
import com.iare.club.entity.AppUser;
import com.iare.club.repository.CarouselBannerRepository;
import com.iare.club.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CarouselBannerFeatureTests {
    @Autowired MockMvc mvc;
    @Autowired CarouselBannerRepository banners;
    @Autowired UserRepository users;

    @BeforeEach void clear() { banners.deleteAll(); }

    @Test void adminCanCreateReorderToggleAndDeleteBanners() throws Exception {
        create("https://example.com/one.jpg");
        create("https://example.com/two.jpg");
        var ordered = banners.findAllByOrderByDisplayOrderAscIdAsc();
        long firstId = ordered.get(0).getId(), secondId = ordered.get(1).getId();

        mvc.perform(post("/admin/carousel-banners/" + secondId + "/move").param("direction", "up")
                        .with(user("admin@gmail.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/carousel-banners/active").with(user("investor@example.com").roles("USER")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$[0].id").value(secondId)).andExpect(jsonPath("$[1].id").value(firstId));

        mvc.perform(post("/admin/carousel-banners/" + secondId + "/toggle")
                        .with(user("admin@gmail.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/carousel-banners/active").with(user("investor@example.com").roles("USER")))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(firstId));

        mvc.perform(post("/admin/carousel-banners/" + firstId + "/delete")
                        .with(user("admin@gmail.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        Assertions.assertFalse(banners.existsById(firstId));
    }

    @Test void endpointsAreRoleProtectedAndInvalidUrlsAreRejected() throws Exception {
        mvc.perform(get("/admin/carousel-banners")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/carousel-banners").with(user("user@example.com").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/carousel-banners/active")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/carousel-banners/active").with(user("admin@gmail.com").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/carousel-banners").with(user("admin@gmail.com").roles("ADMIN")).with(csrf())
                        .param("imageUrl", "data:text/html,bad").param("active", "true"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Enter a valid HTTP or HTTPS image URL")));
        Assertions.assertEquals(0, banners.count());
    }

    @Test void dashboardContainsCarouselAndWalletActions() throws Exception {
        AppUser account = new AppUser(); account.setFullName("Carousel Test User"); account.setEmail("carousel-test@example.com"); account.setPasswordHash("not-used"); users.save(account);
        mvc.perform(get("/dashboard").with(user(account.getEmail()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("id=\"dashboardCarousel\"")))
                .andExpect(content().string(containsString("class=\"dashboard-money-actions\"")));
    }

    private void create(String url) throws Exception {
        mvc.perform(post("/admin/carousel-banners").with(user("admin@gmail.com").roles("ADMIN")).with(csrf())
                        .param("imageUrl", url).param("active", "true"))
                .andExpect(status().is3xxRedirection());
    }
}
