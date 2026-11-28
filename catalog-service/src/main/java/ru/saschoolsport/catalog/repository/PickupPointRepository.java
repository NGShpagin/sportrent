package ru.saschoolsport.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.catalog.entity.PickupPoint;

import java.util.UUID;

public interface PickupPointRepository extends JpaRepository<PickupPoint, UUID> {
}
