package ru.saschoolsport.promocode.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(PromocodeUsedId.class)
@Table(name = "promocodes_used")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PromocodeUsed {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promocode_id", nullable = false, comment = "ID промокода")
    @ToString.Exclude
    private Promocode promocode;

    @Id
    @Column(name = "user_id", nullable = false, comment = "ID пользователя")
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false, comment = "Время использования промокода")
    private Instant createdAt;
}
