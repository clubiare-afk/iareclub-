package com.iare.club.controller;

import com.iare.club.entity.Advertisement;
import com.iare.club.service.AdvertisementService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/advertisements")
public class AdminAdvertisementController {
    private final AdvertisementService advertisements;

    public AdminAdvertisementController(AdvertisementService advertisements) {
        this.advertisements = advertisements;
    }

    @GetMapping
    String list(Model model) {
        model.addAttribute("advertisements", advertisements.findAll());
        return "admin/advertisements";
    }

    @GetMapping("/new")
    String createForm(Model model) {
        model.addAttribute("advertisement", new Advertisement());
        model.addAttribute("editing", false);
        return "admin/advertisement-form";
    }

    @PostMapping
    String create(@Valid @ModelAttribute Advertisement advertisement, BindingResult binding, Model model,
                  RedirectAttributes flash) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", false);
            return "admin/advertisement-form";
        }
        advertisements.create(advertisement);
        flash.addFlashAttribute("success", "Advertisement created successfully.");
        return "redirect:/admin/advertisements";
    }

    @GetMapping("/{id}/edit")
    String editForm(@PathVariable long id, Model model) {
        model.addAttribute("advertisement", advertisements.find(id));
        model.addAttribute("editing", true);
        return "admin/advertisement-form";
    }

    @PostMapping("/{id}")
    String update(@PathVariable long id, @Valid @ModelAttribute Advertisement advertisement,
                  BindingResult binding, Model model, RedirectAttributes flash) {
        if (binding.hasErrors()) {
            advertisement.setId(id);
            model.addAttribute("editing", true);
            return "admin/advertisement-form";
        }
        advertisements.update(id, advertisement);
        flash.addFlashAttribute("success", "Advertisement updated successfully.");
        return "redirect:/admin/advertisements";
    }

    @PostMapping("/{id}/toggle")
    String toggle(@PathVariable long id, RedirectAttributes flash) {
        Advertisement advertisement = advertisements.toggle(id);
        flash.addFlashAttribute("success", advertisement.isActive()
                ? "Advertisement enabled and visible on user dashboards."
                : "Advertisement disabled.");
        return "redirect:/admin/advertisements";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable long id, RedirectAttributes flash) {
        advertisements.delete(id);
        flash.addFlashAttribute("success", "Advertisement deleted successfully.");
        return "redirect:/admin/advertisements";
    }
}
