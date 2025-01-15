package org.psd.parkingspacedetector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.psd.parkingspacedetector.entity.CoordinatesInfo;

public interface CoordinatesInfoRepository extends JpaRepository<CoordinatesInfo, Long> {
}
