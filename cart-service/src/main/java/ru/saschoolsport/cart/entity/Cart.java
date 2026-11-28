package ru.saschoolsport.cart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "carts",
        check = @CheckConstraint(
                name = "carts_rental_duration_chk",
                constraint = "rent_duration_hours is null or rent_duration_hours > 0"
        )
)
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, comment = "Идентификатор пользователя (владелец корзины, из JWT)")
    private UUID userId;

    @Column(name = "rental_start", comment = "Плановая дата начала аренды")
    private LocalDate rentalStart;

    @Column(name = "rent_duration_hours", comment = "Плановая длительность аренды, ч")
    private Integer rentDurationHours;

    @Column(name = "pickup_point_id", comment = "ID пункта выдачи")
    private UUID pickupPointId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false, comment = "Время создания записи")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", comment = "Время обновления записи")
    private Instant updatedAt;

    @AssertTrue(message = "rent_duration_hours must be positive when set")
    public boolean isRentalDurationValid() {
        return rentDurationHours == null || rentDurationHours > 0;
    }
}
