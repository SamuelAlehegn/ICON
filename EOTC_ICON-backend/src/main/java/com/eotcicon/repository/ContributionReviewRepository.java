package com.eotcicon.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eotcicon.domain.ContributionReview;

public interface ContributionReviewRepository extends JpaRepository<ContributionReview, UUID> {
}
