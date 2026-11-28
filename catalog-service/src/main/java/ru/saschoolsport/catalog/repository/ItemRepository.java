package ru.saschoolsport.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.catalog.entity.Item;

import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {
}
