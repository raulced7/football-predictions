# Perfil de producción: se activa con SPRING_PROFILES_ACTIVE=prod

spring:
  docker:
    compose:
      enabled: false

app:
  jwt:
    # Sin valor por defecto: si falta JWT_SECRET, la aplicación no arranca
    secret: ${JWT_SECRET}

# La documentación de la API solo se publica en local
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false