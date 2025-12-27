package com.xbot.parser;

import com.xbot.model.ExtractionResult;
import com.xbot.model.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

/**
 * Parser for Telegram HTML chat exports.
 */
public class HtmlChatParser implements ChatHistoryParser {

    @Override
    public ExtractionResult parse(String content) throws ParserException {

        if (content == null || content.isBlank()) {
            throw new ParserException("Empty HTML content");
        }

        try {
            Document doc = Jsoup.parse(content);

            Set<User> participants = new HashSet<>();
            Set<User> mentions = new HashSet<>();
            Set<User> channels = new HashSet<>();

            Elements messages = doc.select("div.message");

            for (Element msg : messages) {

                if (msg.hasClass("service")) {
                    continue;
                }

                Element from = msg.selectFirst("div.from_name");
                if (from == null) {
                    continue;
                }

                String displayName = extractDisplayName(from);
                if (displayName.isEmpty()) {
                    continue;
                }

                if (isDeletedAccount(displayName)) {
                    continue;
                }

                User user = new User(displayName);
                participants.add(user);

                // Extract mentions from <a> tags (real Telegram mentions)
                extractMentionsFromLinks(msg, mentions);
            }

            return new ExtractionResult(participants, mentions, channels);

        } catch (Exception e) {
            throw new ParserException("Failed to parse HTML", e);
        }
    }

    @Override
    public boolean canParse(String content) {
        if (content == null) {
            return false;
        }
        String lower = content.toLowerCase();
        return lower.contains("<html") && lower.contains("class=\"message ");
    }

    @Override
    public String getFormatName() {
        return "HTML";
    }

    private boolean isDeletedAccount(String displayName) {
        String lower = displayName.toLowerCase();
        return lower.contains("deleted account") || lower.contains("удалённый аккаунт");
    }

    /**
     * Extracts display name from from_name element, excluding date span.
     * Forwarded messages have: Name <span class="date details">timestamp</span>
     */
    private String extractDisplayName(Element fromElement) {
        // Clone to avoid modifying original DOM
        Element clone = fromElement.clone();
        // Remove date span that appears in forwarded messages
        clone.select("span.date").remove();
        return clone.text().trim();
    }

    /**
     * Extracts mentions from <a> tags in the message.
     * Telegram HTML exports mark mentions as:
     * - <a href="https://t.me/username">@username</a> for @mentions
     * - <a href="tg://user?id=12345">Display Name</a> for text mentions
     */
    private void extractMentionsFromLinks(Element msg, Set<User> mentions) {
        Elements links = msg.select("div.text a[href]");

        for (Element link : links) {
            String href = link.attr("href");
            String text = link.text().trim();

            if (href.startsWith("tg://user?id=")) {
                // Text mention with user ID: tg://user?id=12345
                String idString = href.replace("tg://user?id=", "");
                // Validate it's numeric
                if (!idString.isEmpty() && idString.chars().allMatch(Character::isDigit)) {
                    String telegramId = "user" + idString;
                    mentions.add(new User(telegramId, null, text, text));
                }
            } else if (href.startsWith("https://t.me/") || href.startsWith("http://t.me/")) {
                // @username mention: https://t.me/username
                String username = href.replaceFirst("https?://t\\.me/", "");
                if (!username.isEmpty() && !username.contains("/")) {
                    // Clean up display text
                    String displayName;
                    if (text.startsWith("@")) {
                        displayName = text.substring(1);
                    } else if (text.startsWith("https://t.me/") || text.startsWith("http://t.me/")) {
                        // Link text is URL itself, use username as display name
                        displayName = username;
                    } else {
                        displayName = text;
                    }
                    mentions.add(new User(username, username, displayName, displayName));
                }
            }
        }
    }
}
