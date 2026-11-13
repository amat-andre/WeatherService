package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Scanner;

public class WeatherService {

    public void run() throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Введите API ключ: ");
        String key = scanner.nextLine().trim();

        System.out.println("Введите координаты точки, в которой хотите определить погоду.");
        double lat = readCoordinate(scanner, "широту lat", -90.0, 90.0);
        double lon = readCoordinate(scanner, "долготу lon", -180.0, 180.0);
        int limit = readLimit(scanner);

        String answer = getWeather(lat, lon, limit, key);
        if (answer == null) {
            System.out.println("Ошибка получения ответа.");
            return;
        }

        System.out.println("Полученный json без изменений: ");
        System.out.println(answer);

        double factTemp = extractFactTemp(answer);
        System.out.println("Текущая температура fact.temp: " + factTemp);

        double avg = averageDayTemp(answer, limit);
        System.out.printf(Locale.ROOT, "Средняя температура за %d сут.: %.1f%n", limit, avg);
    }

    private double readCoordinate(Scanner scanner, String name, double min, double max) {
        while (true) {
            System.out.printf("Введите %s (от %.0f до %.0f): %n", name, min, max);
            String input = scanner.nextLine().trim();

            try {
                double value = Double.parseDouble(input.replace(",", "."));
                if (Double.isFinite(value) && value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // Сообщение об ошибке выводится ниже.
            }

            System.out.printf("Ошибка: введите число от %.0f до %.0f.%n", min, max);
        }
    }

    private int readLimit(Scanner scanner) {
        while (true) {
            System.out.println("Введите количество суток для расчета средней температуры по прогнозу limit: ");
            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);
                if (value >= 1 && value <= 7) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // Сообщение об ошибке выводится ниже.
            }

            System.out.println("Ошибка: введите целое число от 1 до 7.");
        }
    }

    private String getWeather(double lat, double lon, int limit, String key) throws Exception {
        String url = String.format(Locale.ROOT,
                "https://api.weather.yandex.ru/v2/forecast?lat=%s&lon=%s&limit=%d",
                lat, lon, limit);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Yandex-Weather-Key", key)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.out.println("Ошибка, код ответа: " + response.statusCode());
            System.out.println(response.body());
            return null;
        }

        return response.body();
    }

    private double extractFactTemp(String answer) {
        JsonObject root = JsonParser.parseString(answer).getAsJsonObject();
        JsonObject fact = root.getAsJsonObject("fact");
        return fact.get("temp").getAsDouble();
    }

    private double averageDayTemp(String answer, int limit) {
        JsonObject root = JsonParser.parseString(answer).getAsJsonObject();
        JsonArray forecasts = root.getAsJsonArray("forecasts");

        int days = Math.min(limit, forecasts.size());
        double sum = 0;

        for (int i = 0; i < days; i++) {
            JsonObject forecast = forecasts.get(i).getAsJsonObject();
            JsonObject parts = forecast.getAsJsonObject("parts");
            JsonObject day = parts.getAsJsonObject("day");

            double temperature = day.get("temp_avg").getAsDouble();
            sum += temperature;
        }
        return sum / days;
    }
}
