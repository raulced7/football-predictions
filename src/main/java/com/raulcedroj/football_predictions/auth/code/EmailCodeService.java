package com.raulcedroj.football_predictions.auth.code;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Códigos de 6 dígitos que se envían por correo. Se guardan cifrados (como las contraseñas), caducan,
 * admiten pocos intentos y no se pueden pedir seguidos.
 *
 * <p>No es {@code @Transactional} a propósito: se ejecuta dentro de la transacción de quien lo llama,
 * que decide si un intento fallido se guarda aunque se lance una excepción.
 */
@Service
public class EmailCodeService {

    public static final Duration TIME_TO_LIVE = Duration.ofMinutes(15);
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    public static final int MAX_ATTEMPTS = 5;

    private final EmailCodeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public EmailCodeService(EmailCodeRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Genera un código nuevo (sustituye al anterior, si lo había) y lo devuelve en claro para enviarlo. */
    public String issue(Long userId, CodePurpose purpose) {
        String code = "%06d".formatted(random.nextInt(1_000_000));
        String hash = passwordEncoder.encode(code);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(TIME_TO_LIVE);

        repository.findByUserIdAndPurpose(userId, purpose).ifPresentOrElse(
                existing -> existing.renew(hash, now, expiresAt),
                () -> repository.save(new EmailCodeEntity(userId, purpose, hash, now, expiresAt)));
        return code;
    }

    /** Se puede pedir un código si no hay ninguno o si el último se envió hace más de {@link #RESEND_COOLDOWN}. */
    public boolean canIssue(Long userId, CodePurpose purpose) {
        Instant now = clock.instant();
        return repository.findByUserIdAndPurpose(userId, purpose)
                .map(code -> !code.getCreatedAt().plus(RESEND_COOLDOWN).isAfter(now))
                .orElse(true);
    }

    /** Comprueba el código y, si es correcto, lo borra para que no se pueda volver a usar. */
    public void consume(Long userId, CodePurpose purpose, String code) {
        EmailCodeEntity stored = repository.findByUserIdAndPurpose(userId, purpose)
                .orElseThrow(() -> new InvalidCodeException("El código no es válido. Pide uno nuevo."));

        if (stored.isExpired(clock.instant())) {
            repository.delete(stored);
            throw new InvalidCodeException("El código ha caducado. Pide uno nuevo.");
        }
        if (!passwordEncoder.matches(code, stored.getCodeHash())) {
            if (stored.registerFailedAttempt() >= MAX_ATTEMPTS) {
                repository.delete(stored);
                throw new InvalidCodeException("Demasiados intentos. Pide un código nuevo.");
            }
            throw new InvalidCodeException("El código no es correcto.");
        }
        repository.delete(stored);
    }
}