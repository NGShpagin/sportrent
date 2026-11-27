package ru.saschoolsport.promocode.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "promocodes",
        check = @CheckConstraint(
                name = "promocodes_period_chk",
                constraint = "start_date <= end_date"
        )
)
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Promocode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 24, comment = "Название промокода")
    private String name;

    @Column(name = "start_date", nullable = false, comment = "Начало действия промокода")
    private Instant startDate;

    @Column(name = "end_date", nullable = false, comment = "Конец действия промокода")
    private Instant endDate;

    @Column(name = "is_active", nullable = false, comment = "Статус")
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, comment = "Тип промокода")
    private PromocodeType type;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @AssertTrue(message = "start_date must not be after end_date")
    public boolean isPeriodValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }
}
