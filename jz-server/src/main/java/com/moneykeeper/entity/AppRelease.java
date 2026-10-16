package com.moneykeeper.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "app_release")
public class AppRelease {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_name", nullable = false, length = 50)
    private String versionName;

    @Column(name = "version_code", nullable = false)
    private Integer versionCode;

    @Column(name = "platform", nullable = false, length = 20)
    private String platform = "android";

    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    @Column(name = "download_url", nullable = false)
    private String downloadUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_key")
    private String fileKey;

    @Column(name = "is_force_update")
    private Boolean isForceUpdate = false;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
