package ru.saschoolsport.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.order.entity.Order;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
