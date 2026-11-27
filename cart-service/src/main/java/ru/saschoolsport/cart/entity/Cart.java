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
                name = "carts_rental_period_chk",
                constraint = "rental_start is null or rental_end is null or rental_start <= rental_end"
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

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "rental_start")
    private LocalDate rentalStart;

    @Column(name = "rental_end")
    private LocalDate rentalEnd;

    @Column(name = "pickup_point_id")
    private UUID pickupPointId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @AssertTrue(message = "rental_start must not be after rental_end")
    public boolean isRentalPeriodValid() {
        if (rentalStart == null || rentalEnd == null) {
            return true;
        }
        return !rentalStart.isAfter(rentalEnd);
    }
}
