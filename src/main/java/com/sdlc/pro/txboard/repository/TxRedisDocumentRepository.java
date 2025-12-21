package com.sdlc.pro.txboard.repository;

import com.redis.om.spring.repository.RedisDocumentRepository;
import com.sdlc.pro.txboard.model.TransactionLogDocument;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TxRedisDocumentRepository
        extends RedisDocumentRepository<TransactionLogDocument, String> {
    List<TransactionLogDocument> searchByStatus(String text);
}