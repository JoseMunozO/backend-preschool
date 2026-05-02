# Roadmap funcional - App de administracion para preescolar

Documento vivo para alinear el backend con la propuesta validada con el cliente. Resume objetivo, alcance funcional, estado actual, pendientes y orden recomendado de implementacion.

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
- [ ] Administracion de material escolar.
- [ ] Administracion de horarios.
- [ ] Dashboard principal.

## Version inicial recomendada

La primera version debe construir una base funcional que permita validar si la aplicacion responde a las necesidades reales del preescolar. No se busca incluir todo desde el primer dia, sino empezar con lo mas importante y luego ampliar.

| Modulo | Incluido en primera version | Estado actual |
| --- | --- | --- |
| Estudiantes | Crear, editar, consultar y organizar estudiantes. | Parcialmente implementado. |
| Padres/tutores | Registrar responsables y conectarlos con cada estudiante. | Implementado en backend. |
| Pagos | Control mensual con estados pagado, pendiente y atrasado. | Implementado en backend. |
| Material escolar | Inventario basico con alertas de cantidad baja. | Pendiente como API. |
| Horarios | Organizacion basica por grupo y actividades. | Pendiente como API. |
| Dashboard | Resumen general de informacion clave. | Pendiente. |

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
- [x] Eliminar estudiante.
- [x] Estado del estudiante.
- [x] Grupo/aula mediante `groupId`.
- [x] Alergias, notas medicas y observaciones.
- [x] Vinculacion con padres/tutores mediante `student_guardians`.
- [ ] Busqueda por nombre, codigo, grupo o estado.
- [ ] Filtros formales por estado/grupo.
- [ ] Contactos de emergencia como campo o entidad especifica.
- [ ] Respuesta de ficha completa con tutores incluidos.
- [ ] Revisar si las notas actuales son suficientes o si se necesita historial de notas por fecha/usuario.
- [ ] Investigar almacenamiento de fotos: base de datos, filesystem local, S3/Cloudinary u otro proveedor.
- [ ] Definir modelo de album de fotos por estudiante.
- [ ] Endpoint para subir foto de estudiante.
- [ ] Endpoint para listar album de estudiante.
- [ ] Endpoint para eliminar foto de estudiante.
- [ ] Endpoint o dashboard item para cumpleanos proximos.
- [ ] Tests de controller/API.

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
- [ ] Agregar tests de controller/API.

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
- [ ] Reporte/resumen mensual de pagos pendientes y atrasados.
- [ ] Tests de controller/API.
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
- [ ] Crear modelos Java necesarios si faltan.
- [ ] Crear repositories.
- [ ] Crear DTOs.
- [ ] Crear `MaterialService`.
- [ ] Crear `MaterialController`.
- [ ] Endpoint para listar inventario.
- [ ] Endpoint para crear/editar material.
- [ ] Endpoint para registrar entrada de material.
- [ ] Endpoint para registrar salida de material.
- [ ] Endpoint para consultar movimientos.
- [ ] Endpoint o filtro de materiales bajo stock minimo.
- [ ] Tests de servicio.
- [ ] Tests de controller/API.
- [ ] Actualizar `api-test.http`.

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
- [ ] Crear modelos Java necesarios si faltan.
- [ ] Crear repositories.
- [ ] Crear DTOs.
- [ ] Crear `ScheduleService`.
- [ ] Crear `ScheduleController`.
- [ ] Endpoint para horarios por grupo.
- [ ] Endpoint para horarios por dia.
- [ ] Endpoint para horarios por semana.
- [ ] Endpoint para crear/editar actividad.
- [ ] Endpoint para asignar responsable.
- [ ] Tests de servicio.
- [ ] Tests de controller/API.
- [ ] Actualizar `api-test.http`.

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

- [ ] Crear DTO de resumen.
- [ ] Crear `DashboardService`.
- [ ] Crear `DashboardController`.
- [ ] Conteo de estudiantes activos.
- [ ] Conteo/listado de pagos pendientes o atrasados del mes.
- [ ] Conteo/listado de materiales con stock bajo.
- [ ] Horarios o actividades importantes del dia.
- [ ] Listado de cumpleanos proximos.
- [ ] Tests de servicio.
- [ ] Tests de controller/API.
- [ ] Actualizar `api-test.http`.

### Resultado esperado

Al entrar en la aplicacion, el cliente vera lo mas importante sin tener que revisar modulo por modulo.

## Funciones para fases posteriores

- [ ] Portal para padres: consultar pagos, horarios o avisos del centro.
- [ ] Notificaciones automaticas para pagos pendientes o comunicados importantes.
- [ ] Registro de asistencia diaria.
- [ ] Reportes mensuales de pagos, estudiantes o inventario.
- [ ] Generacion de recibos y documentos en PDF.
- [ ] Album de fotos avanzado por estudiante o grupo.
- [ ] Roles avanzados: administrador, profesor, contabilidad y padre/tutor.
- [ ] Sistema de mensajes internos entre administracion y padres.

Nota: parte del portal para padres ya empezo con `/api/parents/me`, `/api/parents/me/students`, `/api/payments/me` y `/api/payments/me/charges`. Horarios y avisos para padres siguen pendientes.

## Flujo de uso esperado

1. El administrador entra al sistema y ve el dashboard principal.
2. Puede revisar rapidamente pagos pendientes, materiales bajos y actividades del dia.
3. Desde estudiantes puede consultar o actualizar la informacion de cada nino.
4. Desde pagos puede registrar cuotas mensuales y revisar deudas.
5. Desde materiales puede actualizar entradas, salidas y necesidades de compra.
6. Desde horarios puede organizar la rutina diaria o semanal del preescolar.

## Puntos a validar con el cliente

- [ ] Que datos exactos necesitan guardar de cada estudiante.
- [ ] Como manejan actualmente los pagos y si hay diferentes tipos de cuota.
- [ ] Confirmar si los metodos de pago son solo efectivo, tarjeta y transferencia.
- [ ] Confirmar si "transferencia" necesita numero de referencia, banco o comprobante.
- [ ] Confirmar politica de privacidad y permisos para almacenar fotos de ninos.
- [ ] Confirmar si las fotos se organizan por estudiante, grupo, fecha, evento o album manual.
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
- [ ] Agregar futuras migraciones `V3`, `V4`, etc. para nuevos cambios de esquema o seeds.
- [ ] Mejorar cobertura de tests de controllers.
- [ ] Revisar `open-in-view` de JPA.
- [ ] Revisar warnings de Mockito/Java agent en Java 25.
- [ ] Revisar warning de Flyway con MySQL 9.5.

## Propuesta de cierre

Construir una primera version enfocada en administracion interna: estudiantes, tutores, pagos, materiales, horarios y dashboard. Despues de probarla con el uso real del centro, se podran ajustar flujos y anadir funciones como portal de padres, notificaciones, asistencia y reportes avanzados.

## Proximo paso recomendado

Implementar `D. Administracion de material escolar`, porque pagos mensuales ya tiene una API base funcional y el inventario es el siguiente bloque operativo de la primera version.
