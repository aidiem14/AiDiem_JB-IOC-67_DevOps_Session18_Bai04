package com.example.demoss18;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
    private static final Logger log =
            LoggerFactory.getLogger(DemoController.class);

    @GetMapping("/demo")
    public String demo() {
        log.info("Request started");

        log.info("Processing transaction");

        log.error("Transaction Failed");

        log.info("Request completed");

        return "Check console logs";
    }


    @GetMapping("/test-error")
    public String testError() {

        log.error("Lỗi DB");

        return "ERROR log has been generated";
    }

    @GetMapping("/test-info")
    public String testInfo() {
        log.info("Đây là log INFO");
        return "INFO log generated";
    }

    @GetMapping("/test-exception")
    public String testException() {

        try {
            throw new RuntimeException("Database connection failed");
        } catch (RuntimeException e) {
            log.error("Transaction Failed", e);
        }

        return "Exception logged";
    }

    @GetMapping("/test-log-rotation")
    public String testLogRotation() {

        for (int i = 1; i <= 100000; i++) {
            log.info(
                    "Log Rotation Test - Request Number: {} - This is a test message to generate log data.",
                    i
            );
        }

        return "Log rotation test completed";
    }
}
