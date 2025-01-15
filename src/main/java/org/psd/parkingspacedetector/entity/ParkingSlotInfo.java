package org.psd.parkingspacedetector.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Entity(name = "ParkingSlotInfo")
@Table(name = "parking_slot_info", schema = "parking")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ParkingSlotInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "slot_index", columnDefinition = "INT", nullable = false)
    private Integer slotIndex;
    @Column(name = "is_train", columnDefinition = "BIT", nullable = false)
    private Boolean isTrain;
    @Column(name = "is_empty", columnDefinition = "BIT", nullable = false)
    private Boolean isEmpty;
    @Column(name = "created_at", columnDefinition = "DATETIME", nullable = false)
    private ZonedDateTime createdAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @OneToMany(mappedBy = "parkingSlotInfo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CoordinatesInfo> coordinatesInfos;
}
