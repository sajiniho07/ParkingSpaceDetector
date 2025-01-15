package org.psd.parkingspacedetector.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PointInfo {
    private Integer id;
    private Double x;
    private Double y;
    private Double realX;
    private Double realY;
}
