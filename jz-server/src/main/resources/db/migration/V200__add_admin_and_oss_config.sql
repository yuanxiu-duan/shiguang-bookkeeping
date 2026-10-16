-- 用户角色字段
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- OSS配置表
CREATE TABLE oss_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(50) NOT NULL COMMENT 'OSS提供商: ALIYUN/TENCENT/QINIU/MINIO/AWS',
    name VARCHAR(100) NOT NULL COMMENT '配置名称',
    endpoint VARCHAR(255) NOT NULL COMMENT '服务端点',
    region VARCHAR(100) DEFAULT 'us-east-1' COMMENT '区域',
    access_key VARCHAR(255) NOT NULL COMMENT 'AccessKey',
    secret_key VARCHAR(255) NOT NULL COMMENT 'SecretKey',
    bucket_name VARCHAR(255) NOT NULL COMMENT '存储桶名称',
    path_style_access BOOLEAN DEFAULT TRUE COMMENT '是否路径样式访问',
    public_domain VARCHAR(255) COMMENT '自定义公开访问域名',
    prefix VARCHAR(255) DEFAULT '' COMMENT '存储路径前缀',
    is_active BOOLEAN DEFAULT FALSE COMMENT '是否为当前使用的配置',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- APP发布记录表
CREATE TABLE app_release (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_name VARCHAR(50) NOT NULL COMMENT '版本号 如 1.0.0',
    version_code INT NOT NULL COMMENT '版本编号 如 100',
    platform VARCHAR(20) NOT NULL DEFAULT 'android' COMMENT '平台: android/ios',
    release_notes TEXT COMMENT '发布内容/更新说明',
    download_url VARCHAR(500) NOT NULL COMMENT 'APP文件下载链接',
    file_size BIGINT COMMENT '文件大小(字节)',
    file_key VARCHAR(500) COMMENT 'OSS存储key',
    is_force_update BOOLEAN DEFAULT FALSE COMMENT '是否强制更新',
    is_published BOOLEAN DEFAULT FALSE COMMENT '是否已发布',
    published_at DATETIME COMMENT '发布时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
