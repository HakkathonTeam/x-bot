package com.xbot.service;

import com.xbot.model.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Generates Excel files from extraction results.
 */
public class ExcelGenerator {

    public File generateUsersExcel(List<User> users, String chatName, String tempDirectory) throws IOException {
        String fileName = "telegram_users_" + System.currentTimeMillis() + ".xlsx";
        File tempFile = new File(tempDirectory, fileName);

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            Sheet sheet = workbook.createSheet("Участники чата");

            // Стили
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {"№", "Telegram ID", "Имя и фамилия", "Ссылка на профиль", "Дата экспорта"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данные
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String exportDate = LocalDateTime.now().format(formatter);

            int rowNum = 1;
            int counter = 1;
            for (User user : users) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(counter++);
                row.createCell(1).setCellValue(user.telegramId());
                row.createCell(2).setCellValue(user.fullName());
                row.createCell(3).setCellValue("https://web.telegram.org/k/#" + user.telegramId());
                row.createCell(4).setCellValue(exportDate);
            }

            // Автоподбор ширины колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Записываем в файл
            workbook.write(outputStream);
        }

        return tempFile;
    }

    /**
     * Generates Excel with separate sheets for participants, mentions, and channels.
     */
    public File generateExcel(Set<User> participants, Set<User> mentions, Set<User> channels,
                              String chatName, String tempDirectory) throws IOException {
        String fileName = "telegram_users_" + System.currentTimeMillis() + ".xlsx";
        File tempFile = new File(tempDirectory, fileName);

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String exportDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Create sheets
            if (!participants.isEmpty()) {
                createUserSheet(workbook, "Участники", participants, headerStyle, exportDate);
            }
            if (!mentions.isEmpty()) {
                createUserSheet(workbook, "Упоминания", mentions, headerStyle, exportDate);
            }
            if (!channels.isEmpty()) {
                createUserSheet(workbook, "Каналы", channels, headerStyle, exportDate);
            }

            // If all empty, create empty sheet
            if (workbook.getNumberOfSheets() == 0) {
                workbook.createSheet("Пусто");
            }

            workbook.write(outputStream);
        }

        return tempFile;
    }

    private void createUserSheet(Workbook workbook, String sheetName, Set<User> users,
                                  CellStyle headerStyle, String exportDate) {
        Sheet sheet = workbook.createSheet(sheetName);

        // Headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {"№", "Telegram ID", "Имя и фамилия", "Ссылка на профиль", "Дата экспорта"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        int rowNum = 1;
        int counter = 1;
        for (User user : users) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(counter++);
            row.createCell(1).setCellValue(user.telegramId() != null ? user.telegramId() : "");
            row.createCell(2).setCellValue(user.fullName() != null ? user.fullName() : "");
            row.createCell(3).setCellValue(formatProfileLink(user));
            row.createCell(4).setCellValue(exportDate);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String formatProfileLink(User user) {
        if (user.telegramId() == null) {
            return "";
        }
        if (user.telegramId().startsWith("user")) {
            return "tg://user?id=" + user.telegramId().substring(4);
        }
        return "https://t.me/" + user.telegramId();
    }
}