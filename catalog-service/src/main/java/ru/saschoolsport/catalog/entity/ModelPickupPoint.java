package ru.saschoolsport.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@IdClass(ModelPickupPointId.class)
@Table(name = "model_pickup_points")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ModelPickupPoint {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false, comment = "ID модели")
    @ToString.Exclude
    private Model model;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_point_id", nullable = false, comment = "ID пункта выдачи")
    @ToString.Exclude
    private PickupPoint pickupPoint;

    @Column(name = "is_available", nullable = false, comment = "Признак доступности модели в пункте выдачи")
    private Boolean isAvailable = true;

    @UpdateTimestamp
    @Column(name = "updated_at", comment = "Время обновления записи")
    private Instant updatedAt = Instant.now();
}
