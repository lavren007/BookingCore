package ru.bookingcore.hotel_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class IdempotencyService {

    // В реальной системе это было бы в Redis или БД
    private final Set<String> processedRequests = ConcurrentHashMap.newKeySet();

    public boolean isProcessed(String requestId) {
        boolean processed = processedRequests.contains(requestId);
        log.debug("Checking if request {} is processed: {}", requestId, processed);
        return processed;
    }

    public void markAsProcessed(String requestId) {
        processedRequests.add(requestId);
        log.info("Request {} marked as processed", requestId);
    }

    public void removeProcessed(String requestId) {
        processedRequests.remove(requestId);
        log.info("Request {} removed from processed", requestId);
    }
}
