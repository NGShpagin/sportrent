package ru.saschoolsport.catalog.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "pickup_points",
        check = @CheckConstraint(
                name = "pickup_points_hours_chk",
                constraint = "open_time is null or close_time is null or open_time < close_time"
        )
)
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PickupPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", comment = "Название пункта выдачи")
    private String name;

    @Column(name = "address", comment = "Адрес пункта выдачи")
    private String address;

    @Column(name = "description", comment = "Краткое описание")
    private String description;

    @Column(name = "metro_station", comment = "Ближайшее метро к пункту выдачи")
    private String metroStation;

    @Column(name = "open_time", comment = "Время открытия пункта выдачи")
    private LocalTime openTime;

    @Column(name = "close_time", comment = "Время закрытия пункта выдачи")
    private LocalTime closeTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false, comment = "Время создания записи")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, comment = "Время обновления записи")
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at", comment = "Время удаления записи")
    private Instant deletedAt;

    @AssertTrue(message = "open_time must be before close_time")
    public boolean isHoursValid() {
        if (openTime == null || closeTime == null) {
            return true;
        }
        return openTime.isBefore(closeTime);
    }
}
