package ru.saschoolsport.catalog.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "models",
        check = @CheckConstraint(
                name = "models_price_chk",
                constraint = "price_per_hour >= 0"
        )
)
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, comment = "Название модели")
    private String name;

    @Column(name = "brand", nullable = false, comment = "Название бренда модели")
    private String brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, comment = "ID категории")
    @ToString.Exclude
    private Category category;

    @Column(name = "price_per_hour", nullable = false, precision = 12, scale = 2,
            comment = "Стоимость аренды модели за 1 час")
    private BigDecimal pricePerHour;

    @Column(name = "is_available", nullable = false, comment = "Признак глобальной доступности модели")
    private Boolean isAvailable;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false, comment = "Время создания записи")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, comment = "Время обновления записи")
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at", comment = "Время удаления записи")
    private Instant deletedAt;

    @AssertTrue(message = "price_per_hour must be non-negative")
    public boolean isPriceValid() {
        return pricePerHour == null || pricePerHour.signum() >= 0;
    }
}
