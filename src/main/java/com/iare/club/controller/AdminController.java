package com.iare.club.controller;

import com.iare.club.entity.InvestmentProduct;
import com.iare.club.repository.UserRepository;
import com.iare.club.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {
    private final ProductService products; private final UserRepository users;
    public AdminController(ProductService products,UserRepository users){this.products=products;this.users=users;}

    @GetMapping("/money")
    String login(Authentication auth){return isAdmin(auth)?"redirect:/admin/dashboard":"admin/login";}

    @GetMapping("/admin/dashboard")
    String dashboard(Model model){
        var allProducts=products.findAll();model.addAttribute("products",allProducts);model.addAttribute("productCount",(long)allProducts.size());
        model.addAttribute("activeProductCount",allProducts.stream().filter(InvestmentProduct::isActive).count());model.addAttribute("userCount",users.count());return "admin/dashboard";
    }

    @GetMapping("/admin/products/new")
    String createForm(Model model){model.addAttribute("product",new InvestmentProduct());model.addAttribute("editing",false);return "admin/product-form";}

    @GetMapping("/admin/users")
    String users(Model model){var allUsers=users.findAllByOrderByCreatedAtDesc();model.addAttribute("users",allUsers);model.addAttribute("userCount",(long)allUsers.size());return "admin/users";}

    @PostMapping("/admin/products")
    String create(@Valid @ModelAttribute("product") InvestmentProduct product,BindingResult binding,RedirectAttributes flash,Model model){
        if(binding.hasErrors()){model.addAttribute("editing",false);return "admin/product-form";}
        products.save(product);flash.addFlashAttribute("success","Product added successfully.");return "redirect:/admin/dashboard#products";
    }

    @GetMapping("/admin/products/{id}")
    String view(@PathVariable long id,Model model){model.addAttribute("product",products.find(id));return "admin/product-view";}

    @GetMapping("/admin/products/{id}/edit")
    String editForm(@PathVariable long id,Model model){model.addAttribute("product",products.find(id));model.addAttribute("editing",true);return "admin/product-form";}

    @PostMapping("/admin/products/{id}")
    String update(@PathVariable long id,@Valid @ModelAttribute("product") InvestmentProduct product,BindingResult binding,RedirectAttributes flash,Model model){
        if(binding.hasErrors()){product.setId(id);model.addAttribute("editing",true);return "admin/product-form";}
        products.update(id,product);flash.addFlashAttribute("success","Product updated successfully.");return "redirect:/admin/dashboard#products";
    }

    @PostMapping("/admin/products/{id}/delete")
    String delete(@PathVariable long id,RedirectAttributes flash){products.delete(id);flash.addFlashAttribute("success","Product deleted from the catalogue. Existing customer investment records remain protected.");return "redirect:/admin/dashboard#products";}

    @PostMapping("/admin/products/{id}/toggle")
    String toggle(@PathVariable long id,RedirectAttributes flash){
        InvestmentProduct product=products.toggleActive(id);
        flash.addFlashAttribute("success",product.isActive()?"Product activated and is now visible to users.":"Product deactivated and hidden from users.");
        return "redirect:/admin/dashboard#products";
    }

    private boolean isAdmin(Authentication auth){return auth!=null&&auth.isAuthenticated()&&!(auth instanceof AnonymousAuthenticationToken)&&auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));}
}
