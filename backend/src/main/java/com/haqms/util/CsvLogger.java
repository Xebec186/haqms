package com.haqms.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvLogger {

    public static void log(String fileName, String header, List<String[]> rows) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(header + "\n");

            for (String[] row : rows) {
                writer.write(String.join(",", row) + "\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
