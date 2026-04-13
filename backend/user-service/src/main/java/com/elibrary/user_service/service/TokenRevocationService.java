package com.elibrary.user_service.service;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenRevocationService {

    private final ConcurrentHashMap<String, Long> revokedTokensByExpiry = new ConcurrentHashMap<>();

    public void revoke(String tokenId, Date expiresAt) {
        if (tokenId == null || tokenId.isBlank() || expiresAt == null) {
            return;
        }
        revokedTokensByExpiry.put(tokenId, expiresAt.getTime());
        cleanupExpired();
    }

    public boolean isRevoked(String tokenId) {
        cleanupExpired();
        return tokenId != null && revokedTokensByExpiry.containsKey(tokenId);
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        revokedTokensByExpiry.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
