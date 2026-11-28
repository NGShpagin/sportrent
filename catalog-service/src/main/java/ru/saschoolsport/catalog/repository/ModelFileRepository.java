package ru.saschoolsport.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.catalog.entity.ModelFile;

import java.util.UUID;

public interface ModelFileRepository extends JpaRepository<ModelFile, UUID> {
}
