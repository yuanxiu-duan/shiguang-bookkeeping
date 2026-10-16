package com.moneykeeper.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "oss_config")
public class OssConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "endpoint", nullable = false)
    private String endpoint;

    @Column(name = "region", length = 100)
    private String region = "us-east-1";

    @Column(name = "access_key", nullable = false)
    private String accessKey;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    @Column(name = "bucket_name", nullable = false)
    private String bucketName;

    @Column(name = "path_style_access")
    private Boolean pathStyleAccess = true;

    @Column(name = "public_domain")
    private String publicDomain;

    @Column(name = "prefix")
    private String prefix = "";

    @Column(name = "is_active")
    private Boolean isActive = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
