package org.psd.parkingspacedetector.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "MediaType")
@Table(name = "media_type", schema = "media")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MediaType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String name;
}
