package ru.saschoolsport.promocode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.saschoolsport.promocode.entity.PromocodeUsed;
import ru.saschoolsport.promocode.entity.PromocodeUsedId;

public interface PromocodeUsedRepository extends JpaRepository<PromocodeUsed, PromocodeUsedId> {
}
