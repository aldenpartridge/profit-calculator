package com.profitcalc.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.profitcalc.api.model.AuctionResponse;
import com.profitcalc.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DonutSMPApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProfitCalc/API");
    private static final String BASE_URL = "https://api.donutsmp.net";
    private static final DonutSMPApiClient INSTANCE = new DonutSMPApiClient();

    private final HttpClient httpClient;
    private final Gson gson;

    private DonutSMPApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new GsonBuilder().create();
    }

    public static DonutSMPApiClient getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<AuctionResponse.AuctionEntry>> fetchAllAuctions() {
        String apiKey = ConfigManager.getInstance().getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            LOGGER.warn("No API key set. Use /profitcalc apikey <key> to set it.");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        return CompletableFuture.supplyAsync(() -> {
            List<AuctionResponse.AuctionEntry> allEntries = new ArrayList<>();
            int page = 1;
            boolean hasMore = true;

            while (hasMore) {
                try {
                    AuctionResponse response = fetchAuctionPage(page, apiKey);
                    if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
                        allEntries.addAll(response.getResult());
                        LOGGER.info("Fetched {} auction entries from page {}", response.getResult().size(), page);
                        page++;

                        // DonutSMP might have pagination limits, so we'll fetch multiple pages
                        // Stop if we get less than 100 items (likely the last page)
                        if (response.getResult().size() < 100) {
                            hasMore = false;
                        }
                    } else {
                        hasMore = false;
                    }

                    // Rate limiting: 250 reqs/min, so wait a bit between requests
                    if (hasMore) {
                        Thread.sleep(300); // 300ms between requests = ~200 reqs/min
                    }

                } catch (Exception e) {
                    LOGGER.error("Error fetching auction page {}: {}", page, e.getMessage());
                    hasMore = false;
                }
            }

            LOGGER.info("Fetched total of {} auction entries", allEntries.size());
            return allEntries;
        });
    }

    private AuctionResponse fetchAuctionPage(int page, String apiKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/v1/auction/list/" + page))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), AuctionResponse.class);
            } else if (response.statusCode() == 401) {
                LOGGER.error("Unauthorized - Invalid API key. Generate a new key with /api in-game.");
                return null;
            } else if (response.statusCode() == 429) {
                LOGGER.warn("Rate limit exceeded. Waiting before retry...");
                return null;
            } else {
                LOGGER.error("API error: {} - {}", response.statusCode(), response.body());
                return null;
            }

        } catch (Exception e) {
            LOGGER.error("Error fetching auction page: {}", e.getMessage());
            return null;
        }
    }

    public CompletableFuture<Boolean> testApiKey(String apiKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/v1/auction/list/1"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;

            } catch (Exception e) {
                LOGGER.error("Error testing API key: {}", e.getMessage());
                return false;
            }
        });
    }
}
