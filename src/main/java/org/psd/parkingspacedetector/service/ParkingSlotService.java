package org.psd.parkingspacedetector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.psd.parkingspacedetector.dto.request.ParkingSlotDetail;
import org.psd.parkingspacedetector.dto.request.ParkingTrainingRequest;
import org.psd.parkingspacedetector.dto.request.PointInfo;
import org.psd.parkingspacedetector.entity.CoordinatesInfo;
import org.psd.parkingspacedetector.entity.Media;
import org.psd.parkingspacedetector.entity.ParkingSlotInfo;
import org.psd.parkingspacedetector.repository.MediaRepository;
import org.psd.parkingspacedetector.repository.ParkingSlotInfoRepository;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingSlotService {

    private final MediaRepository mediaRepository;
    private final ParkingSlotInfoRepository parkingSlotInfoRepository;

    @Transactional
    public String saveParkingSlotInfos(ParkingTrainingRequest request) {
        Long mediaId = request.getMediaId();
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found"));
        saveParkingSlotInfos(request, mediaId, media);
        return media.getOriginalPath();
    }

    private void saveParkingSlotInfos(ParkingTrainingRequest request, Long mediaId, Media media) {
        parkingSlotInfoRepository.deleteByMediaIdAndIsTrain(mediaId);
        ZonedDateTime currentDatetime = ZonedDateTime.now();

        List<ParkingSlotInfo> parkingSlots = request.getSlotDetails().stream()
                .map(slotDetail -> createParkingSlotInfo(slotDetail, media, currentDatetime))
                .toList();

        parkingSlotInfoRepository.saveAll(parkingSlots);
    }

    private ParkingSlotInfo createParkingSlotInfo(ParkingSlotDetail slotDetail, Media media, ZonedDateTime createdAt) {
        ParkingSlotInfo parkingSlotInfo = ParkingSlotInfo.builder()
                .slotIndex(slotDetail.getIndex())
                .isTrain(true)
                .isEmpty(slotDetail.getIsEmpty())
                .createdAt(createdAt)
                .media(media)
                .build();

        List<CoordinatesInfo> coordinatesInfos = slotDetail.getPoints().stream()
                .map(point -> createCoordinatesInfo(point, parkingSlotInfo))
                .toList();

        parkingSlotInfo.setCoordinatesInfos(coordinatesInfos);
        return parkingSlotInfo;
    }

    private CoordinatesInfo createCoordinatesInfo(PointInfo point, ParkingSlotInfo parkingSlotInfo) {
        return CoordinatesInfo.builder()
                .pointOrder(point.getId())
                .x(point.getX())
                .y(point.getY())
                .realX(point.getRealX())
                .realY(point.getRealY())
                .parkingSlotInfo(parkingSlotInfo)
                .build();
    }

}
