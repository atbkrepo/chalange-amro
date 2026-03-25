package com.xyz.orders.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class LoggedOutController {

    @GetMapping("/logged-out")
    public RedirectView loggedOut() {
        return new RedirectView("/logged-out.html");
    }
}
