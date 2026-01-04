package com.xbot.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void equalUserById() {
        User u1 = new User("id1", "Tester", "Tester Tester");
        User u2 = new User("id1", "Tester2", "Different Tester");

        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
    }

    @Test
    void constructorsFillAllFields() {
        User u = new User("Tester");
        assertEquals("Tester", u.telegramId());
        assertNull(u.username());
        assertEquals("Tester", u.name());
        assertEquals("Tester", u.fullName());

        User u2 = new User("id2", "Tester2");
        assertEquals("id2", u2.telegramId());
        assertNull(u2.username());
        assertEquals("Tester2", u2.name());
        assertEquals("Tester2", u2.fullName());

        User u3 = new User("user123", "alice", "Alice", "Alice Smith");
        assertEquals("user123", u3.telegramId());
        assertEquals("alice", u3.username());
        assertEquals("Alice", u3.name());
        assertEquals("Alice Smith", u3.fullName());
        assertTrue(u3.hasNumericId());
    }
}
