# Preschool Admin — Backend

API REST en Spring Boot para administrar un preescolar: estudiantes, padres/tutores, pagos
mensuales, materiales, horarios, asistencia y reportes. Ver [`docs/roadmap.md`](docs/roadmap.md)
para el historial completo de decisiones y `docs/institution-roadmap.md` para la vision a futuro
orientada a instituciones grandes.

Repo hermano: [`frontend-preschool`](https://github.com/JoseMunozO/frontend-preschool) (React +
Vite + TypeScript), consume esta API.

## Stack

- Java 25, Spring Boot 4
- MySQL 8.4, Flyway (migraciones versionadas en `src/main/resources/db/migration`)
- Spring Security + JWT
- iText/OpenPDF (recibos y facturas en PDF)
- Docker Compose para levantar todo localmente

## Inicio rapido

```bash
docker compose up --build
```

- Backend: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Credenciales de demo: [`docs/demo-credentials.md`](docs/demo-credentials.md)

Detalle completo (variables de entorno, volumenes, troubleshooting) en
[`docs/docker.md`](docs/docker.md).

## Capacidades actuales

### Estudiantes
Ficha completa (datos, foto de perfil, alergias/notas medicas), grupos/aulas, contactos de
emergencia (automaticos desde los tutores vinculados, mas manuales), notas tipo comentario por
profesor/direccion con historial de edicion y auditoria, consentimientos de imagen/privacidad,
albumes de fotos con aprobacion. Soft-delete con ventana de 7 dias para deshacer, purga automatica
despues.

### Padres / tutores
Alta, vinculacion a uno o varios estudiantes (con tipo de relacion), portal propio
(`/api/parents/me`) para ver sus hijos, pagos y asistencia. Ciclo de vida extendido: papelera (7
dias) -> archivado (6 anos, recuperable) -> purga definitiva, pensado para familias que se van y
podrian volver.

### Pagos
Tipos de cargo configurables (mensualidad, comedor, excursiones, materiales, etc.), generacion
automatica de la cuota mensual (con prorrateo si el estudiante se inscribe a mitad de mes),
registro de pagos con asignacion a uno o varios cargos, recibos/facturas en PDF descargables.
Descuentos aplicables a un cargo especifico (no a todos los cargos de un estudiante a la vez), ya
sea al crearlo o sobre uno ya existente. Multa por atraso: 5% de la cuota por cada mes o fraccion
vencida, calculada en tiempo real.

### Materiales
Inventario con entradas/salidas/ajustes de stock, alerta de stock bajo, historial de movimientos y
auditoria de cambios, sugerencia de cantidad minima segun consumo historico.

### Horarios y asistencia
Horario semanal por grupo con actividades y personal asignado. Asistencia diaria
(presente/ausente/tarde/enfermo) por grupo, bloqueada para edicion despues de medianoche del dia
registrado, con historial por estudiante.

### Reportes
Seis reportes con acceso por rol (rangos superiores ven todos; profesor ve asistencia, notas,
materiales y salud; finanzas ve financiero y materiales): financiero (pagos pendientes/atrasados),
asistencia agregada por rango de fechas, historial de notas de un estudiante con su auditoria,
movimientos de materiales (con balance corrido), datos de salud/alergias, y papeleras (todo lo
eliminado o archivado en un solo lugar, con fecha limite de purga).

### Roles y personal
Seis roles (`SUPER_ADMIN`, `ADMIN`, `DIRECTOR`, `TEACHER`, `FINANCE`, `PARENT`) con jerarquia por
rango para asignar/quitar roles. Alta y baja de personal (con o sin cuenta de acceso), reactivable
en cualquier momento, sin purga automatica (se conserva el historial de horarios/auditorias).

### Dashboard
Resumen especifico por rol: profesor (asistencia del dia, horarios, cumpleanos proximos),
finanzas (pagos), administracion (vista general).

## Tests y verificacion

```bash
./mvnw test                                    # suite completa (unit + integracion)
node scripts/api-smoke-test.mjs                # smoke test contra un backend corriendo
API_SMOKE_READ_ONLY=true node scripts/api-smoke-test.mjs   # version sin crear/modificar datos
```

Ver [`docs/api-smoke-tester.md`](docs/api-smoke-tester.md) para mas detalle.

## Documentacion

- [Roadmap funcional](docs/roadmap.md) — historial completo de decisiones y estado por modulo.
- [Roadmap institucional (futuro)](docs/institution-roadmap.md)
- [Docker](docs/docker.md)
- [Smoke tester](docs/api-smoke-tester.md)
- [CI (GitHub Actions)](docs/github-actions-ci.md)
- [Despliegue en la nube](docs/cloud-deployment.md)
- [Runbook de operaciones](docs/operations-runbook.md)
- [Credenciales de demo](docs/demo-credentials.md)
