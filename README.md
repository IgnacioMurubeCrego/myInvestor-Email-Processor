# myInvestor Email Processor

Aplicación de escritorio para Windows que lee tus correos de confirmación de
compra/venta de acciones de **myInvestor** (exportados desde Gmail en formato
`.mbox`) y genera un Excel con las fechas e importes que necesitas para tu
declaración de la renta.

- Procesa el `.mbox` completamente en local: no envía ningún dato a internet.
- Genera un Excel con dos hojas, **Compras** y **Ventas**, con fecha, valor, ISIN,
  cantidad, importe neto (divisa original) e importe neto en euros ya descontando
  comisiones, gastos y retenciones.
- Una hoja adicional **"No procesados"** lista los correos con detalle de operación
  que no se hayan podido interpretar, para revisión manual.

## Para usuarios: instalar y usar la aplicación

No hace falta tener Java ni herramientas de desarrollo instaladas. Sigue la
**[Guía de uso](GUIA_USO.md)**, que explica paso a paso:

1. Cómo exportar tus correos de myInvestor desde Gmail (Google Takeout).
2. Cómo instalar la aplicación descargando el `.zip` de la
   [última Release](../../releases).
3. Cómo procesarlos y qué contiene el Excel generado.

## Para desarrolladores

Requisitos: JDK 21 y Maven.

```powershell
mvn package                 # compila y genera target/myInvestorStockProcessor.jar
.\update-app.ps1            # recompila, empaqueta con jpackage y reinstala localmente
.\build-release.ps1         # genera el .zip de distribución en release/
```

`build-release.ps1` admite un parámetro de versión, por ejemplo:

```powershell
.\build-release.ps1 -Version 1.1.0
```

El `.zip` resultante en `release/` está listo para subirse como asset de una
[Release de GitHub](https://docs.github.com/es/repositories/releasing-projects-on-github).

### Estructura del proyecto

- `src/main/java/org/example/myinvestor/mbox` — lectura y separación del fichero `.mbox`.
- `src/main/java/org/example/myinvestor/mail` — extracción de asunto/fecha/cuerpo de cada correo.
- `src/main/java/org/example/myinvestor/parser` — reconocimiento de los correos de
  operación y extracción de sus campos.
- `src/main/java/org/example/myinvestor/export` — generación del Excel con Apache POI.
- `src/main/java/org/example/myinvestor/ui` — interfaz gráfica (Swing).
- `installer/` — scripts que se distribuyen dentro del `.zip` de la Release para
  instalar/desinstalar la app en el equipo del usuario final.

## Aviso

Esta herramienta no sustituye el asesoramiento fiscal profesional. Organiza los datos
de tus propios correos para facilitar tu declaración, pero revisa siempre los importes
contra tus extractos oficiales de myInvestor.

## Licencia

Distribuido bajo la licencia [MIT](LICENSE).
