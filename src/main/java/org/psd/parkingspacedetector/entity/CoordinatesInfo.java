package org.psd.parkingspacedetector.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "CoordinatesInfo")
@Table(name = "coordinates_info", schema = "parking")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CoordinatesInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "point_order", columnDefinition = "INT", nullable = false)
    private Integer pointOrder;
    @Column(name = "x", columnDefinition = "FLOAT", nullable = false)
    private Double x;
    @Column(name = "y", columnDefinition = "FLOAT", nullable = false)
    private Double y;
    @Column(name = "realX", columnDefinition = "FLOAT", nullable = false)
    private Double realX;
    @Column(name = "realY", columnDefinition = "FLOAT", nullable = false)
    private Double realY;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_slot_id", nullable = false)
    private ParkingSlotInfo parkingSlotInfo;
}
