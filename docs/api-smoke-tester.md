# API smoke tester

Script local para verificar rapidamente que los endpoints principales del backend responden como se espera.

## Requisitos

- MySQL local funcionando.
- Backend arrancado con perfil `local`.
- Node.js 18 o superior.

## Uso

En una terminal, arranca el backend:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

En otra terminal, ejecuta:

```bash
node scripts/api-smoke-test.mjs
```

El script imprime `PASS` o `FAIL` por cada comprobacion y genera un log local en `logs/`. Por defecto conserva solo los ultimos 4 logs del smoke tester.

## Configuracion

Variables opcionales:

```bash
API_BASE_URL=http://localhost:8080 \
API_ADMIN_EMAIL=admin@school.com \
API_ADMIN_PASSWORD=123456 \
API_PARENT_EMAIL=parent.demo@school.com \
API_PARENT_PASSWORD=123456 \
API_SMOKE_LOGS_TO_KEEP=4 \
node scripts/api-smoke-test.mjs
```

## Resultado

Si alguna comprobacion falla, el script termina con exit code `1`. Esto permite usarlo mas adelante en CI.
