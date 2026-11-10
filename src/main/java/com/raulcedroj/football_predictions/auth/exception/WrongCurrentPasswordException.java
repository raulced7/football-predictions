package com.raulcedroj.football_predictions.auth.exception;

/**
 * Responde 400 y no 401: el usuario sí está autenticado, solo se ha equivocado al escribir
 * la contraseña actual (y un 401 haría que el front cerrase la sesión).
 */
public class WrongCurrentPasswordException extends RuntimeException {
    public WrongCurrentPasswordException() {
        super("La contraseña actual no es correcta");
    }
}