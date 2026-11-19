package com.raulcedroj.football_predictions.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Los textos de los avisos de jornada. Sin dependencias, para poder probarlos con tests unitarios. */
final class RoundMessages {

    static final ZoneId ZONE = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d/M");
    private static final int MAX_GROUPS_LISTED = 3;

    private RoundMessages() {
    }

    static PushMessage reminder(Long roundId, String roundName, Instant startsAt, int missing, Instant now) {
        String title = missing == 1
                ? "Te falta 1 partido por pronosticar"
                : "Te faltan %d partidos por pronosticar".formatted(missing);
        String body = "La %s empieza %s. ¡Que no se te pase!".formatted(lowerFirst(roundName), when(startsAt, now));
        return new PushMessage(title, body, "/", "reminder-" + roundId);
    }

    static PushMessage summary(Long roundId, String roundName, int points, int exactHits, List<GroupPosition> groups) {
        String title = "%s: %s".formatted(roundName, plural(points, "punto", "puntos"));
        if (exactHits > 0) {
            title += " · " + plural(exactHits, "pleno", "plenos");
        }
        String body = groups.isEmpty()
                ? "Crea un grupo e invita a tus amigos para competir con ellos."
                : "Vas " + positions(groups) + ".";
        String url = groups.size() == 1 ? "/grupos/" + groups.getFirst().groupId() : "/grupos";
        return new PushMessage(title, body, url, "summary-" + roundId);
    }

    /** "2º en Los del curro", "2º en A y 1º en B", "2º en A, 1º en B, 3º en C y en 2 grupos más". */
    private static String positions(List<GroupPosition> groups) {
        List<String> parts = groups.stream()
                .limit(MAX_GROUPS_LISTED)
                .map(group -> group.position() + "º en " + group.groupName())
                .toList();
        int rest = groups.size() - parts.size();
        if (rest > 0) {
            return String.join(", ", parts) + " y en " + plural(rest, "grupo más", "grupos más");
        }
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " y " + parts.getLast();
    }

    /** "hoy a las 21:00", "mañana a las 14:00" o "el 23/11 a las 18:30". */
    private static String when(Instant startsAt, Instant now) {
        ZonedDateTime start = startsAt.atZone(ZONE);
        LocalDate today = now.atZone(ZONE).toLocalDate();
        String day;
        if (start.toLocalDate().equals(today)) {
            day = "hoy";
        } else if (start.toLocalDate().equals(today.plusDays(1))) {
            day = "mañana";
        } else {
            day = "el " + start.format(DAY);
        }
        return day + " a las " + start.format(TIME);
    }

    private static String plural(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    private static String lowerFirst(String value) {
        return value.isEmpty() ? value : Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    record GroupPosition(Long groupId, String groupName, int position) {
    }
}
