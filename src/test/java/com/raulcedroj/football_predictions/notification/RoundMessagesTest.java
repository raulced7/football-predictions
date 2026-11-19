package com.raulcedroj.football_predictions.notification;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundMessagesTest {

    // 18 de noviembre, 10:00 en Madrid
    private static final Instant NOW = Instant.parse("2026-11-18T09:00:00Z");

    @Test
    void reminderSaysHowManyMatchesAreMissingAndWhenTheRoundStarts() {
        PushMessage one = RoundMessages.reminder(7L, "Jornada 13", Instant.parse("2026-11-18T20:00:00Z"), 1, NOW);
        PushMessage several = RoundMessages.reminder(7L, "Jornada 13", Instant.parse("2026-11-19T13:00:00Z"), 4, NOW);
        PushMessage later = RoundMessages.reminder(7L, "Jornada 13", Instant.parse("2026-11-21T17:30:00Z"), 2, NOW);

        assertThat(one.title()).isEqualTo("Te falta 1 partido por pronosticar");
        assertThat(one.body()).isEqualTo("La jornada 13 empieza hoy a las 21:00. ¡Que no se te pase!");
        assertThat(one.tag()).isEqualTo("reminder-7");
        assertThat(several.title()).isEqualTo("Te faltan 4 partidos por pronosticar");
        assertThat(several.body()).contains("mañana a las 14:00");
        assertThat(later.body()).contains("el 21/11 a las 18:30");
    }

    @Test
    void summaryShowsPointsExactHitsAndPositions() {
        PushMessage message = RoundMessages.summary(7L, "Jornada 13", 7, 2,
                List.of(new RoundMessages.GroupPosition(3L, "Los del curro", 2)));

        assertThat(message.title()).isEqualTo("Jornada 13: 7 puntos · 2 plenos");
        assertThat(message.body()).isEqualTo("Vas 2º en Los del curro.");
        assertThat(message.url()).isEqualTo("/grupos/3");
        assertThat(message.tag()).isEqualTo("summary-7");
    }

    @Test
    void summaryAdaptsToTheNumberOfGroups() {
        var a = new RoundMessages.GroupPosition(1L, "A", 2);
        var b = new RoundMessages.GroupPosition(2L, "B", 1);
        var c = new RoundMessages.GroupPosition(3L, "C", 3);
        var d = new RoundMessages.GroupPosition(4L, "D", 5);

        assertThat(RoundMessages.summary(1L, "Jornada 1", 1, 0, List.of()).body())
                .isEqualTo("Crea un grupo e invita a tus amigos para competir con ellos.");
        assertThat(RoundMessages.summary(1L, "Jornada 1", 1, 0, List.of()).title()).isEqualTo("Jornada 1: 1 punto");
        assertThat(RoundMessages.summary(1L, "Jornada 1", 0, 0, List.of(a, b)).body())
                .isEqualTo("Vas 2º en A y 1º en B.");
        assertThat(RoundMessages.summary(1L, "Jornada 1", 0, 0, List.of(a, b, c)).body())
                .isEqualTo("Vas 2º en A, 1º en B y 3º en C.");
        assertThat(RoundMessages.summary(1L, "Jornada 1", 0, 0, List.of(a, b, c, d)).body())
                .isEqualTo("Vas 2º en A, 1º en B, 3º en C y en 1 grupo más.");
        assertThat(RoundMessages.summary(1L, "Jornada 1", 0, 0, List.of(a, b)).url()).isEqualTo("/grupos");
    }
}
