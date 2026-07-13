# Guía de uso — myInvestor Email Processor

Esta aplicación lee el correo que myInvestor te envía cada vez que compras o vendes
acciones, y genera un Excel con las fechas e importes (en la divisa original y en
euros) que necesitas para tu declaración de la renta. Todo el procesado ocurre en tu
propio ordenador: la aplicación no envía ningún dato a internet.

Hay dos pasos: **1)** descargar tus correos de Gmail y **2)** procesarlos con la app.

---

## 1. Descargar tus correos de myInvestor desde Gmail

### 1.1 Comprueba cómo Gmail agrupa esos correos

Gmail suele etiquetar automáticamente los correos de myInvestor bajo una etiqueta como
**"Aplicaciones y Servicios/myInvestor"**. Para comprobarlo:

1. Abre Gmail y busca `from:myinvestor.es` en la barra de búsqueda.
2. Abre uno de los correos de confirmación de compra/venta.
3. Junto al asunto, en la parte superior, verás una o varias etiquetas de color. Si ves
   una llamada "myInvestor" (normalmente dentro de "Aplicaciones y Servicios"), esa es
   la que vamos a exportar.

Si no encuentras ninguna etiqueta así, no pasa nada: puedes seleccionar manualmente
esos correos en Gmail (con el buscador `from:myinvestor.es`), aplicarles una etiqueta
nueva (por ejemplo "myInvestor") desde el menú de etiquetas, y usar esa etiqueta en el
paso siguiente.

### 1.2 Exporta solo esa etiqueta con Google Takeout

1. Ve a [https://takeout.google.com](https://takeout.google.com) (con la misma cuenta
   de Gmail).
2. Pulsa **"Anular selección"** para desmarcar todos los productos.
3. Busca **"Correo"** en la lista y márcalo.
4. Haz clic en el desplegable que aparece debajo, donde pone algo como "Se incluirán
   todos los datos de Correo". Ahí puedes elegir **etiquetas concretas** en lugar de
   todo el correo: desmarca todas y deja marcada solo la etiqueta de myInvestor que
   identificaste en el paso 1.1 (p. ej. "Aplicaciones y Servicios/myInvestor").
5. Baja hasta el final y pulsa **"Siguiente paso"**.
6. Elige el método de entrega (lo más sencillo es "Enviar enlace de descarga por
   correo electrónico"), frecuencia **"Exportar una vez"**, y tipo de archivo
   **.zip**.
7. Pulsa **"Crear exportación"**. Google tardará entre unos minutos y un par de horas
   en prepararla; te avisará por correo cuando esté lista.

### 1.3 Descomprime el archivo descargado

Descarga el `.zip` que te envía Google Takeout y descomprímelo. Dentro encontrarás una
carpeta con esta estructura:

```
Takeout/
  Correo/
    Aplicaciones y Servicios-myInvestor.mbox
```

Ese archivo `.mbox` es el que vas a abrir con la aplicación. Puedes dejarlo donde está.

---

## 2. Instalar la aplicación

1. Descarga el `.zip` de la última versión desde la sección
   [Releases](../../releases) de este repositorio.
2. Descomprímelo por completo en cualquier carpeta (por ejemplo, en el Escritorio).
   Es importante descomprimir el `.zip` entero, no solo abrirlo: dentro tiene que
   quedar visible la carpeta `app`.
3. Haz doble clic en **`Instalar.bat`**.
   - Es posible que Windows muestre un aviso azul de **"Windows protegió su PC"**
     (SmartScreen), porque la aplicación no está firmada digitalmente por una empresa
     conocida. Es un aviso normal para aplicaciones pequeñas o de código abierto: haz
     clic en **"Más información"** y luego en **"Ejecutar de todas formas"**.
4. Al terminar, tendrás un icono de **"myInvestor Email Processor"** en el Escritorio y
   en el Menú Inicio. Ya puedes borrar la carpeta descomprimida si quieres; la app ha
   quedado instalada en tu carpeta de programas personal.

Para desinstalarla más adelante, ejecuta `Desinstalar.bat` (está en esa misma carpeta
descomprimida), o simplemente borra la carpeta de instalación y los accesos directos.

---

## 3. Usar la aplicación

1. Abre **myInvestor Email Processor** desde su icono.
2. Pulsa **"Examinar..."** junto a "Archivo .mbox" y selecciona el archivo `.mbox` que
   descomprimiste en el paso 1.3.
3. Pulsa **"Guardar como..."** junto a "Excel de salida" y elige dónde quieres guardar
   el Excel resultante.
4. Pulsa **"Procesar"**. En el cuadro de registro verás cuántos correos se han
   encontrado y cuántas operaciones se han reconocido.
5. Al terminar, abre el Excel generado.

### Qué contiene el Excel

- **Hoja "Compras"**: una fila por cada compra, con fecha, valor, ISIN, cantidad,
  importe neto en la divisa original y el importe neto ya convertido a euros
  (descontando comisiones, gastos y retenciones).
- **Hoja "Ventas"**: lo mismo, para las ventas.
- **Hoja "No procesados"**: correos que contenían un detalle de operación pero que la
  aplicación no ha podido interpretar del todo (formato distinto al esperado). Conviene
  revisarla para no perder ninguna operación real.

---

## Preguntas frecuentes

**¿La aplicación envía mis datos a algún sitio?**
No. Todo el procesado del `.mbox` y la generación del Excel ocurre localmente en tu
ordenador. La aplicación no tiene conexión a internet.

**¿Sirve para declarar la renta directamente?**
No sustituye el asesoramiento fiscal. Solo organiza los datos de tus correos de
myInvestor (fechas e importes de compra/venta) para que te resulte más fácil rellenar
tu declaración. Revisa siempre los importes contra tus propios extractos.

**Faltan operaciones o hay correos en "No procesados".**
Puede deberse a que ese correo tenga un formato ligeramente distinto (por ejemplo,
fondos de inversión en lugar de acciones). Revisa la hoja "No procesados": ahí se
indica el motivo por el que no se pudo interpretar cada correo.
