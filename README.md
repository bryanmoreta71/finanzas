# Finanzas

App Android (Kotlin) para llevar egresos e ingresos, pensada para usarse desde un **widget en la pantalla de inicio**.

## Widget

```
 Viernes 31 de octubre                      ⚙
 Monto  Motivo          Ingresos   Pagar
 $20    Netflix         ~~$20~~     [ ]
 $60    Agua            ~~$60~~     [ ]
 $200   Renta           Alcanza     [ ]
 $300   Tarjeta         Faltan $50
 Por cobrar
 $400   Uber            lun 2 nov  [Llegó]
 Egresos: $580                  Ingresos: $250
```

- Arriba: la fecha de hoy (y un título opcional) y la tuerquita ⚙, que abre la app.
- Los egresos pendientes van del monto **más pequeño al más alto**.
- **Tachado en verde = sugerencia**: con tus ingresos disponibles, la app tacha los egresos que
  alcanzas a pagar empezando por los más pequeños.
- **Visto de "Pagar"**: paga el egreso con tus ingresos y se resta de ellos. Puedes marcar cualquier
  egreso que te alcance, aunque no esté sugerido ("Alcanza"). Solo si el egreso es mayor que tus
  ingresos no se puede marcar, y dice cuánto falta.
- **Por cobrar**: ingresos que todavía no llegan (ej. Uber, que paga los lunes) con la fecha en que
  llegan. No cuentan en los ingresos hasta que tocas **Llegó**.
- Abajo: total de egresos pendientes y de ingresos disponibles.

## Widget "Gasto rápido"

Un botón para la pantalla de inicio que muestra cuánto tienes en la mano (*Tienes $250*). Al tocarlo
se abre una ventanita con el teclado listo: escribes en qué gastaste (o tocas uno de tus gastos
recientes), el valor, y **Guardar gasto**. El gasto se resta al instante de tus ingresos. Toma el
mismo diseño que elegiste para el widget principal (normal, CMD, Android Studio, VS Code…).

## Personalizar el widget

En la app, botón **Widget** arriba a la derecha. Ves los cambios en la vista previa y, cuando te
gusten, tocas **Guardar** para que el widget de tu pantalla de inicio cambie. Si sales sin guardar,
la app te pregunta si quieres guardar o descartar los cambios. Puedes cambiar:

- Diseño **Terminal (CMD)**: el widget se ve como una ventana de consola, con barra de título,
  prompt `C:\finanzas>`, comandos, tabla con guiones y vistos `[ ]`. Presets **CMD** (paleta
  clásica de Windows) y **Hacker** (verde fosforescente con cian y magenta).
- Diseños **Android Studio** y **VS Code** como en una Mac: ventana con el semáforo rojo/amarillo/verde,
  pestaña del archivo, números de línea, los egresos escritos como código (Kotlin o TypeScript) con
  los colores de cada editor, el cursor en la última línea y los totales en la barra de estado.
- Estilos rápidos: Clásico, CMD, Hacker, Android Studio, VS Code, Noche, Transparente, Vidrio, Neón, Pastel, Papel, Océano y Rosa.
- Título propio, 11 tipos de letra, tamaño de letra, montos en negrita.
- Colores de fondo, letras, ingresos, egresos y filas (paleta o cualquier color), con transparencia
  del fondo y de las filas.
- Forma de las filas (simple, con líneas, rayas o tarjetas) y esquinas redondeadas.
- Mostrar u ocultar títulos de columnas, totales e ingresos por cobrar.

## App

- **Egresos**: la misma lista que el widget. Toca un egreso para editarlo o eliminarlo.
- **Ingresos**: por cobrar (con botón **Llegó**) y recibidos; total, lo usado y lo disponible.
- **Gastos**: en qué se te va la plata. Total de hoy, la semana, el mes o todo; "¿En qué gastas
  más?" con tus gastos agrupados por motivo; y la lista día por día. Ahí puedes **Borrar** un gasto
  o **Deshacer** un egreso pagado. El botón **Anotar gasto** abre la misma ventanita del widget.
- Botón **Añadir egreso / Añadir ingreso**. Al añadir un ingreso puedes marcarlo como
  *por cobrar* y elegir el día ("El lunes", "Mañana" u otra fecha).

## Compilar

Abre el proyecto en Android Studio, o desde la terminal (con el SDK de Android configurado):

```
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Después de instalarlo, mantén presionada la
pantalla de inicio → Widgets → **Finanzas / Egresos**.

Para ver los estilos del widget como imágenes PNG sin teléfono:

```
./gradlew testDebugUnitTest -Pwidget.render.dir=/ruta/de/salida
```

Hecho con Jetpack Compose (app), Jetpack Glance (widget) y Room (base de datos local). Android 8.0+.
