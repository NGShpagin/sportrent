package ru.saschoolsport.order.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        check = {
                @CheckConstraint(
                        name = "orders_actual_period_chk",
                        constraint = "actual_issued_at is null or actual_end_at is null or actual_issued_at <= actual_end_at"
                ),
                @CheckConstraint(
                        name = "orders_money_chk",
                        constraint = "fixed_total_price >= 0"
                                + " and discount_amount >= 0"
                                + " and amount_to_pay >= 0"
                                + " and (added_price is null or added_price >= 0)"
                                + " and used_points >= 0"
                                + " and (accrued_points is null or accrued_points >= 0)"
                                + " and discount_amount <= fixed_total_price"
                ),
                @CheckConstraint(
                        name = "orders_rental_duration_chk",
                        constraint = "planned_rental_duration_hours is null or planned_rental_duration_hours > 0"
                )
        }
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

    @Column(name = "user_id", nullable = false, comment = "Ссылка на айди пользователя")
    private UUID userId;

    @Column(name = "customer_name", nullable = false, length = 100, comment = "Имя клиента, на которого оформлен заказ")
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20, comment = "Телефон клиента, на которого оформлен заказ")
    private String customerPhone;

    @Column(name = "pickup_point_id", nullable = false, comment = "Ссылка на ID пункта выдачи")
    private UUID pickupPointId;

    @Column(name = "promocode_id", comment = "Ссылка на ID промокода")
    private UUID promocodeId;

    @Column(name = "planned_rent_start_date", comment = "Плановая дата прихода в пункт выдачи")
    private LocalDate plannedRentStartDate;

    @Column(name = "planned_rental_duration_hours", comment = "Продолжительность аренды в часах")
    private Integer plannedRentalDurationHours;

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

    @Column(name = "fixed_total_price", nullable = false, precision = 12, scale = 2,
            comment = "Фиксированная полная сумма всех позиций")
    private BigDecimal fixedTotalPrice = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2, comment = "Скидка по промокоду")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "used_points", nullable = false, comment = "Количество списанных баллов")
    private Integer usedPoints = 0;

    @Column(name = "amount_to_pay", nullable = false, precision = 12, scale = 2,
            comment = "Сумма к оплате при выдаче заказа")
    private BigDecimal amountToPay = BigDecimal.ZERO;

    @Column(name = "added_price", precision = 12, scale = 2, comment = "Сумма доплаты после сдачи оборудования")
    private BigDecimal addedPrice;

    @Column(name = "accrued_points", comment = "Начисленные баллы после завершения заказа")
    private Integer accruedPoints;

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

    @AssertTrue(message = "money and points fields must be non-negative; discount must not exceed total")
    public boolean isMoneyValid() {
        if (fixedTotalPrice == null || discountAmount == null || amountToPay == null || usedPoints == null) {
            return true;
        }
        if (fixedTotalPrice.signum() < 0
                || discountAmount.signum() < 0
                || amountToPay.signum() < 0
                || usedPoints < 0) {
            return false;
        }
        if (addedPrice != null && addedPrice.signum() < 0) {
            return false;
        }
        if (accruedPoints != null && accruedPoints < 0) {
            return false;
        }
        return discountAmount.compareTo(fixedTotalPrice) <= 0;
    }

    @AssertTrue(message = "planned_rental_duration_hours must be positive when set")
    public boolean isRentalDurationValid() {
        return plannedRentalDurationHours == null || plannedRentalDurationHours > 0;
    }
}
