package com.csye6300.group1.pricingengine.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoDataController {

    private final DemoDataSeeder demoDataSeeder;

    public DemoDataController(DemoDataSeeder demoDataSeeder) {
        this.demoDataSeeder = demoDataSeeder;
    }

    @PostMapping("/seed")
    public Map<String, Object> seed() {
        List<String> skus = demoDataSeeder.seed();
        return Map.of("seeded", skus);
    }
}
