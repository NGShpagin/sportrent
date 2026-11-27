package ru.saschoolsport.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.cart.entity.Cart;

import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
}
