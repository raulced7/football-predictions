package com.raulcedroj.football_predictions.notification;

/**
 * Lo que recibe el service worker del navegador y muestra como notificación.
 *
 * @param url al pulsar la notificación se abre esta ruta de la app
 * @param tag una notificación nueva con el mismo tag sustituye a la anterior en vez de acumularse
 */
public record PushMessage(String title, String body, String url, String tag) {

    /** JSON a mano: son cuatro textos y así el contenido cifrado no depende de la configuración de Jackson. */
    String toJson() {
        return "{\"title\":%s,\"body\":%s,\"url\":%s,\"tag\":%s}"
                .formatted(quote(title), quote(body), quote(url), quote(tag));
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append("\\u%04x".formatted((int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
