package org.psd.parkingspacedetector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.psd.parkingspacedetector.entity.Media;

import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findAllByDisabledFalse();

    List<Media> findTop5ByDisabledFalseAndMediaType_IdOrderByCreatedAtDesc(Long mediaTypeId);

    Optional<Media> findByIdAndDisabledFalse(Long id);

    List<Media> findByRefIdAndDisabledFalse(Long refId);

    List<Media> findByRefIdAndDisabledFalseAndMediaType_IdOrderByCreatedAtDesc(Long refId, Long mediaTypeId);
}
