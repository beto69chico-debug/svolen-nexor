# Svölen-Nexor

Asistente administrativo para Android: clientes, entregas y cobranza. Hermana de Ya Merito y ParqTime, hecha por Tecnobasics.

- **Clientes** con color y contacto (llamar, WhatsApp, correo).
- **Proyectos** con hitos de entrega configurables y estado (pendiente, en curso, en revisión, entregado, cancelado).
- **Cobranza**: monto total sin IVA, plan de parcialidades con fecha, pagos reales, cobrado vs. pendiente y vencido.
- **Servicios recurrentes**: dominios (aviso para renovarlo y para cobrarlo), hosting, pólizas de mantenimiento y otros.
- **Avisos** configurables (por defecto 30, 15 y 3 días antes para cobros y servicios; 7, 3 y 1 para entregas) a la hora que elijas.
- **Timeline** de todo lo pendiente y **resumen** del mes al abrir.
- **Recordatorio de cobro por WhatsApp** ya redactado; tú lo revisas y lo envías.
- **Respaldo** en JSON y exportación de la cobranza a CSV para Excel.

Funciona sin internet: todo se guarda en el teléfono.

## Descargar

El APK más reciente siempre está en:

https://github.com/beto69chico-debug/svolen-nexor/releases/latest/download/SvolenNexor.apk

## Cómo se compila

Cada push a `main` corre las pruebas de cobranza y avisos y compila el APK firmado con GitHub Actions (`.github/workflows/build.yml`). La firma fija (`app/signing/svolen.jks`) permite instalar actualizaciones encima sin perder datos.

Kotlin + Jetpack Compose · minSdk 26 · sin dependencias de red.
