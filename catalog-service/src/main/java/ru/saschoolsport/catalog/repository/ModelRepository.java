package ru.saschoolsport.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.catalog.entity.Model;

import java.util.UUID;

public interface ModelRepository extends JpaRepository<Model, UUID> {
}
