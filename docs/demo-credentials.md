# Credenciales demo — backend-preschool (Docker local)

Backend: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui/index.html

Todas las contraseñas: `123456`

## Administración / Dirección

| Rol | Email | Nombre |
| --- | --- | --- |
| Admin | admin@school.com | — |
| Director | director@school.com | — (cuenta demo generica) |
| Director | rafael.mendoza@escuela.com | Rafael Mendoza (2026-08-25, cuenta con nombre real para la presentacion) |

## Profesores

| Rol | Email | Nombre | Grupos asignados |
| --- | --- | --- | --- |
| Teacher (lead) | teacher@school.com | — | Rainbow Room (2), Forest Room (3) |
| Assistant teacher | assistant@school.com | — | Sunflower Room (1), Rainbow Room (2) |
| Teacher | camila.vargas@escuela.com | Camila Vargas (2026-08-25) | Sunflower Room (1) |

## Finanzas

| Rol | Email | Nombre |
| --- | --- | --- |
| Finance | finance@school.com | — |
| Finance | valentina.reyes@escuela.com | Valentina Reyes (2026-08-25) |

## Padres / Tutores

| Rol | Email | Nombre | Hijo/a |
| --- | --- | --- | --- |
| Parent (demo) | parent.demo@school.com | — | — |
| Parent (Sofia) | parent.sofia@school.com | — | Sofia Lindberg |
| Parent (Noah) | parent.noah@school.com | — | Noah Eriksson |
| Parent (Emma) | parent.emma@school.com | — | Emma Nilsson |
| Parent | ana.fernandez@correo.com | Ana Fernandez | Mateo Fernandez |
| Parent | carlos.cruz@correo.com | Carlos Cruz | Isabella Cruz |
| Parent | daniela.rodriguez@correo.com | Daniela Rodriguez | Santiago Rodriguez |
| Parent | miguel.santos@correo.com | Miguel Santos | Valeria Santos |
| Parent | lucia.paulino@correo.com | Lucia Paulino | Diego Paulino |
| Parent | patricia.guzman@correo.com | Patricia Guzman | Emiliano Guzman |

## Notas para probar

- **Fotos**: subir foto de perfil requiere consentimiento `IMAGE_PROFILE_PHOTO` activo para ese estudiante primero (`POST /api/students/{id}/consents`), si no da 400.
- **Asistencia**: `teacher@school.com` solo puede marcar/leer asistencia de sus grupos (2 y 3) — grupo 1 (Sunflower Room) le da 403. Mismo criterio para `camila.vargas@escuela.com`, que solo tiene Sunflower Room (1).
- **Padres/tutores**: `teacher@school.com` ve la lista de padres filtrada solo a los estudiantes de sus grupos asignados.
- Estudiantes semilla: Lucas Andersson (grupo 3), Sofia Lindberg (grupo 2), Noah Eriksson (grupo 2), Emma Nilsson (grupo 3), Maya Garcia (grupo 1), Oliver Brown (grupo 2, pendiente).

## Datos agregados para la demo del 2026-08-26

Creados el 2026-08-25 vía la API real (no en el seed de Docker) para mostrar variedad en la
presentación. Contraseña de todas las cuentas nuevas: `123456`.

**Estudiantes nuevos** (con su padre/madre ya vinculado, ver tabla de arriba): Mateo Fernandez
(grupo 1), Isabella Cruz (grupo 2), Santiago Rodriguez (grupo 3), Valeria Santos (grupo 1), Diego
Paulino (grupo 2), Emiliano Guzman (grupo 3).

**Multa por atraso — escalando de 1 a 6 meses** (5% de la mensualidad por mes o fraccion de atraso,
ver `PaymentService.LATE_FEE_PERCENTAGE_PER_MONTH`), un cargo por estudiante para que se vea claro
cada nivel por separado:

| Estudiante | Vencio | Meses de atraso | Cuota | Multa | Total |
| --- | --- | --- | --- | --- | --- |
| Mateo Fernandez | 2026-07-31 | 1 | RD$6,000 | RD$300 | RD$6,300 |
| Isabella Cruz | 2026-06-30 | 2 | RD$6,000 | RD$600 | RD$6,600 |
| Santiago Rodriguez | 2026-05-31 | 3 | RD$6,000 | RD$900 | RD$6,900 |
| Valeria Santos | 2026-04-30 | 4 | RD$6,000 | RD$1,200 | RD$7,200 |
| Diego Paulino | 2026-03-31 | 5 | RD$6,000 | RD$1,500 | RD$7,500 |
| Emiliano Guzman | 2026-02-28 | 6 | RD$6,000 | RD$1,800 | RD$7,800 |

La multa se calcula al vuelo (no esta guardada), asi que estos montos subiran un poco mas cada mes
que pase sin pagarse — ver `GET /api/payments/charges?status=OVERDUE`.

**Materiales con poco stock** (para mostrar junto a los que ya tenian mucho stock): Pintura tempera
(5/20), Papel crepe (8/25), Plastilina (3/15) — ver `GET /api/materials/low-stock`.
