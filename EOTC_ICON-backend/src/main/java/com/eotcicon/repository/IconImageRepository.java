package com.eotcicon.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eotcicon.domain.IconImage;

public interface IconImageRepository extends JpaRepository<IconImage, UUID> {

	@Query("SELECT image FROM IconImage image WHERE image.icon.id = :iconId "
			+ "AND (image.storageKey = :key OR image.thumbnailStorageKey = :key)")
	java.util.Optional<IconImage> findMediaKey(@Param("iconId") UUID iconId, @Param("key") String key);
}
