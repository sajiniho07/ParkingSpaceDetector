package org.psd.parkingspacedetector.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class MediaConfig {

    @Value("${media.url}")
    private String mediaUrl;

    @Value("${media.upload-base-dir}")
    private String uploadBaseDir;
}
