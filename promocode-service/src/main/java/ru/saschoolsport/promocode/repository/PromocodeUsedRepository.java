package ru.saschoolsport.promocode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.promocode.entity.PromocodeUsed;

import java.util.UUID;

public interface PromocodeUsedRepository extends JpaRepository<PromocodeUsed, UUID> {
}
