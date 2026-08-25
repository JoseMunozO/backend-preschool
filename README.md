# Preschool Admin — Backend

Antes de este proyecto, administrar el preescolar significaba pagos anotados en cuadernos o Excel
sueltos, inventario de materiales que nadie sabia si alcanzaba, y asistencia que vivia en papel.
Esta API es el motor que centraliza esa operacion diaria: quien pago y quien debe, que material
queda y cuando reponerlo, quien vino hoy y quien no, y quien es responsable de cada nino — todo
consultable al instante en vez de reconstruido a mano al final del mes.

Es la mitad backend de una aplicacion completa (API REST en Spring Boot + interfaz web en React).
La UI vive en el repo hermano [`frontend-preschool`](https://github.com/JoseMunozO/frontend-preschool)
y consume esta API para todo: no hay logica de negocio duplicada del lado del cliente. Este
documento explica que resuelve el sistema hoy; el detalle de cada decision (por que se hizo asi, que
pidio el cliente, que se probo y descarto) vive en [`docs/roadmap.md`](docs/roadmap.md).

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

## Que resuelve, en la practica

**Un director o administrador** abre el dashboard y ve de un vistazo lo que antes tomaba llamadas y
revisiones manuales: cuantos pagos estan atrasados y cuanto se debe (con la multa por atraso ya
calculada, no algo que alguien tiene que sumar aparte), que materiales se estan por acabar, y que
cumpleanos se acercan. Puede dar de alta o de baja personal, asignar roles sin inventar cuentas
nuevas para cada combinacion de responsabilidades, y — si algo se borro por error, un estudiante, un
padre, un material — recuperarlo, porque cada eliminacion tiene una ventana real para deshacerse
antes de perderse de verdad.

**Un profesor** entra y ve solo lo suyo: los estudiantes de su grupo, no los de todo el preescolar.
Marca la asistencia del dia (una vez pasada la medianoche, ese registro queda archivado y ya no se
puede alterar, para que "asistencia de ayer" signifique lo que paso, no lo que alguien decidio
despues). Deja notas sobre un estudiante — de conducta, de salud, pedagogicas — sabiendo que solo el
puede editar las suyas, y que si otro profesor lo reemplaza un dia, puede leer el historial completo
de cada nota con quien cambio que y cuando, no solo la version mas reciente.

**Finanzas** ve la mensualidad, el comedor y cualquier otro cobro por estudiante, registra pagos
(parciales o completos, uno o varios cargos a la vez) y genera el recibo en PDF al instante. Si un
cargo especifico necesita un descuento — una beca, un caso de hermanos — se aplica a esa factura
puntual, no a todo lo que ese estudiante debe ese mes; la mensualidad y el comedor no se mezclan por
accidente.

**Un padre o tutor** entra a su propio portal y ve unicamente a sus hijos: sus pagos, su asistencia,
nada de otros estudiantes.

Todo esto corre sobre las mismas reglas de acceso en cada capa (no solo "esta ruta requiere tal
rol", sino "este profesor solo ve los grupos que tiene asignados hoy"), y todo lo que se elimina
pasa primero por una papelera recuperable antes de purgarse — decision explicita para que un clic
equivocado no borre historial que despues hace falta.

## Modulos

| Modulo | Que cubre |
| --- | --- |
| Estudiantes | Ficha completa, grupo, foto de perfil, alergias/notas medicas, notas de profesor con auditoria, consentimientos de imagen, albumes de fotos. |
| Padres / tutores | Alta, vinculo con uno o varios estudiantes, portal propio, ciclo de vida extendido (papelera -> archivado 6 anos -> purga) pensado para familias que se van y vuelven. |
| Pagos | Cargos configurables, generacion mensual automatica con prorrateo, pagos con asignacion a uno o varios cargos, recibos/facturas en PDF, descuentos por cargo especifico, multa por atraso calculada en tiempo real. |
| Materiales | Inventario con entradas/salidas/ajustes, alerta de stock bajo, historial y auditoria, sugerencia de cantidad minima segun consumo. |
| Horarios y asistencia | Horario semanal por grupo, asistencia diaria bloqueada tras medianoche, historial por estudiante. |
| Reportes | Seis vistas con acceso segun rol: financiero, asistencia, historial de notas, movimientos de materiales, salud/alergias, y papeleras unificadas. |
| Roles y personal | Seis roles con jerarquia por rango, alta/baja de personal reactivable sin perder historial. |
| Dashboard | Resumen especifico por rol (profesor, finanzas, administracion). |

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
