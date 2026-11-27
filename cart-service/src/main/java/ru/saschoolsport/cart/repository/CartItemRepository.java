package ru.saschoolsport.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.cart.entity.CartItem;

import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
}
