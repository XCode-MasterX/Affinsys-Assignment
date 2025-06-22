package com.restapi.taskManagement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CurrencyConverter {
    private static final String API_KEY = "cur_live_N7CtW72AsuwFAd4mGZvyQR1kwREfEN29xOpWkcrm";

    public static double convert(double amount, String from, String to) {
        try {
            String url = String.format(
                "https://api.currencyapi.com/v3/latest?apikey=%s&base_currency=%s&currencies=%s",
                API_KEY, from, to
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            double rate = json.getAsJsonObject("data").getAsJsonObject(to).get("value").getAsDouble();

            return amount * rate;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void main(String[] args) {
        double balance = 100.0;
        String from = "USD";
        String to = "INR";

        double result = convert(balance, from, to);
        System.out.printf("Converted %.2f %s to %.2f %s\n", balance, from, result, to);
    }
}