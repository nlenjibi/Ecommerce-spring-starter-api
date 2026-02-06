package com.smart_ecomernce_api.smart_ecomernce_api.modules.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CommonController {

    @GetMapping("/help-support/settings")
    public Map<String, String> getHelpSupportSettings() {
        return Collections.singletonMap("message", "Help and support settings endpoint");
    }

    @GetMapping("/social-links")
    public Map<String, String> getSocialLinks() {
        return Collections.singletonMap("message", "Social links endpoint");
    }

    @GetMapping("/app-download-links")
    public Map<String, String> getAppDownloadLinks() {
        return Collections.singletonMap("message", "App download links endpoint");
    }
}
