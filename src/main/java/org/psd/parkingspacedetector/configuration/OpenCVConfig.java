package org.psd.parkingspacedetector.configuration;

import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OpenCVConfig {
    public OpenCVConfig() {
        OpenCV.loadLocally();
        log.info("OpenCV Native Library loaded successfully.");
    }

}
