package org.example;

import java.util.Scanner;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner input = new Scanner(System.in);

        System.out.println("Введите API ключ: ");
        String key = input.nextLine().trim();

        System.out.println("Введите координаты точки, в которой хотите определить погоду.");
        System.out.println("Введите широту lat (от -90 до 90): ");
        String lat = input.nextLine().trim();
        System.out.println("Введите долготу lon (от 0 до 180): ");
        String lon = input.nextLine().trim();
        System.out.println("Введите количество суток для расчета средней температуры по прогнозу limit: ");
        int limit = input.nextInt();


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
        System.out.println("Средняя температура за " + limit + " сут.: " + String.format("%.1f", avg));
    }

    static String getWeather(String lat, String lon, int limit, String key) throws Exception {
        String url = "https://api.weather.yandex.ru/v2/forecast?lat=" + lat + "&lon=" + lon + "&limit=" + limit;

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

    static double extractFactTemp(String answer) {
        JsonObject root = JsonParser.parseString(answer).getAsJsonObject();
        JsonObject fact = root.getAsJsonObject("fact");
        return fact.get("temp").getAsDouble();
    }

    static double averageDayTemp(String answer, int limit) {
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