# Roadmap funcional - App de administracion para preescolar

Documento vivo para alinear el backend con la propuesta validada con el cliente. Resume objetivo, alcance funcional, estado actual, pendientes y orden recomendado de implementacion.

Este es el roadmap activo para el preescolar actual. Para una version futura orientada a instituciones grandes, ver `docs/institution-roadmap.md`.

## Idea principal

Crear una aplicacion sencilla y centralizada para que el preescolar pueda administrar estudiantes, pagos mensuales, material escolar y horarios desde un solo lugar.

## Objetivo final del proyecto

Crear una herramienta administrativa clara, facil de usar y adaptada al funcionamiento real del preescolar. La aplicacion debe ayudar a reducir trabajo manual, evitar errores y dar una vision rapida del estado del centro.

- Centralizar la informacion importante en un solo sistema.
- Ahorrar tiempo en tareas administrativas repetitivas.
- Tener mejor control de pagos, estudiantes, materiales y horarios.
- Permitir que el sistema pueda crecer en el futuro con nuevas funciones.

## Problemas a resolver

| Area | Que se busca resolver |
| --- | --- |
| Estudiantes | Tener una ficha ordenada de cada nino, sus datos importantes y sus responsables. |
| Pagos mensuales | Controlar cuotas pagadas, pendientes y atrasadas sin depender de notas sueltas o archivos dispersos. |
| Material escolar | Saber que materiales existen, cuanto queda y cuando hace falta reponer. |
| Horarios | Organizar grupos, actividades, rutinas diarias y personal responsable. |
| Dashboard | Ver de forma rapida lo mas importante del dia o del mes. |

## Estado general actual del backend

- [x] Autenticacion JWT.
- [x] Usuarios y roles base.
- [x] Seguridad por roles.
- [x] Administracion basica de estudiantes.
- [x] Administracion de padres, madres y tutores.
- [x] Vinculacion entre estudiantes y padres/tutores.
- [x] Portal basico de padre/tutor: `/api/parents/me` y `/api/parents/me/students`.
- [x] Flyway baseline aplicado sobre la base de datos existente.
- [x] Seed versionado de roles.
- [x] Tests basicos de contexto y servicios principales.
- [x] Administracion base de pagos mensuales.
- [x] Administracion base de material escolar.
- [x] Administracion base de horarios.
- [x] Dashboard principal.

## Prioridad actual: soft-delete y restauracion (eliminar con deshacer)

Pedido desde el frontend (2026-08-20): la app de administracion quiere ofrecer "eliminar con posibilidad de deshacer" para acciones destructivas, empezando por eliminar estudiante, con una ventana de gracia de unos 7 dias antes del borrado definitivo. Extendido (2026-08-21) al resto de entidades que tenga sentido, mismo patron.

Estado por entidad:

- [x] `Student` — implementado y verificado (2026-08-20).
- [x] `Material` — implementado y verificado (2026-08-21). Unica diferencia: `material_movements` tiene `ON DELETE CASCADE` (no `RESTRICT` como `student_charges`), asi que la purga automatica si borra el historial de movimientos — aceptable, no es dato financiero.
- [x] `ScheduleSlot` — implementado y verificado (2026-08-21). Confirmado: `schedule_slots` no tiene FKs entrantes, asi que la purga automatica nunca queda bloqueada (verificado con `DELETE` directo en MySQL real, sin error de constraint).
- [x] `Parent` (padres/tutores) — implementado y verificado (2026-08-21). Decision de diseño confirmada: `deletedAt` es independiente del campo `status` (ACTIVE/INACTIVE) existente — `status` sigue siendo el toggle operativo (activar/desactivar) y `deletedAt` solo gobierna la mecanica de eliminar/restaurar/purgar en el listado de administracion; el resto de servicios (pagos, consentimientos, etc.) siguen usando `parentRepository.findById` sin filtrar por `deletedAt`, igual que el patron ya usado para `Student`/`Material` en otros servicios. `student_guardians` tiene `ON DELETE CASCADE` (se pierde el vinculo, aceptable), `payments` tiene `ON DELETE SET NULL` (se preserva el historial de pagos), `student_consents` no tiene accion explicita = `RESTRICT` por defecto (protege el registro de consentimientos, igual que los cargos de un estudiante) — verificado con `DELETE` directo en MySQL real en los tres casos.

Patron usado para `Student`, `Material`, `ScheduleSlot` y `Parent`:

- [x] Agregado campo `deletedAt` (timestamp nullable) a la entidad `Student` (`V8__add_student_deleted_at.sql`), `Material` (`V9__add_material_deleted_at.sql`), `ScheduleSlot` (`V10__add_schedule_slot_deleted_at.sql`) y `Parent` (`V11__add_parent_deleted_at.sql`).
- [x] `DELETE /api/students/{id}` ahora hace soft-delete: setea `deletedAt = now()` en vez de eliminar la fila.
- [x] `GET /api/students` y `GET /api/students/{id}` excluyen por defecto los registros con `deletedAt` no nulo.
- [x] Nuevo endpoint `POST /api/students/{id}/restore`: limpia `deletedAt` si todavia esta dentro de la ventana de gracia (7 dias); responde `404` si el estudiante no existe o no esta eliminado, `409` si la ventana ya expiro.
- [x] Job programado (`StudentPurgeScheduler`, diario 03:00) que purga definitivamente los registros con `deletedAt` mas antiguo que 7 dias. Si el estudiante tiene cargos de pago u otro registro protegido por `ON DELETE RESTRICT`, la purga de esa fila se omite (se loguea) en vez de fallar el job completo o perder historial financiero — queda soft-deleted indefinidamente. Verificado contra MySQL real: intentar borrar un estudiante con cargos da `Error 1451` (constraint), uno sin cargos se borra sin problema.
- [x] `GET /api/students?includeDeleted=true` para que administracion vea los eliminados recientes (incluye `deletedAt` en la respuesta).
- [x] Tests: soft-delete no borra la fila, restore dentro/fuera de la ventana, purga respeta registros protegidos por FK, `includeDeleted=true`. Verificado tambien end-to-end contra Docker real (crear, eliminar, confirmar oculto, restaurar, confirmar visible, forzar ventana expirada por SQL y confirmar 409).
- [x] Mismo patron aplicado a `Material` (`DELETE/POST restore /api/materials/{id}`, `MaterialPurgeScheduler` diario 03:15), `ScheduleSlot` (`DELETE/POST restore /api/schedules/{id}`, `ScheduleSlotPurgeScheduler` diario 03:30) y `Parent` (`DELETE/POST restore /api/parents/{id}`, `ParentPurgeScheduler` diario 03:45) — horarios escalonados para no chocar entre jobs.
- [x] `Parent` extendido con un tercer estado, "archivado" (2026-08-21): a diferencia de las otras 3 entidades, `Parent` no se purga a los 7 dias — pasa a un estado intermedio de retencion larga (6 anios) pensado para familias que podrian volver a inscribirse. Ver detalle en la seccion "Ciclo de vida extendido de Parent" mas abajo.

Con esto quedan las 4 entidades acordadas (Student, Material, ScheduleSlot, Parent) con soft-delete/restore/purge completo.

Esto desbloquea la funcion de frontend "confirmaciones para acciones sensibles" en su parte de eliminar estudiante (ver `frontend-preschool/docs/frontend-roadmap.md`).

### Ciclo de vida extendido de Parent (2026-08-21)

Pedido especifico del cliente: si una familia deja el preschool y despues vuelve (con el mismo hijo u otro), seria mejor poder recuperar el registro anterior del padre/tutor en vez de crear uno nuevo desde cero — asi se mantiene el historial de hijos vinculados (util para elegibilidad de descuentos o beneficios a futuro) y no hay que volver a crear cuenta de acceso.

Por eso `Parent` tiene 4 estados en vez de los 2 simples (activo/eliminado) de las otras entidades:

1. **Activo** — normal.
2. **Papelera (0-7 dias)** — igual que las otras 3 entidades: `deletedAt` seteado, datos completos intactos, visible con `includeDeleted=true`, deshacer total con `POST /api/parents/{id}/restore` (sin cambios respecto al comportamiento original).
3. **Archivado (dia 7 en adelante, nuevo)** — en vez de purgar a los 7 dias como las demas entidades, un job (`ParentPurgeScheduler.archiveExpiredSoftDeletedParents`, diario 03:45) minimiza el registro:
   - Se conservan: `firstName`, `lastName`, `email` (para poder buscar visualmente en el archivo) y la cuenta de login vinculada (`User`, con su contraseña intacta). El vinculo con los hijos (`student_guardians`) tambien se conserva automaticamente porque la fila del padre/tutor no se borra, solo se vacian campos.
   - Se limpian: `phone`, `address`, `preferredLanguage`, `notes`.
   - Se marca con el nuevo campo `archivedAt`.
4. **Purga definitiva** — recien 6 anios despues de `archivedAt` (no del soft-delete original), un job nuevo (`ParentPurgeScheduler.purgeExpiredArchivedParents`, diario 03:50) borra la fila para siempre, respetando el mismo bloqueo por FK que ya existia (si tiene consentimientos registrados, se omite y se loguea en vez de fallar).

Si la familia vuelve durante el estado archivado: nuevo endpoint `POST /api/parents/{id}/claim` — recibe los datos completos (nombre/email ya estaban, hay que rellenar telefono/direccion/etc.), limpia `deletedAt` y `archivedAt`, y reactiva el mismo `parentId` de siempre (por eso el historial de hijos queda vinculado). Responde `404` si el padre/tutor no esta archivado, `409` si ya pasaron los 6 anios desde `archivedAt`.

`POST /restore` no cambio: sigue siendo exclusivamente el "deshacer rapido" de los primeros 7 dias, ya rechaza automaticamente (409) una vez que el registro pasa a archivado, sin necesidad de tocar su logica.

Verificado end-to-end contra Docker real: eliminar, forzar ventana de 7 dias expirada por SQL, simular archivado (campos minimizados via SQL, nombre/email intactos), confirmar 404 en `GET` directo, confirmar visible con `includeDeleted=true` mostrando `archivedAt`, reclamar con `claim` y confirmar que vuelve a ser un parent activo normal, forzar `archivedAt` a 7 anios y confirmar `409` en `claim`, y confirmar que la purga final (`DELETE` directo en MySQL) no encuentra bloqueos de FK.

## Version inicial recomendada

La primera version debe construir una base funcional que permita validar si la aplicacion responde a las necesidades reales del preescolar. No se busca incluir todo desde el primer dia, sino empezar con lo mas importante y luego ampliar.

| Modulo | Incluido en primera version | Estado actual |
| --- | --- | --- |
| Estudiantes | Crear, editar, consultar y organizar estudiantes. | Parcialmente implementado. |
| Padres/tutores | Registrar responsables y conectarlos con cada estudiante. | Implementado en backend. |
| Pagos | Control mensual con estados pagado, pendiente y atrasado. | Implementado en backend. |
| Material escolar | Inventario basico con alertas de cantidad baja. | Implementado en backend. |
| Horarios | Organizacion basica por grupo y actividades. | Implementado en backend. |
| Dashboard | Resumen general de informacion clave. | Implementado en backend. |

## A. Administracion de estudiantes

### Criterios del cliente

- Registro de estudiantes activos, pendientes o dados de baja.
- Ficha individual con nombre, fecha de nacimiento, grupo/aula, datos de contacto y observaciones importantes.
- Vinculacion del estudiante con sus padres o tutores responsables.
- Espacio para informacion importante como alergias, notas medicas o contactos de emergencia.
- Notas internas sobre cada nino.
- Fotos de los ninos y posible album de fotos por estudiante.
- Recordatorio de cumpleanos proximos.
- Busqueda y filtros para encontrar rapidamente a un estudiante.

### Estado actual

- [x] Crear estudiante.
- [x] Listar estudiantes.
- [x] Consultar estudiante por id.
- [x] Actualizar estudiante.
- [x] Eliminar estudiante: soft-delete con restauracion y ventana de gracia de 7 dias — ver seccion "Prioridad actual: soft-delete y restauracion" arriba.
- [x] Estado del estudiante.
- [x] Grupo/aula mediante `groupId`.
- [x] Alergias, notas medicas y observaciones.
- [x] Vinculacion con padres/tutores mediante `student_guardians`.
- [x] Busqueda por nombre, codigo, grupo o estado: `GET /api/students?search=&groupId=&status=`.
- [x] Filtros formales por estado/grupo: mismos parametros `groupId` y `status`.
- [x] Contactos de emergencia como campo o entidad especifica: entidad `student_emergency_contacts` (nombre, relacion, telefono, telefono alterno, notas, contacto principal), endpoints `GET/POST/PUT/DELETE /api/students/{id}/emergency-contacts`.
- [x] Respuesta de ficha completa con tutores incluidos: `StudentResponse` ahora incluye `guardians` (lista completa de tutores con datos de contacto) ademas de `primaryGuardianName`, tanto en `GET /api/students` como en `GET /api/students/{id}`.
- [ ] Revisar si las notas actuales son suficientes o si se necesita historial de notas por fecha/usuario.
- [ ] Investigar almacenamiento de fotos: base de datos, filesystem local, S3/Cloudinary u otro proveedor.
- [ ] Definir modelo de album de fotos por estudiante.
- [ ] Endpoint para subir foto de estudiante.
- [ ] Endpoint para listar album de estudiante.
- [ ] Endpoint para eliminar foto de estudiante.
- [x] Endpoint o dashboard item para cumpleanos proximos: ya implementado en el dashboard (`upcomingBirthdays` en `teacher-summary` y `admin-summary`), ver seccion F.
- [x] Tests de controller/API.

### Resultado esperado

El personal podra consultar rapidamente la informacion de cada nino sin depender de papeles, mensajes antiguos o archivos separados.

## B. Administracion de padres o tutores

### Criterios del cliente

- Registro de padres, madres o tutores legales.
- Datos de contacto: telefono, correo y relacion con el estudiante.
- Posibilidad de asociar un tutor con uno o varios estudiantes.
- Identificacion del responsable principal de pagos o comunicaciones.

### Estado actual

- [x] Crear padre/madre/tutor.
- [x] Listar padres/tutores.
- [x] Buscar padres/tutores.
- [x] Consultar padre/tutor por id.
- [x] Actualizar padre/tutor.
- [x] Activar/desactivar padre/tutor.
- [x] Crear cuenta `User` con rol `PARENT`.
- [x] Consultar perfil propio con `/api/parents/me`.
- [x] Consultar estudiantes propios con `/api/parents/me/students`.
- [x] Asociar padre/tutor con uno o varios estudiantes.
- [x] Definir relacion: `FATHER`, `MOTHER`, `GUARDIAN`, `RELATIVE`, `OTHER`.
- [x] Marcar contacto principal.
- [x] Marcar responsable de pagos.
- [x] Marcar autorizado para recogida.
- [x] Marcar si vive con el estudiante.
- [x] Tests de servicio principales.
- [ ] Revisar payloads finales para frontend.
- [x] Agregar tests de controller/API: `ParentControllerApiTest` (filtros de admin, acceso propio de padre, rechazo sin autenticar).

### Resultado esperado

El centro sabra rapidamente a quien contactar y quien es responsable de cada estudiante.

## C. Administracion de pagos mensuales

### Criterios del cliente

- Registro de cuota mensual por estudiante.
- Estados claros: pagado, pendiente o atrasado.
- Historial de pagos por estudiante y por mes.
- Filtro por mes, estudiante o estado del pago.
- Posibilidad de registrar fecha de pago, metodo de pago y comentario administrativo.
- Metodos de pago iniciales: efectivo, tarjeta y transferencia.
- Opcional: generar recibo simple o comprobante en PDF en una fase posterior.

### Estado actual

- [x] La base de datos contiene tablas relacionadas con pagos y cargos.
- [x] Modelos Java para tipos de cargo, cargos de estudiante, pagos, asignaciones y staff.
- [x] Repositories de pagos, cargos, tipos de cargo, asignaciones y staff.
- [x] DTOs de pagos/cargos.
- [x] `PaymentService`.
- [x] `PaymentController`.
- [x] Endpoint para listar pagos por estudiante: `GET /api/payments/students/{studentId}`.
- [x] Endpoint para filtrar cargos por mes: `GET /api/payments/charges?month=YYYY-MM`.
- [x] Endpoint para filtrar cargos por estudiante o estado.
- [x] Endpoint para registrar pago: `POST /api/payments`.
- [x] Soportar metodo de pago: `CASH`, `CARD`, `TRANSFER`.
- [x] Calculo de saldo pendiente por cargo.
- [x] Actualizacion automatica de estado del cargo al registrar pagos.
- [x] Acceso de padre/tutor a sus propios pagos: `GET /api/payments/me`.
- [x] Acceso de padre/tutor a sus propios cargos: `GET /api/payments/me/charges`.
- [x] Seguridad por roles para `ADMIN`, `DIRECTOR`, `FINANCE` y `PARENT`.
- [x] Tests de servicio.
- [x] Actualizar `api-test.http`.
- [ ] Endpoint explicito para actualizar/cancelar estado de cargo sin registrar pago.
- [x] Reporte/resumen mensual de pagos pendientes y atrasados: `GET /api/payments/reports/monthly?month=YYYY-MM` (mes opcional, default el mes actual). Devuelve conteo, saldo y detalle de cargos pendientes y atrasados de ese mes, mas el total de pagos recibidos.
- [x] Tests de controller/API.
- [ ] Revisar optimizacion de queries si el volumen de pagos crece.
- [ ] Generacion de recibo simple o comprobante en PDF en fase posterior.

### Resultado esperado

El preescolar podra ver rapidamente quien ha pagado, quien esta pendiente y que pagos requieren seguimiento.

## D. Administracion de material escolar

### Criterios del cliente

- Inventario de materiales del centro: papeleria, limpieza, juguetes, comida u otras categorias.
- Cantidad disponible y cantidad minima recomendada.
- Alertas cuando un material este bajo o necesite reposicion.
- Registro de entradas y salidas de material.
- Responsable o comentario asociado al movimiento de material.

### Estado actual

- [x] La base de datos contiene tablas relacionadas con materiales y movimientos.
- [x] Modelos Java para materiales y movimientos.
- [x] Repositories para inventario y movimientos.
- [x] DTOs.
- [x] `MaterialService`.
- [x] `MaterialController`.
- [x] Endpoint para listar inventario: `GET /api/materials`.
- [x] Endpoint para crear/editar material.
- [x] Endpoint para registrar entrada de material.
- [x] Endpoint para registrar salida de material.
- [x] Endpoint para registrar ajuste por conteo fisico.
- [x] Endpoint para consultar movimientos.
- [x] Endpoint y filtro de materiales bajo stock minimo.
- [x] Eliminar material: soft-delete con restauracion y ventana de gracia de 7 dias, igual que estudiantes (`DELETE /api/materials/{id}`, `POST /api/materials/{id}/restore`, `GET /api/materials?includeDeleted=true`). A diferencia de estudiantes, `material_movements` tiene `ON DELETE CASCADE` (no `RESTRICT`), asi que la purga automatica si borra el historial de movimientos del material purgado — aceptable porque no es informacion financiera.
- [x] Stock minimo sugerido (2026-08-21): `GET /api/materials/{id}/suggested-minimum?window=WEEK|MONTH|THREE_MONTHS|SIX_MONTHS|TWELVE_MONTHS` — calcula el consumo real (movimientos `OUT`) en la ventana elegida y lo normaliza a un promedio mensual comparable. Es solo sugerencia, nunca escribe `minimumQuantity` automaticamente; el admin sigue decidiendo el valor final via `PUT /api/materials/{id}` como siempre. Sin movimientos en la ventana, devuelve `hasData: false` en vez de inventar un numero.
- [x] Nombre de quien hizo el movimiento (2026-08-21): `MaterialMovementResponse` gana `performedByName`, resuelto via `Staff` (si el usuario que hizo el movimiento es staff registrado); si no, queda `null` y se usa `performedByEmail` como antes.
- [x] Auditoria de ediciones al material (2026-08-21): cada `PUT /api/materials/{id}` guarda un snapshot antes/despues en `material_audit_log` (quien, cuando, valores previos y nuevos). Nuevo endpoint `GET /api/materials/{id}/audit-log`.
- [x] Retencion de historial (2026-08-21): job diario (`MaterialHistoryPurgeScheduler`, 04:00) que borra definitivamente `material_movements` y `material_audit_log` con mas de 3 anios de antiguedad — son registros operativos de inventario, no comprobantes fiscales, asi que no aplica la retencion legal de 10 anios de la DGII (RD) que si aplicaria a `payments`; por eso `payments` queda explicitamente fuera de cualquier borrado automatico por ahora.
- [x] Seguridad por roles internos.
- [x] Tests de servicio.
- [x] Actualizar `api-test.http`.
- [x] Tests de controller/API.
- [ ] Revisar categorias finales con el cliente.
- [ ] Revisar si se necesita responsable como staff especifico en vez de usuario autenticado.

### Resultado esperado

El centro podra prevenir faltas de material y planificar compras con mas control.

## E. Administracion de horarios

### Criterios del cliente

- Horarios por grupo o aula.
- Actividades del dia: entrada, comidas, siesta, recreo, actividades educativas y salida.
- Asignacion de personal responsable por actividad o grupo.
- Vista diaria o semanal para facilitar la planificacion.
- Espacio para eventos especiales o cambios puntuales.

### Estado actual

- [x] La base de datos contiene tabla relacionada con horarios.
- [x] Modelos Java para horarios y asignaciones de personal a grupo.
- [x] Repositories.
- [x] DTOs.
- [x] `ScheduleService`.
- [x] `ScheduleController`.
- [x] Endpoint para listar horarios: `GET /api/schedules`.
- [x] Endpoint para horarios por grupo: `GET /api/schedules/groups/{groupId}`.
- [x] Endpoint para horarios por dia: `GET /api/schedules/days/{dayOfWeek}`.
- [x] Endpoint para horarios por grupo y dia: `GET /api/schedules/groups/{groupId}/days/{dayOfWeek}`.
- [x] Endpoint para crear/editar actividad.
- [x] Endpoint para asignar responsable principal.
- [x] Endpoint para consultar/asignar personal a grupos.
- [x] Seguridad por roles internos.
- [x] Tests de servicio.
- [x] Tests de controller/API.
- [x] Actualizar `api-test.http`.

### Resultado esperado

El personal podra tener una vision clara de la organizacion diaria y semanal del preescolar.

## F. Dashboard principal

### Criterios del cliente

- Resumen de estudiantes activos.
- Pagos pendientes o atrasados del mes.
- Materiales con stock bajo.
- Horarios o actividades importantes del dia.
- Cumpleanos proximos de estudiantes.
- Accesos rapidos a las secciones principales.

### Estado actual

- [x] Crear DTO de resumen.
- [x] Crear `DashboardService`.
- [x] Crear `DashboardController`.
- [x] Endpoint principal `GET /api/dashboard/summary` para administracion/direccion.
- [x] Separar dashboard en endpoints `teacher-summary`, `admin-summary` y `finance-summary`.
- [x] Conteo de estudiantes activos.
- [x] Conteo/listado de pagos pendientes o atrasados del mes.
- [x] Dashboard financiero restringido a `SUPER_ADMIN`, `ADMIN`, `DIRECTOR` y `FINANCE`.
- [x] Conteo/listado de materiales con stock bajo.
- [x] Horarios o actividades importantes del dia.
- [x] Listado de cumpleanos proximos.
- [x] Tests de servicio.
- [x] Tests de controller/API.
- [x] Actualizar `api-test.http`.

### Resultado esperado

Al entrar en la aplicacion, el cliente vera lo mas importante sin tener que revisar modulo por modulo.

## Funciones para fases posteriores

- [ ] Portal para padres: consultar pagos, horarios o avisos del centro.
- [ ] Notificaciones automaticas para pagos pendientes o comunicados importantes.
- [ ] Registro de asistencia diaria.
- [ ] Reportes mensuales de pagos, estudiantes o inventario.
- [ ] Generacion de recibos y documentos en PDF.
- [ ] Notas estilo comentarios para estudiantes: profesores responsables pueden crear/editar sus comentarios; direccion/admin pueden revisar historial y moderar.
- [x] Backend base para foto de perfil por estudiante mediante `profilePhotoUrl`.
- [ ] Subida/almacenamiento real de foto de perfil por estudiante, visible segun permisos internos y consentimiento familiar.
- [ ] Album de fotos avanzado por estudiante o grupo, con permisos por grupo/estudiante asignado.
- [ ] Consentimientos de privacidad/imagen: padres o tutores deben aceptar condiciones antes de permitir uso de fotos del estudiante.
- [ ] Roles avanzados: administrador, profesor, contabilidad y padre/tutor.
- [ ] Reglas avanzadas para gestion de roles: definir quien puede crear usuarios, asignar roles, quitar roles y evitar que `ADMIN` o `DIRECTOR` puedan otorgar permisos superiores a los propios.
- [ ] Sistema de mensajes internos entre administracion y padres.

Nota: parte del portal para padres ya empezo con `/api/parents/me`, `/api/parents/me/students`, `/api/payments/me` y `/api/payments/me/charges`. Horarios y avisos para padres siguen pendientes.

### Modulo futuro - Notas, fotos y consentimientos

Este modulo debe tratarse como sensible porque puede incluir informacion personal de menores.

Estado actual:

- [x] Backend base para notas estilo comentarios con autor, tipo, fecha, moderacion y soft delete.
- [x] Profesores pueden gestionar notas solo de estudiantes cuyo grupo tienen asignado activamente.
- [x] Direccion/admin pueden revisar, moderar, actualizar o eliminar notas de cualquier estudiante.
- [x] Foto de perfil base disponible con URL en estudiante.
- [x] Backend base para consentimientos familiares por estudiante y tutor.
- [x] La foto de perfil requiere consentimiento activo `IMAGE_PROFILE_PHOTO`.
- [x] Backend base para albumes/fotos por URL, con aprobacion, borrado logico y permisos por grupo.
- [x] Las fotos asociadas a estudiante requieren consentimiento activo `PHOTO_ALBUM`.
- [ ] Historial detallado de edicion/auditoria avanzada para notas.
- [ ] UI de consentimientos familiares antes de habilitar foto de perfil/albumes en produccion.
- [ ] Almacenamiento real de archivos/imagenes para albumes.

Reglas iniciales deseadas:

- Las notas deben funcionar como comentarios con autor, fecha, tipo y posible historial de edicion.
- Profesores pueden crear y modificar notas solo para estudiantes o grupos bajo su responsabilidad.
- Direccion/admin pueden revisar, moderar o eliminar notas si es necesario.
- La foto de perfil del estudiante debe depender de consentimiento familiar activo.
- Los albumes pueden organizarse por estudiante, grupo, fecha, evento o album manual.
- Profesores pueden subir/modificar fotos solo de sus grupos o estudiantes asignados.
- Directores/admin pueden revisar, aprobar, eliminar o corregir fotos.
- Padres/tutores deben aceptar consentimiento de privacidad/imagen antes de habilitar fotos del estudiante.
- Debe existir forma de revocar consentimiento y definir que ocurre con fotos ya existentes.
- Debe quedar preparada auditoria basica: quien subio, modifico, elimino o aprobo contenido.

Endpoints futuros sugeridos:

```text
GET /api/students/{studentId}/notes
POST /api/students/{studentId}/notes
PUT /api/students/{studentId}/notes/{noteId}
PATCH /api/students/{studentId}/notes/{noteId}/moderate
DELETE /api/students/{studentId}/notes/{noteId}

POST /api/students/{studentId}/profile-photo
DELETE /api/students/{studentId}/profile-photo

GET /api/photo-albums
POST /api/photo-albums
GET /api/photo-albums/{albumId}
PUT /api/photo-albums/{albumId}
DELETE /api/photo-albums/{albumId}
POST /api/photo-albums/{albumId}/photos
PATCH /api/photo-albums/{albumId}/photos/{photoId}/approve
DELETE /api/photo-albums/{albumId}/photos/{photoId}

GET /api/students/{studentId}/consents
POST /api/students/{studentId}/consents
PATCH /api/students/{studentId}/consents/{consentId}/revoke
```

## Flujo de uso esperado

1. El administrador entra al sistema y ve el dashboard principal.
2. Puede revisar rapidamente pagos pendientes, materiales bajos y actividades del dia.
3. Desde estudiantes puede consultar o actualizar la informacion de cada nino.
4. Desde pagos puede registrar cuotas mensuales y revisar deudas.
5. Desde materiales puede actualizar entradas, salidas y necesidades de compra.
6. Desde horarios puede organizar la rutina diaria o semanal del preescolar.

## Puntos a validar con el cliente

- [ ] Que datos exactos necesitan guardar de cada estudiante.
- [ ] Que tipos de comentarios/notas necesitan: pedagogicas, conducta, incidentes, seguimiento familiar, salud o administrativas.
- [ ] Como manejan actualmente los pagos y si hay diferentes tipos de cuota.
- [ ] Confirmar si los metodos de pago son solo efectivo, tarjeta y transferencia.
- [ ] Confirmar si "transferencia" necesita numero de referencia, banco o comprobante.
- [ ] Confirmar politica de privacidad y permisos para almacenar fotos de ninos.
- [ ] Confirmar flujo de consentimiento: que tutor acepta, como se revoca y que pasa con fotos ya subidas.
- [ ] Confirmar si las fotos se organizan por estudiante, grupo, fecha, evento o album manual.
- [ ] Confirmar si profesores solo pueden modificar fotos/notas de sus propios grupos o estudiantes asignados.
- [ ] Confirmar cuantos dias antes debe avisar el sistema de cumpleanos proximos.
- [ ] Si los padres necesitan acceso directo a la aplicacion desde la primera version o mas adelante.
- [ ] Que tipos de materiales quieren controlar en el inventario.
- [ ] Como se organizan los grupos, aulas y horarios actualmente.
- [ ] Quienes usaran el sistema: administracion, profesores, contabilidad o padres.
- [ ] Si necesitan documentos imprimibles, recibos o reportes desde el inicio.

## Infraestructura y calidad

- [x] Flyway configurado.
- [x] Baseline aplicado sobre esquema existente.
- [x] Seed de roles versionado.
- [x] `application-local.properties` fuera del control de versiones.
- [x] Workaround para archivos AppleDouble `._*` en volumen exFAT.
- [x] `api-test.http` actualizado con flujos principales.
- [x] `api-test.http` actualizado con flujo base de pagos mensuales.
- [x] `api-test.http` actualizado con flujo base de horarios.
- [x] Smoke tester automatico para endpoints principales.
- [x] Smoke tester con logs locales, retencion de ultimos 4 logs y modo read-only.
- [x] Mockito configurado como Java agent para tests en Java 25.
- [x] Agregar futuras migraciones `V3`-`V7` para nuevos cambios de esquema (notas, consentimientos, albumes, contactos de emergencia); `V1` agregada ademas como baseline real para bases de datos limpias.
- [x] Mejorar cobertura de tests de controllers: agregados `AuthControllerApiTest`, `RoleControllerApiTest`, `UserControllerApiTest` y `PhotoAlbumControllerApiTest` (antes sin tests de controller propios).
- [x] Revisar `open-in-view` de JPA: se probo `false` y rompio la mayoria de endpoints (`LazyInitializationException` sobre asociaciones como `Parent`); se mantiene `true` explicito con comentario. Ver seccion "Checklist para release oficial".
- [ ] Revisar warnings de Mockito/Java agent en Java 25.
- [ ] Revisar warning de Flyway con MySQL 9.5.
- [x] Corregir que peticiones denegadas por rol devolvian `401` en vez de `403` en la app real (no se detectaba en tests `@WebMvcTest`): `response.sendError()` disparaba un forward interno a `/error` sin reautenticar, sobreescribiendo el status. Fix: `/error` marcado `permitAll()`. Verificado con un test de integracion con servidor real (`SecurityErrorDispatchIntegrationTest`) que falla sin el fix y pasa con el.

## Propuesta de cierre

Construir una primera version enfocada en administracion interna: estudiantes, tutores, pagos, materiales, horarios y dashboard. Despues de probarla con el uso real del centro, se podran ajustar flujos y anadir funciones como portal de padres, notificaciones, asistencia y reportes avanzados.

## Checklist para release oficial

Antes de publicar la aplicacion para uso real del cliente:

- [x] Preparar una base de datos limpia para produccion: apuntar Flyway a un MySQL vacio real (no reusar `docker/mysql/init/`, que es solo para desarrollo local y siembra datos demo). Verificado que una base vacia migra completa via Flyway sin datos demo.
- [ ] Si alguna vez se corrio `api-smoke-test.mjs` en modo escritura contra un entorno compartido/staging, limpiar los residuos `SMOKE-*` con `scripts/cleanup-smoke-data.sql` (revisar manualmente `staff_group_assignments`, que no tiene campo distintivo para filtrar por patron).
- [x] Aplicar migraciones Flyway desde cero y confirmar que el esquema queda completo: agregado `V1__initial_schema.sql`, verificado contra base de datos vacia (crea las 20 tablas) y contra el flujo existente de docker-compose (Flyway hace baseline en vez de re-ejecutar V1).
- [x] Mantener solo seeds necesarios para roles base y datos imprescindibles del sistema: `V2__seed_roles.sql` solo siembra los 6 roles base, sin datos demo dentro de Flyway.
- [ ] Crear usuario administrador inicial para el cliente. (Requiere entorno/credenciales reales del cliente; pendiente hasta tener el destino de despliegue.)
- [ ] Configurar `JWT_SECRET` real y suficientemente largo. (La app ahora falla al arrancar si falta; pendiente configurarlo en el entorno real de despliegue.)
- [ ] Configurar credenciales reales de base de datos y no usar passwords de desarrollo. (Idem: depende del entorno real de despliegue.)
- [x] Confirmar que `application-local.properties`, `.env` y secretos no se suben al repositorio. Verificado en `.gitignore` y `git check-ignore`.
- [x] Ejecutar `./mvnw test`. 71/71 passing.
- [ ] Ejecutar `API_SMOKE_READ_ONLY=true node scripts/api-smoke-test.mjs` contra el entorno final o staging. (Solo corrido contra local/docker; no existe todavia un staging real.)
- [x] Revisar warnings importantes de runtime antes de entregar. Ver seccion "Infraestructura y calidad".
- [x] Documentar URL, usuario inicial y pasos basicos de operacion para el cliente: ver `docs/operations-runbook.md` (incluye el procedimiento para crear el primer admin en una base de datos limpia, verificado end-to-end).

## Proximo paso recomendado

Iniciar una revision pre-release del backend: comprobar configuracion, seguridad, warnings conocidos, datos demo, smoke tester, documentacion operativa y checklist de entrega antes de pasar al frontend o a nuevas funciones.
