package ru.saschoolsport.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "items")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, comment = "Название товара")
    private String name;

    @Column(name = "description", comment = "Описание")
    private String description;

    @Column(name = "article", nullable = false, comment = "Артикул")
    private String article;

    @Column(name = "serial_number", nullable = false, unique = true, comment = "Серийный номер")
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false, comment = "Модель")
    @ToString.Exclude
    private Model model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32, comment = "Статус единицы товара")
    private ItemStatus status = ItemStatus.READY_FOR_RENT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specification", columnDefinition = "jsonb", comment = "Характеристики единицы товара")
    private Map<String, Object> specification;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false, comment = "Время создания записи")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, comment = "Время обновления записи")
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at", comment = "Время удаления записи")
    private Instant deletedAt;
}
