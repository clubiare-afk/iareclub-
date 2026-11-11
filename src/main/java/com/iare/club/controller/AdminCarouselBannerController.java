package com.iare.club.controller;

import com.iare.club.entity.CarouselBanner;
import com.iare.club.service.CarouselBannerService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/carousel-banners")
public class AdminCarouselBannerController {
    private final CarouselBannerService banners;

    public AdminCarouselBannerController(CarouselBannerService banners) { this.banners = banners; }

    @GetMapping String list(Model model) {
        model.addAttribute("banners", banners.findAll());
        return "admin/carousel-banners";
    }

    @GetMapping("/new") String createForm(Model model) {
        model.addAttribute("banner", new CarouselBanner());
        model.addAttribute("editing", false);
        return "admin/carousel-banner-form";
    }

    @PostMapping String create(@Valid @ModelAttribute("banner") CarouselBanner banner, BindingResult binding,
                               Model model, RedirectAttributes flash) {
        if (binding.hasErrors()) { model.addAttribute("editing", false); return "admin/carousel-banner-form"; }
        banners.create(banner);
        flash.addFlashAttribute("success", "Carousel banner added successfully.");
        return "redirect:/admin/carousel-banners";
    }

    @GetMapping("/{id}/edit") String editForm(@PathVariable long id, Model model) {
        model.addAttribute("banner", banners.find(id));
        model.addAttribute("editing", true);
        return "admin/carousel-banner-form";
    }

    @PostMapping("/{id}") String update(@PathVariable long id, @Valid @ModelAttribute("banner") CarouselBanner banner,
                                        BindingResult binding, Model model, RedirectAttributes flash) {
        if (binding.hasErrors()) { banner.setId(id); model.addAttribute("editing", true); return "admin/carousel-banner-form"; }
        banners.update(id, banner);
        flash.addFlashAttribute("success", "Carousel banner updated successfully.");
        return "redirect:/admin/carousel-banners";
    }

    @PostMapping("/{id}/toggle") String toggle(@PathVariable long id, RedirectAttributes flash) {
        CarouselBanner banner = banners.toggle(id);
        flash.addFlashAttribute("success", banner.isActive() ? "Banner enabled on the dashboard." : "Banner disabled.");
        return "redirect:/admin/carousel-banners";
    }

    @PostMapping("/{id}/move") String move(@PathVariable long id, @RequestParam String direction, RedirectAttributes flash) {
        banners.move(id, direction);
        flash.addFlashAttribute("success", "Banner order updated.");
        return "redirect:/admin/carousel-banners";
    }

    @PostMapping("/{id}/delete") String delete(@PathVariable long id, RedirectAttributes flash) {
        banners.delete(id);
        flash.addFlashAttribute("success", "Carousel banner deleted successfully.");
        return "redirect:/admin/carousel-banners";
    }
}
