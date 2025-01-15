package org.psd.parkingspacedetector.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ParkingSlotDetail {
    private Integer index;
    private Boolean isEmpty;
    private List<PointInfo> points;
}
