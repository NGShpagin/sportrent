package ru.saschoolsport.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.order.entity.OrderItem;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}
