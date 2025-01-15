package org.psd.parkingspacedetector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.psd.parkingspacedetector.entity.ParkingSlotInfo;

import java.util.List;

public interface ParkingSlotInfoRepository extends JpaRepository<ParkingSlotInfo, Long> {

    @Modifying
    @Query("DELETE FROM ParkingSlotInfo psi WHERE psi.media.id = :mediaId AND psi.isTrain = true")
    void deleteByMediaIdAndIsTrain(@Param("mediaId") Long mediaId);

    List<ParkingSlotInfo> findAllByMedia_Id(Long mediaId);
}
