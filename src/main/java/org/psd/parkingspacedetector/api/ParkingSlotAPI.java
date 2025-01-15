package org.psd.parkingspacedetector.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.psd.parkingspacedetector.dto.request.ParkingTrainingRequest;
import org.psd.parkingspacedetector.dto.response.GeneralResult;
import org.psd.parkingspacedetector.entity.Media;
import org.psd.parkingspacedetector.enums.EnumMediaType;
import org.psd.parkingspacedetector.service.ImageProcessingService;
import org.psd.parkingspacedetector.service.MediaService;
import org.psd.parkingspacedetector.service.ParkingSlotService;

import java.io.File;

@RestController
@RequiredArgsConstructor
@RequestMapping("/parking-slot")
@Slf4j
public class ParkingSlotAPI {

    private final ParkingSlotService parkingSlotService;
    private final MediaService mediaService;
    private final ImageProcessingService imageProcessingService;

    @PostMapping("/generateModelFile")
    public ResponseEntity<GeneralResult> generateModelFile(@RequestBody ParkingTrainingRequest request) {
        try {
            String mediaOriginalPath = parkingSlotService.saveParkingSlotInfos(request);
            File modelFile = imageProcessingService.generateModelFile(request, mediaOriginalPath);
            if (modelFile == null || !modelFile.exists()) {
                throw new RuntimeException("Model file generation failed.");
            }
            Media trainArffMedia = mediaService.saveByMediaTypeAndRefId(modelFile, EnumMediaType.MODEL, request.getMediaId());
            GeneralResult result = new GeneralResult(trainArffMedia.getId(), trainArffMedia.getPath(), "Parking model generated successfully.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error generating model file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GeneralResult(null, null, "Error generating model file: " + e.getMessage()));
        }
    }

}
