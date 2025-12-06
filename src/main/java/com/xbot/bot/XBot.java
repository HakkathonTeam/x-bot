package com.xbot.bot;

import com.xbot.config.AppConfig;
import com.xbot.parser.ParserFactory;
import com.xbot.service.ExcelGenerator;
import com.xbot.service.UserExtractor;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Main Telegram bot class.
 * TODO: Implement by Vladimir
 */
public class XBot implements LongPollingSingleThreadUpdateConsumer {
    private final AppConfig config;
    private final ParserFactory parserFactory;
    private final UserExtractor userExtractor;
    private final ExcelGenerator excelGenerator;

    public XBot(AppConfig config,
                ParserFactory parserFactory,
                UserExtractor userExtractor,
                ExcelGenerator excelGenerator) {
        this.config = config;
        this.parserFactory = parserFactory;
        this.userExtractor = userExtractor;
        this.excelGenerator = excelGenerator;
    }
    @Override
    public void consume(Update update) {

    }
}
