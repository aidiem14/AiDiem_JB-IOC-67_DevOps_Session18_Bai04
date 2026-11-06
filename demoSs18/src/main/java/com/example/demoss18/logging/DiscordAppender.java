
package com.example.demoss18.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DiscordAppender extends AppenderBase<ILoggingEvent> {

    private String webhookUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {

        // Chỉ gửi thông báo với log level ERROR
        if (eventObject.getLevel() != Level.ERROR) {
            return;
        }

        if (webhookUrl == null || webhookUrl.isBlank()) {
            addError("Discord webhook URL is not configured");
            return;
        }

        try {
            String message = eventObject.getFormattedMessage();

            // Lấy stack trace nếu log có Exception
            String stackTrace = "";

            if (eventObject.getThrowableProxy() != null) {
                stackTrace = ThrowableProxyUtil.asString(
                        eventObject.getThrowableProxy()
                );
            }

            // Discord Embed payload
            Map<String, Object> embed = Map.of(
                    "title", "StoreX - System Alert",
                    "description", message,
                    "color", 15158332,
                    "fields", List.of(
                            Map.of(
                                    "name", "Level",
                                    "value", eventObject.getLevel().toString(),
                                    "inline", true
                            ),
                            Map.of(
                                    "name", "Logger",
                                    "value", eventObject.getLoggerName(),
                                    "inline", true
                            ),
                            Map.of(
                                    "name", "Timestamp",
                                    "value", Instant.ofEpochMilli(
                                            eventObject.getTimeStamp()
                                    ).toString(),
                                    "inline", false
                            )
                    )
            );

            Map<String, Object> payload;

            if (stackTrace.isBlank()) {
                payload = Map.of(
                        "embeds", List.of(embed)
                );
            } else {
                // Discord giới hạn độ dài nội dung embed.
                // Chỉ đưa một phần stack trace vào thông báo.
                String shortenedStackTrace = stackTrace.length() > 1000
                        ? stackTrace.substring(0, 1000)
                        : stackTrace;

                Map<String, Object> embedWithException = Map.of(
                        "title", "StoreX - System Alert",
                        "description", message,
                        "color", 15158332,
                        "fields", List.of(
                                Map.of(
                                        "name", "Level",
                                        "value", eventObject.getLevel().toString(),
                                        "inline", true
                                ),
                                Map.of(
                                        "name", "Logger",
                                        "value", eventObject.getLoggerName(),
                                        "inline", true
                                ),
                                Map.of(
                                        "name", "Exception",
                                        "value", shortenedStackTrace,
                                        "inline", false
                                )
                        )
                );

                payload = Map.of(
                        "embeds", List.of(embedWithException)
                );
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                addError(
                        "Discord webhook returned HTTP "
                                + response.statusCode()
                );
            }

        } catch (Exception e) {
            // Không dùng log.error() ở đây vì có thể tạo vòng lặp:
            // DiscordAppender -> log.error -> DiscordAppender
            addError("Failed to send Discord webhook", e);
        }
    }
}

