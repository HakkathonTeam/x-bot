package com.xbot.service;

import com.xbot.model.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates Excel files from extraction results.
 */
public class ExcelGenerator {

    public String generateUsersExcel(List<User> users, String chatName) throws IOException {
        String fileName = "telegram_users_" + System.currentTimeMillis() + ".xlsx";

        try (Workbook workbook = new XSSFWorkbook()) {
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

            // Сохранение файла
            try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
            }
        }

        return fileName;
    }
}