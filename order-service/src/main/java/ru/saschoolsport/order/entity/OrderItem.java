package ru.saschoolsport.order.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "order_items",
        check = @CheckConstraint(
                name = "order_items_quantity_chk",
                constraint = "(issued_quantity is null or issued_quantity >= 0)"
                        + " and (returned_quantity is null or returned_quantity >= 0)"
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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, comment = "Ссылка на заказ")
    @ToString.Exclude
    private Order order;

    @Column(name = "catalog_item_id", nullable = false, comment = "Ссылка на ID товара в каталоге")
    private UUID catalogItemId;

    @Column(name = "issued_quantity", comment = "Фактически выданное количество товара")
    private Integer issuedQuantity;

    @Column(name = "returned_quantity", comment = "Фактически принятое количество товара")
    private Integer returnedQuantity;

    @AssertTrue(message = "returned_quantity must not exceed issued_quantity")
    public boolean isQuantityValid() {
        if (issuedQuantity == null || returnedQuantity == null) {
            return true;
        }
        if (issuedQuantity < 0 || returnedQuantity < 0) {
            return false;
        }
        return returnedQuantity <= issuedQuantity;
    }
}
