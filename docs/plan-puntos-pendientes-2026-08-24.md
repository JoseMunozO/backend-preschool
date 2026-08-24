# Plan — puntos pendientes del roadmap (2026-08-24)

Respuesta a los 6 puntos que José señaló en `docs/roadmap.md`. Los números de línea que él dio no
coinciden exactamente con los actuales (el archivo se ha editado mucho hoy), pero el contenido de
cada uno se identificó sin ambigüedad por el texto.

## 1. Historial de notas por fecha/usuario

**Item:** "Revisar si las notas actuales son suficientes o si se necesita historial de notas por
fecha/usuario."

**Estado real:** confirmado, es un hueco genuino. Hoy `StudentNote` no guarda historial — al editar
una nota (`PUT /api/students/{id}/notes/{noteId}`), el contenido anterior se pierde, solo queda
`updatedAt` actualizado. No hay forma de ver quién cambió qué y cuándo, más allá del autor original.

Esto es literalmente el mismo pedido que ya existía más abajo en el roadmap como "Historial detallado
de edición/auditoría avanzada para notas" — son el mismo punto repetido en dos secciones.

**Propuesta:** mismo patrón que ya existe para materiales (`material_audit_log`, `MaterialAuditLog`,
`GET /api/materials/{id}/audit-log`) — un snapshot antes/después en cada edición de nota, con quién y
cuándo. Nuevo endpoint `GET /api/students/{studentId}/notes/{noteId}/audit-log`.

**Esfuerzo:** bajo-medio (patrón ya probado en este mismo proyecto, unas horas).

## 2. Revisar payloads finales para frontend

**Item:** "Revisar payloads finales para frontend" (sección de padres/tutores).

**Estado real:** dado todo lo coordinado hoy con frontend (contactos de emergencia, lista filtrada
por grupo, vinculación estudiante-tutor, descuentos, etc.), este punto probablemente ya está cubierto
en la práctica — frontend no reportó ningún problema de formato/payload en toda la coordinación de
hoy. Voy a preguntarle directamente para cerrar el punto con su confirmación explícita, en vez de
asumirlo.

**Esfuerzo:** ninguno si frontend confirma que está bien; ajustes puntuales si señalan algo.

## 3. Explicación: "Revisar optimización de queries si el volumen de pagos crece"

Esto **no es una funcionalidad pendiente**, es una nota técnica para el futuro. Hoy varios métodos de
`PaymentService` (ej. `getPayments`, `getCharges`, `getMonthlyReport`) traen **todos** los pagos/cargos
de la base de datos con `findAll()` y después filtran en memoria (Java streams) en vez de filtrar
directamente en la consulta SQL. Con pocos cientos de registros esto no se nota, pero si el volumen de
pagos crece mucho (años de historial, muchos estudiantes), esas consultas se van a poner lentas porque
cargan de más.

**No es necesario resolverlo ahora** — el volumen actual es pequeño. Es un recordatorio para revisitar
más adelante (cambiar a queries con filtros en la base de datos en vez de en memoria) si el preescolar
crece mucho o el sistema empieza a sentirse lento en el módulo de pagos.

## 4. Recibos en PDF — aclaración técnica importante antes de diseñar la solución

Lo que describiste (generar el PDF, mandarlo al padre, Y ADEMÁS guardar automáticamente una copia en
una carpeta que se crea sola en la PC del usuario, con sincronización "la próxima vez que inicie sesión
en la PC" si se hizo desde el móvil) **no es algo que una aplicación web pueda hacer**. Un navegador no
tiene permiso para crear carpetas ni escribir archivos en el sistema de archivos del dispositivo por su
cuenta — eso es una restricción de seguridad de todos los navegadores (Chrome, Safari, etc.), no una
limitación de esta app en particular. Para lograr eso literal haría falta una aplicación de escritorio
nativa instalada en cada PC (como Electron o similar), que es un tipo de proyecto completamente distinto
al que tenemos hoy (una app web).

**Lo que sí se puede hacer, y logra el mismo objetivo real (recibo disponible desde cualquier
dispositivo, sin depender de sincronizar carpetas a mano):**

1. Al registrar un pago, el backend genera el PDF del recibo automáticamente.
2. Se guarda en el servidor (mismo mecanismo que ya usamos para las fotos —
   `FileStorageService`, filesystem local por ahora, migrable a la nube después sin rehacer nada).
3. Queda disponible para descargar desde la app en cualquier dispositivo (PC o celular) donde el padre
   o el admin inicien sesión — no hace falta "sincronizar" nada, porque el archivo vive en el servidor,
   no en un dispositivo particular.
4. Enviarlo automáticamente por correo al padre es un paso aparte: **hoy la aplicación no tiene ninguna
   configuración de envío de correos** (no hay servidor SMTP conectado). Si se quiere ese envío
   automático, hay que decidir con qué proveedor (Gmail SMTP, SendGrid, Amazon SES, etc.) y configurar
   credenciales reales — no es algo que se pueda dejar "listo" sin esa decisión externa.

**Esfuerzo:**
- Generar y guardar el PDF + descargarlo desde la app: medio (nueva librería de PDF en el proyecto,
  diseño simple de plantilla de recibo, endpoint de descarga). Es factible para mañana si se prioriza.
- Envío automático por correo: depende de tener un proveedor de correo elegido y sus credenciales —
  no es bloqueante para lo demás, se puede dejar como paso 2 una vez decidido el proveedor.

## 5. Aviso de cumpleaños próximos — ya implementado

**Item:** "Confirmar cuántos días antes debe avisar el sistema de cumpleaños próximos."

Ya está resuelto y ya estaba en el sistema desde antes de esta conversación: el dashboard
(`teacher-summary` y `admin-summary`) ya muestra `upcomingBirthdays` con una ventana de
**30 días** (`BIRTHDAY_LOOKAHEAD_DAYS = 30` en `DashboardService`) — exactamente lo que pediste
("aproximadamente un mes antes"). No hace falta ningún cambio, solo marcar el punto del roadmap como
resuelto.

## 6. Documentos imprimibles

**Item:** "Si necesitan documentos imprimibles, recibos o reportes desde el inicio."

Esto se resuelve en gran parte con el punto 4 (recibo en PDF) — un PDF ya es "imprimible" (se abre y
se manda a imprimir desde cualquier dispositivo). Si además necesitan otros documentos imprimibles
específicos (ej. listado de estudiantes por grupo, reporte mensual de pagos en PDF en vez de solo JSON),
eso es un alcance más grande que un recibo individual — decir cuáles hacen falta, si los hay, para
dimensionarlo aparte.

---

## Prioridad sugerida para mañana (dado el deadline)

1. **Historial de notas** — esfuerzo bajo, patrón ya conocido, cierra un pedido genuino del cliente.
2. **Recibo en PDF (generar + guardar + descargar)** — el más valioso de los pendientes grandes, factible
   si se prioriza hoy mismo.
3. **Revisar payloads con frontend** — solo requiere su confirmación, no desarrollo nuevo probablemente.
4. **Envío de correo automático** y **reportes/documentos imprimibles adicionales** — quedan para después,
   dependen de decisiones que no se pueden resolver solo con código (proveedor de correo, qué reportes
   exactos hacen falta).

Cumpleaños ya resuelto, optimización de queries es solo una nota para el futuro — ninguno de los dos
requiere trabajo ahora.
