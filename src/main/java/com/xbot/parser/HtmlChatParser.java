package com.xbot.parser;

import com.xbot.model.ExtractionResult;
import com.xbot.model.User;
import com.xbot.service.MentionExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

/**
 * Parser for Telegram HTML chat exports.
 * TODO: Implement by Vica
 */
public class HtmlChatParser implements ChatHistoryParser {

    private final MentionExtractor mentionExtractor = new MentionExtractor();

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

                String displayName = from.text().trim();
                if (displayName.isEmpty()) {
                    continue;
                }

                if (isDeletedAccount(displayName)) {
                    continue;
                }

                String text = extractFullText(msg);

                User user = new User(displayName);
                participants.add(user);

                if (!text.isBlank()) {
                    var usernames = mentionExtractor.extract(text);

                    for (String username : usernames) {
                        User mentionedUser = new User(username);
                        mentions.add(mentionedUser);
                    }
                }
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

    private String extractFullText(Element msg) {
        Elements textBlocks = msg.select("div.text");
        if (textBlocks.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Element block : textBlocks) {
            String part = block.text().trim();
            if (part.isEmpty()) continue;

            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(part);
        }
        return sb.toString();
    }
}
