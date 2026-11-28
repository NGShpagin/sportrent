package ru.saschoolsport.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.catalog.entity.ModelPickupPoint;
import ru.saschoolsport.catalog.entity.ModelPickupPointId;

public interface ModelPickupPointRepository extends JpaRepository<ModelPickupPoint, ModelPickupPointId> {
}
