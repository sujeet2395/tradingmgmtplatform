package com.learn.tradingmgmt.repository;

import com.learn.tradingmgmt.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByRelatedEntityTypeAndRelatedEntityId(String relatedEntityType, UUID relatedEntityId);
}
