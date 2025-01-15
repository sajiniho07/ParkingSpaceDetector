package org.psd.parkingspacedetector.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.psd.parkingspacedetector.configuration.MediaConfig;
import org.psd.parkingspacedetector.dto.response.CoordinatesInfoDTO;
import org.psd.parkingspacedetector.dto.response.GeneralResult;
import org.psd.parkingspacedetector.dto.response.MediaDTO;
import org.psd.parkingspacedetector.dto.response.ParkingSlotInfoDTO;
import org.psd.parkingspacedetector.entity.CoordinatesInfo;
import org.psd.parkingspacedetector.entity.Media;
import org.psd.parkingspacedetector.entity.ParkingSlotInfo;
import org.psd.parkingspacedetector.enums.EnumMediaType;
import org.psd.parkingspacedetector.service.ImageProcessingService;
import org.psd.parkingspacedetector.service.MediaService;

import java.io.File;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
@Slf4j
public class MediaAPI {

    private final MediaService mediaService;
    private final MediaConfig mediaConfig;
    private final ImageProcessingService imageProcessingService;

    @GetMapping("/type/{mediaTypeId}")
    public ResponseEntity<List<MediaDTO>> getTopFiveMediasByTypeId(@PathVariable Long mediaTypeId) {
        List<Media> medias = mediaService.getTopFiveMediasByTypeId(mediaTypeId);
        List<MediaDTO> mediaDTOS = medias.stream()
                .map(this::convertToDto)
                .toList();
        return ResponseEntity.ok(mediaDTOS);
    }

    @GetMapping("/getMediaByRefIdAndMediaTypeId")
    public ResponseEntity<List<MediaDTO>> getMediaByRefIdAndMediaTypeId(@RequestParam Long refId,
                                                                        @RequestParam Long mediaTypeId) {
        List<Media> medias = mediaService.getMediaByRefIdAndMediaTypeId(refId, mediaTypeId);
        List<MediaDTO> mediaDTOS = medias.stream()
                .map(this::convertToDto)
                .toList();
        return ResponseEntity.ok(mediaDTOS);
    }

    @GetMapping("/getParkingSlotDetectorResult")
    public ResponseEntity<GeneralResult> getParkingSlotDetectorResult(@RequestParam Long mediaId,
                                                                      @RequestParam Long modelId) {
        File resultFile = imageProcessingService.getParkingSlotDetectorResult(mediaId, modelId);
        if (resultFile == null || !resultFile.exists()) {
            throw new RuntimeException("Result file generation failed.");
        }
        try {
            Media resultMedia = mediaService.save(resultFile);
            GeneralResult result = new GeneralResult(resultMedia.getId(), resultMedia.getPath(), "Result file generated successfully.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new GeneralResult(-1L, null, "Failed to generate result file: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaDTO> getMediaById(@PathVariable Long id) {
        return mediaService.getMediaById(id)
                .map(this::convertToDtoFullData)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private MediaDTO convertToDto(Media media) {
        return new MediaDTO(
                media.getId(),
                media.getRefId(),
                media.getName(),
                media.getPath(),
                null
        );
    }

    private MediaDTO convertToDtoFullData(Media media) {
        List<ParkingSlotInfoDTO> parkingSlotInfos = media.getParkingSlotInfos().stream()
                .map(this::convertToParkingSlotInfoDto)
                .toList();
        return new MediaDTO(
                media.getId(),
                media.getRefId(),
                media.getName(),
                media.getPath(),
                parkingSlotInfos
        );
    }

    private ParkingSlotInfoDTO convertToParkingSlotInfoDto(ParkingSlotInfo parkingSlotInfo) {
        List<CoordinatesInfoDTO> coordinatesInfos = parkingSlotInfo.getCoordinatesInfos().stream()
                .map(this::convertToCoordinatesInfoDto)
                .toList();

        return new ParkingSlotInfoDTO(
                parkingSlotInfo.getIsEmpty(),
                coordinatesInfos
        );
    }

    private CoordinatesInfoDTO convertToCoordinatesInfoDto(CoordinatesInfo coordinatesInfo) {
        return new CoordinatesInfoDTO(
                coordinatesInfo.getPointOrder(),
                coordinatesInfo.getX(),
                coordinatesInfo.getY(),
                coordinatesInfo.getRealX(),
                coordinatesInfo.getRealY()
        );
    }

    @PostMapping("/upload")
    public ResponseEntity<GeneralResult> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new GeneralResult(-1L, "", "Please select a file to upload."));
        }

        try {
            File destinationFile = new File(mediaConfig.getUploadBaseDir() + File.separator + file.getOriginalFilename());
            file.transferTo(destinationFile);
            Media media = mediaService.save(destinationFile);
            GeneralResult result = new GeneralResult(media.getId(), media.getPath(), "File uploaded successfully.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new GeneralResult(-1L, null, "Failed to upload file: " + e.getMessage()));
        }
    }

    @PostMapping("/upload/{refId}")
    public ResponseEntity<GeneralResult> upload(@RequestParam("file") MultipartFile file, @PathVariable Long refId) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new GeneralResult(-1L, "", "Please select a file to upload."));
        } else if (!isVideoFile(file.getContentType())) {
            return ResponseEntity.badRequest().body(new GeneralResult(-1L, "", "Uploaded file is not a valid video format."));
        }

        try {
            File destinationFile = new File(mediaConfig.getUploadBaseDir() + File.separator + file.getOriginalFilename());
            file.transferTo(destinationFile);
            Media media = mediaService.saveByMediaTypeAndRefId(destinationFile, EnumMediaType.VIDEO, refId);
            GeneralResult result = new GeneralResult(media.getId(), media.getPath(), "File uploaded successfully.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new GeneralResult(-1L, null, "Failed to upload file: " + e.getMessage()));
        }
    }

    private boolean isVideoFile(String contentType) {
        return contentType != null && (contentType.startsWith("video/"));
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<GeneralResult> disableMedia(@PathVariable Long id) {
        boolean isDisabled = mediaService.disableMedia(id);

        if (isDisabled) {
            return ResponseEntity.ok(new GeneralResult(1L, null, "File deleted successfully."));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
