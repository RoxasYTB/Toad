package com.toad.utils;

import java.util.UUID;

public class UniqueIdGenerator {
    public static Long generateUniqueId() {
        // Générer un ID unique en utilisant UUID
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE; // Exemple simple
    }
} 