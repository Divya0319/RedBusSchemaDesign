package com.fastturtle.redbusschemadesign.controllers;

import com.fastturtle.redbusschemadesign.services.BusService;
import com.fastturtle.redbusschemadesign.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/booking/bus")
public class BusController {

    private BusService busService;
    private UserService userService;

    @Autowired
    public BusController(BusService busService, UserService userService) {
        this.busService = busService;
        this.userService = userService;
    }

    @GetMapping("/availableOnRoute")
    public ResponseEntity<?> getAvailableBusesOnRoute(@RequestParam("source") String source, @RequestParam("destination") String destination) {
        return busService.getAvailableBusesOnRoute(source, destination);
    }
}
