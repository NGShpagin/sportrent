package ru.saschoolsport.order.entity;

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
        name = "orders",
        check = @CheckConstraint(
                name = "orders_actual_period_chk",
                constraint = "actual_issued_at is null or actual_end_at is null or actual_issued_at <= actual_end_at"
        )
)
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "customer_name", nullable = false, length = 100, comment = "Имя клиента, на которого оформлен заказ")
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20, comment = "Телефон клиента, на которого оформлен заказ")
    private String customerPhone;

    @Column(name = "office_id", nullable = false, comment = "Ссылка на ID пункта выдачи")
    private UUID officeId;

    @Column(name = "promocode_id", comment = "Ссылка на ID промокода")
    private UUID promocodeId;

    @Column(name = "planned_date", comment = "Плановая дата прихода в пункт выдачи")
    private LocalDate plannedDate;

    @Column(name = "rental_duration_hours", comment = "Продолжительность аренды в часах")
    private Integer rentalDurationHours;

    @Column(name = "actual_issued_at", comment = "Фактическое время выдачи оборудования")
    private Instant actualIssuedAt;

    @Column(name = "actual_end_at", comment = "Фактическое время возврата оборудования")
    private Instant actualEndAt;

    @Column(name = "issued_by_employee_id", comment = "Ссылка на ID сотрудника пункта выдачи, выдавшего заказ")
    private UUID issuedByEmployeeId;

    @Column(name = "returned_by_employee_id", comment = "Ссылка на ID сотрудника пункта выдачи, принявшего возврат")
    private UUID returnedByEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, comment = "Статус заказа", length = 20)
    private OrderStatus orderStatus = OrderStatus.RESERVED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @AssertTrue(message = "actual_issued_at must not be after actual_end_at")
    public boolean isActualPeriodValid() {
        if (actualIssuedAt == null || actualEndAt == null) {
            return true;
        }
        return !actualIssuedAt.isAfter(actualEndAt);
    }
}
