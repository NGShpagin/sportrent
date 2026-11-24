package ru.saschoolsport.authz.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class UserPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false, updatable = false)
    private String resource;

    @Column(length = 20, nullable = false, updatable = false)
    private String action;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    public UserPermission(String resource, String action) {
        this.resource = resource;
        this.action = action;
        this.name = resource + ":" + action;
    }

    public UserPermission(String resource, String action, String description) {
        this.resource = resource;
        this.action = action;
        this.description = description;
        this.name = resource + ":" + action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPermission that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
