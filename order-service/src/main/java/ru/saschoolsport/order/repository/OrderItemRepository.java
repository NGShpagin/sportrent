package ru.saschoolsport.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.order.entity.OrderItem;
import ru.saschoolsport.order.entity.OrderItemId;

public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemId> {
}
