package com.raulcedroj.football_predictions.notification.dto;

/**
 * @param devices dispositivos suscritos
 * @param sent    a cuántos ha llegado (el resto tenían la suscripción caducada y se han borrado, o ha fallado el envío)
 */
public record PushBroadcastResponse(int devices, int sent) {
}
