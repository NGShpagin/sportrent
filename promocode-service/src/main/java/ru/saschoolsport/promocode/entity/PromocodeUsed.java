package ru.saschoolsport.promocode.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promocodes_used")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PromocodeUsed {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promocode_id", nullable = false, comment = "ID промокода")
    @ToString.Exclude
    private Promocode promocode;

    @Column(name = "user_id", nullable = false, comment = "ID пользователя")
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
