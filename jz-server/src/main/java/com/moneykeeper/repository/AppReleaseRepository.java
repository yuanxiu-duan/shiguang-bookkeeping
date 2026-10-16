package com.moneykeeper.repository;

import com.moneykeeper.entity.AppRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppReleaseRepository extends JpaRepository<AppRelease, Long> {
    List<AppRelease> findByPlatformOrderByVersionCodeDesc(String platform);
    List<AppRelease> findByPlatformAndIsPublishedTrueOrderByVersionCodeDesc(String platform);
    boolean existsByVersionNameAndPlatform(String versionName, String platform);
}
