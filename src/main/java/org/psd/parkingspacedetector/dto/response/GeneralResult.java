package org.psd.parkingspacedetector.dto.response;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GeneralResult {
    private Long id;
    private String name;
    private String message;
}