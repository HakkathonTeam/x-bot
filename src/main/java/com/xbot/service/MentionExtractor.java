package com.xbot.service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts @username mentions from text.
 * TODO: Implement by Vica
 */
public class MentionExtractor {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("(?<=^|\\s)@[A-Za-z][A-Za-z0-9_]{4,31}"); //тут баг  [Text as array , {type=bold, text=@dave_user}] не видит

    public Set<String> extract(String text) {
        Set<String> result = new HashSet<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        Matcher matcher = USERNAME_PATTERN.matcher(text);
        while (matcher.find()) {
            String mention = matcher.group();
            result.add(mention.substring(1));
        }
        return result;
    }
}
