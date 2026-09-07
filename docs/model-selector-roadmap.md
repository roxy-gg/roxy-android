# Selector de modelos: evaluación y roadmap

Evaluación del 7 de septiembre de 2026 sobre el código local de Android y `../roxy` (escritorio). No se verificó qué versión está desplegada en producción ni el código del relay.

## Decisión

**Complejidad media-alta para una implementación completa.** El catálogo y la configuración por sesión ya existen en escritorio, pero no están expuestos al móvil. Se necesita ampliar el contrato remoto, implementarlo en escritorio y Android, revisar el relay y validar la sincronización entre clientes. No es un cambio aislado del componente visual.

Siguiendo la alternativa solicitada, se retiró la demo de Android: lista fija, selección local, buscador, panel y modelos ficticios. El compositor indica que utiliza el modelo de la sesión configurado en escritorio. El envío actual sigue funcionando igual.

## Evidencia del código

| Ubicación | Comportamiento encontrado |
| --- | --- |
| `app/src/main/java/gg/roxy/chatFullscreen/components/ChatComposer.kt` (antes de esta limpieza) | `DesktopRoxyModels` estaba escrito a mano y `currentModel` solo vivía en `rememberSaveable`. No había callback hacia el ViewModel. |
| `app/src/main/java/gg/roxy/shared/data/RemoteWorkspaceClient.kt` | `sendPrompt` envía únicamente `{ "t": "prompt", "text": "..." }`. No recibe catálogos ni confirmaciones de cambio de modelo. |
| `../roxy/src/main/services/remote-protocol.ts` | `GuestFrame` admite `prompt`, `abort`, `list`, `switch` y `dequeue`; no hay operaciones para modelos. |
| `../roxy/src/main/services/remote.ts` | `onFrame` no maneja selección de modelos. `runTurn` resuelve la configuración de la sesión y elige proveedor/modelo en el PC. Añadir un campo al prompt de Android sería ignorado por este host. |
| `../roxy/src/main/services/models.ts` | `listModels(providerId)` ya obtiene catálogos por proveedor, incluidos catálogos autenticados y proxies. Es la base que debe reutilizarse. |
| `../roxy/src/shared/session-config.ts` | `resolveSessionConfig` trata proveedor y modelo como pareja y mantiene configuración por sesión. También existen reglas para contexto y esfuerzo de razonamiento. |
| `../roxy/src/main/db/repo.ts` | Ya existen `listConnectedProviders` y `setChatConfig` para consultar proveedores conectados y guardar configuración. |

Un catálogo de un proveedor conectado no siempre acredita acceso efectivo a cada modelo: algunas rutas usan catálogos públicos. La interfaz debe reflejar las restricciones que conozca el host y mostrar rechazos reales del proveedor, sin prometer permisos que no puede comprobar.

## Implementación propuesta, en orden

### 1. Definir contrato y compatibilidad

- Anunciar una capacidad opcional, por ejemplo `model-selection-v1`. Los clientes nuevos ocultan el selector si el host no la anuncia; los clientes antiguos deben seguir funcionando.
- Definir consulta de catálogo, selección y respuesta confirmada. Ejemplo de petición: `{ "t": "select-model", "requestId": "r1", "sessionId": "s1", "providerId": "p1", "modelId": "m1", "expectedRevision": 3 }`.
- Responder con `requestId`, `sessionId`, la pareja efectiva proveedor/modelo y una revisión, o un error explícito. Los nombres de mensajes de este documento son propuestas, no API existente.
- Incluir en el catálogo identificadores estables, nombre visible, proveedor, capacidades y disponibilidad conocida. Nunca enviar credenciales.
- Revisar en el relay las listas de tipos permitidos, validación por rol y límites de tamaño. Los comentarios del host lo describen como un intermediario de JSON, pero falta comprobar si admite tipos nuevos. Actualizar también el contrato del cliente web.

Criterio de salida: contrato documentado y prueba de que los mensajes recorren el relay en ambos sentidos.

### 2. Implementar el host de escritorio

- Construir el catálogo desde `listConnectedProviders()` y `listModels(providerId)`, reutilizando sus cachés y filtros. Distinguir un proveedor sin modelos de una consulta fallida; permitir resultados parciales y reintento.
- Resolver y publicar la selección efectiva con la misma lógica que ejecuta los turnos, incluidos los valores por defecto. Evitar un segundo resolver que pueda mostrar un modelo distinto del ejecutado.
- Validar sesión, proveedor conectado, modelo y revisión; persistir la pareja con `setChatConfig`. Alinear los valores por defecto para sesiones nuevas con la política del selector de escritorio.
- Confirmar solo después de guardar. Notificar a escritorio y móviles cuando cambie la configuración, también si el cambio se origina en el PC.
- Publicar el estado al conectar, cambiar de sesión y reconectar. Descartar respuestas de consultas pertenecientes a una conexión o sesión anterior.
- Para el primer alcance, rechazar cambios mientras la sesión tenga un turno o una cola pendiente. Así no cambia inesperadamente el modelo de mensajes ya enviados. Implementar la misma regla en ambos clientes; el host debe hacerla cumplir.

Criterio de salida: una selección confirmada determina el modelo del siguiente turno y no modifica otras sesiones.

### 3. Conectar Android

- Añadir DTOs y eventos de catálogo/configuración en `RemoteModels.kt` y `RemoteWorkspaceClient.kt`.
- Mantener catálogo, selección confirmada y petición pendiente en `RoxyAppViewModel`, identificados por conexión y sesión. Limpiar los datos al cambiar de PC o desconectar.
- Extender `ChatFullScreenUiState` y conectar callbacks a través de `MainActivity`, `RoxyApp` y `ChatFullScreen` hasta `ChatComposer`.
- Recuperar el selector con datos del host: búsqueda, grupos por proveedor, carga, catálogo vacío, error y reintento. Usar `(providerId, modelId)` como identidad: un mismo nombre puede existir en varios proveedores.
- Mantener marcada la selección confirmada mientras se guarda otra. Bloquear envío durante el cambio pendiente para que el mensaje no salga con el modelo anterior por una carrera. Ante error o timeout, conservar la selección confirmada y consultar de nuevo.
- Reflejar cambios realizados desde el PC y separar correctamente el estado de dos sesiones. Posponer favoritos y adornos hasta tener sincronización real.

Criterio de salida: el móvil muestra el catálogo recibido y nunca presenta una selección local como si ya estuviera aplicada en el PC.

### 4. Validar y publicar

- Pruebas de protocolo: catálogo vacío/parcial, errores, confirmación, timeout, tipos desconocidos y compatibilidad con host antiguo.
- Pruebas del host: proveedor desconectado, modelo inválido, sesión inexistente, revisión obsoleta, persistencia y aislamiento entre sesiones.
- Pruebas Android: abrir A, cambiar a B antes de recibir la respuesta de A, cambiar de PC, reconectar y recibir cambios desde escritorio.
- Prueba de integración: seleccionar en Android y comprobar los argumentos `providerId` y `model` recibidos por `runSessionTurn`; no basta con verificar la etiqueta.
- Prueba con PC y móvil reales: varios proveedores, cambio de selección en ambos extremos, reconexión y rechazo durante ejecución/cola.
- Publicar primero el soporte compatible del relay si hace falta, después el host y finalmente Android. Mantener oculto el selector para versiones sin soporte.

## Estimación orientativa

Entre **2 y 4 sesiones de trabajo** para contrato/relay, host, Android y validación conjunta, suponiendo acceso a los repositorios y un entorno de prueba. No es una garantía de plazo: depende especialmente de las restricciones del relay y de cómo se propagan los cambios de configuración del escritorio.

Primer entregable recomendable: catálogo real y modelo efectivo en modo lectura, anunciado por capacidad. Segundo: selección persistida y confirmada con prueba de ejecución. El selector interactivo se habilita cuando ambos estén completos.
