package ru.saschoolsport.promocode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.promocode.entity.Promocode;

import java.util.UUID;

public interface PromocodeRepository extends JpaRepository<Promocode, UUID> {
}
