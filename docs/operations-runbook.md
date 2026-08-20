# Runbook de operacion - Backend Preescolar

Guia practica para operar el backend ya desplegado: como entrar, como crear el primer usuario administrador, comandos basicos y que revisar cuando algo falla.

Este documento asume que la infraestructura ya existe (ver `docs/cloud-deployment.md` para decidir donde desplegar). Aqui se cubre el dia a dia una vez que el backend esta corriendo.

## Acceso

```text
URL backend: <definir cuando exista el despliegue real, ej. https://api.nombrepreescolar.com>
Swagger:     <URL backend>/swagger-ui/index.html
API docs:    <URL backend>/v3/api-docs
```

Login:

```text
POST <URL backend>/api/auth/login
Body: { "email": "...", "password": "..." }
```

La respuesta incluye un JWT (`token`) que debe enviarse como `Authorization: Bearer <token>` en el resto de llamadas. Expira segun `JWT_EXPIRATION_MS` (24h por defecto).

## Primer arranque en una base de datos nueva

Una base de datos de produccion recien creada (sin usar `docker/mysql/init/`, que es solo para desarrollo local) queda **sin ningun usuario** despues de que Flyway aplique las migraciones: `V2__seed_roles.sql` solo crea los 6 roles del sistema, ningun usuario.

Como no existe todavia un admin, y crear usuarios via `POST /api/users` requiere estar autenticado como admin, el primer usuario administrador se crea manualmente por SQL contra la base de datos:

1. Generar el hash bcrypt de la contrasena elegida (usa el mismo `BCryptPasswordEncoder` que la app, via `jshell` con el classpath del proyecto):

   ```bash
   CP=$(./mvnw -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt; cat /tmp/cp.txt)
   echo 'System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("TU_PASSWORD_AQUI"));' \
     | jshell --class-path "target/classes:$CP" -q -
   ```

2. Insertar el usuario y asignarle el rol `SUPER_ADMIN` o `ADMIN` (el `role_id` de cada rol sale de la tabla `roles`, sembrada por `V2__seed_roles.sql`: 1=SUPER_ADMIN, 2=ADMIN, 3=DIRECTOR, 4=TEACHER, 5=FINANCE, 6=PARENT):

   ```sql
   INSERT INTO users (email, password_hash, status, email_verified, phone_verified)
   VALUES ('admin@elcliente.com', '<hash generado en el paso 1>', 'active', TRUE, FALSE);

   INSERT INTO user_roles (user_id, role_id)
   SELECT user_id, 1 FROM users WHERE email = 'admin@elcliente.com';
   ```

3. Confirmar que el login funciona contra el backend real antes de entregarlo al cliente.

Esto es un paso manual unico por entorno (local ya tiene usuarios demo via `docker/mysql/init/`; produccion no).

## Variables de entorno requeridas

La app falla al arrancar si faltan (ver `src/main/resources/application.properties`):

```text
DB_URL          jdbc:mysql://<host>:<puerto>/<db>?useSSL=false&serverTimezone=Europe/Stockholm&allowPublicKeyRetrieval=true
DB_USERNAME
DB_PASSWORD
JWT_SECRET      largo y unico para produccion, nunca reusar el de desarrollo/docker
JWT_EXPIRATION_MS   opcional, default 86400000 (24h)
```

Detalle de por que y como elegir proveedor de hosting/DB: `docs/cloud-deployment.md`.

## Comandos operativos

Arrancar (build + run):

```bash
./mvnw clean package
java -jar target/backend-preschool-0.0.1-SNAPSHOT.jar
```

Las migraciones de Flyway (`src/main/resources/db/migration/`) se aplican automaticamente al arrancar; no requieren un paso manual aparte. Para agregar un cambio de esquema, crear `V<N>__descripcion.sql` nuevo, nunca modificar uno ya aplicado.

Verificar que el backend responde:

```bash
curl -i <URL backend>/api/auth/login -X POST -H "Content-Type: application/json" -d '{"email":"...","password":"..."}'
```

Correr el smoke test contra el entorno (modo lectura, no modifica datos):

```bash
API_BASE_URL=<URL backend> API_SMOKE_READ_ONLY=true \
  API_ADMIN_EMAIL=admin@elcliente.com API_ADMIN_PASSWORD=... \
  API_PARENT_EMAIL=<un parent existente> API_PARENT_PASSWORD=... \
  node scripts/api-smoke-test.mjs
```

Ver `docs/api-smoke-tester.md` para el detalle de variables y que valida cada check.

## Troubleshooting

**Error 500 generico (`"message": "Error interno del servidor"`)**: desde este runbook en adelante `GlobalExceptionHandler` loguea la excepcion completa (`log.error("Unhandled exception", ex)`) en los logs del backend — revisar ahi la causa real, no solo la respuesta HTTP.

**Warnings normales al arrancar** (no requieren accion):
- `Using MySQL X which is newer than the version Flyway has been verified with` - informativo, Flyway funciona igual.
- `SpringDoc /swagger-ui.html endpoint is enabled by default` - decision intencional del proyecto (swagger publico), cubierta por test.

**401 Unauthorized en todo**: revisar que el `JWT_SECRET` configurado en el entorno sea el mismo que firmo el token (o volver a hacer login).

**403 Forbidden en un endpoint especifico**: revisar la tabla de permisos por rol en `docs/frontend-start.md` (seccion "Permisos por modulo").

**Conexion rechazada a MySQL al arrancar**: confirmar `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` y que la base de datos acepta conexiones desde donde corre el backend (firewall/whitelist de IP segun el proveedor).

## Pendiente (no cubierto todavia)

- Backups automaticos de la base de datos (ver `docs/cloud-deployment.md`, seccion Backups).
- Monitoreo/alertas (Sentry, uptime).
- Rotacion de `JWT_SECRET`.
- Reglas avanzadas de quien puede crear/promover usuarios (ver `docs/roadmap.md`, seccion "Funciones para fases posteriores").

Ver `docs/roadmap.md` para el checklist completo de release y el estado general del proyecto.
