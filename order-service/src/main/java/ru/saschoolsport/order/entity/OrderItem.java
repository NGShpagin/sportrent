package ru.saschoolsport.order.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@IdClass(OrderItemId.class)
@Table(
        name = "order_items",
        check = @CheckConstraint(
                name = "order_items_quantity_chk",
                constraint = "planned_quantity > 0"
                        + " and (issued_quantity is null or issued_quantity >= 0)"
                        + " and (returned_quantity is null or returned_quantity >= 0)"
                        + " and (issued_quantity is null or issued_quantity <= planned_quantity)"
                        + " and (issued_quantity is null or returned_quantity is null"
                        + " or returned_quantity <= issued_quantity)"
        )
)
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, comment = "Ссылка на заказ")
    @ToString.Exclude
    private Order order;

    @Id
    @Column(name = "catalog_model_id", nullable = false, comment = "Ссылка на ID товара в каталоге")
    private UUID catalogModelId;

    @Column(name = "planned_quantity", nullable = false, comment = "Забронированное количество")
    private Integer plannedQuantity;

    @Column(name = "issued_quantity", comment = "Фактически выданное количество товара")
    private Integer issuedQuantity;

    @Column(name = "returned_quantity", comment = "Фактически принятое количество товара")
    private Integer returnedQuantity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "item_include", columnDefinition = "jsonb", comment = "Характеристики забронированного товара")
    private Map<String, Object> itemInclude;

    @AssertTrue(message = "quantities must be non-negative; issued <= planned; returned <= issued")
    public boolean isQuantityValid() {
        if (plannedQuantity != null && plannedQuantity <= 0) {
            return false;
        }
        if (issuedQuantity != null) {
            if (issuedQuantity < 0) {
                return false;
            }
            if (plannedQuantity != null && issuedQuantity > plannedQuantity) {
                return false;
            }
        }
        if (returnedQuantity != null) {
            if (returnedQuantity < 0) {
                return false;
            }
            if (issuedQuantity != null && returnedQuantity > issuedQuantity) {
                return false;
            }
        }
        return true;
    }
}
