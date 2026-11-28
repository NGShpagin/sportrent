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
        check = {
                @CheckConstraint(
                        name = "promocodes_period_chk",
                        constraint = "start_date <= end_date"
                ),
                @CheckConstraint(
                        name = "promocodes_percent_chk",
                        constraint = "percent >= 0 and percent <= 100"
                ),
                @CheckConstraint(
                        name = "promocodes_max_users_chk",
                        constraint = "max_total_users is null or max_total_users > 0"
                )
        }
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

    @Column(name = "user_id", comment = "ID пользователя. Заполняется для персональных промокодов")
    private UUID userId;

    @Column(name = "percent", nullable = false, comment = "Процент скидки")
    private Integer percent;

    @Column(name = "is_active", nullable = false, comment = "Статус")
    private Boolean isActive = true;

    @Column(name = "max_total_users", comment = "Максимальное количество использования")
    private Integer maxTotalUsers;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false, comment = "Время создания записи")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", comment = "Время обновления записи")
    private Instant updatedAt;

    @Column(name = "deleted_at", comment = "Время удаления записи")
    private Instant deletedAt;

    @AssertTrue(message = "start_date must not be after end_date")
    public boolean isPeriodValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }

    @AssertTrue(message = "percent must be between 0 and 100")
    public boolean isPercentValid() {
        return percent == null || (percent >= 0 && percent <= 100);
    }

    @AssertTrue(message = "max_total_users must be positive when set")
    public boolean isMaxTotalUsersValid() {
        return maxTotalUsers == null || maxTotalUsers > 0;
    }
}
