package org.psd.parkingspacedetector.dto.response;

import java.util.List;

public record MediaDTO(
        Long id,
        Long refId,
        String name,
        String path,
        List<ParkingSlotInfoDTO> parkingSlotInfos
) {
}
