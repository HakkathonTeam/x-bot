package com.xbot.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MentionExtractorTest {

    private final MentionExtractor extractor = new MentionExtractor();

    @Test
    void findsTelegramStyleUsernames() {
        Set<String> mentions = extractor.extract("Hi @bob_user and @Alice1234!");

        assertEquals(Set.of("bob_user", "Alice1234"), mentions);
    }

    @Test
    void doesNotMatchInsideEmailOrMidword() {
        Set<String> mentions = extractor.extract("email a@b.com and wordX@bob_user");
        assertTrue(mentions.isEmpty());
    }

    @Test
    void ignoresInvalidUsernames() {
        Set<String> mentions = extractor.extract("@12345 @ab @a____ @a");
        assertTrue(mentions.isEmpty());
    }

    @Test
    void handlesNullAndBlank() {
        assertEquals(Set.of(), extractor.extract(null));
        assertEquals(Set.of(), extractor.extract("  \n\t"));
    }
}
