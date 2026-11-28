package ru.saschoolsport.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "model_files")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ModelFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false, comment = "ID модели")
    @ToString.Exclude
    private Model model;

    @Column(name = "link_file_id", nullable = false, comment = "url на хранилище s3")
    private String linkFileId;
}
