package org.psd.parkingspacedetector.dto.response;

public record CoordinatesInfoDTO(
        Integer pointOrder,
        Double x,
        Double y,
        Double realX,
        Double realY) {
}
