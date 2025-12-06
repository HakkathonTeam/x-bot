package com.xbot.parser;

import com.xbot.model.ExtractionResult;
import com.xbot.model.User;
import com.xbot.service.MentionExtractor;

import java.util.HashSet;
import java.util.Set;

/**
 * Parser for Telegram JSON chat exports.
 * TODO: Implement by Alexey
 */
public class JsonChatParser implements ChatHistoryParser {
    @Override
    public ExtractionResult parse(String content) throws ParserException {
        return null;
    }

    @Override
    public boolean canParse(String content) {
        return false;
    }

    @Override
    public String getFormatName() {
        return "";
    }
}
