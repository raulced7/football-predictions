package com.raulcedroj.football_predictions.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PushMessageTest {

    @Test
    void escapesTextForJson() {
        PushMessage message = new PushMessage("Vas 1º", "Dijo \"hola\"\ny se fue \\", "/grupos/3", "summary-1");

        assertThat(message.toJson()).isEqualTo(
                "{\"title\":\"Vas 1º\",\"body\":\"Dijo \\\"hola\\\"\\ny se fue \\\\\",\"url\":\"/grupos/3\",\"tag\":\"summary-1\"}");
    }
}
