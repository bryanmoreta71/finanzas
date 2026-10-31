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

## Personalizar el widget

En la app, botón **Widget** arriba a la derecha. Con vista previa en vivo:

- Diseño **Terminal (CMD)**: el widget se ve como una ventana de consola, con barra de título,
  prompt `C:\finanzas>`, comandos, tabla con guiones y vistos `[ ]`. Presets **CMD** (paleta
  clásica de Windows) y **Hacker** (verde fosforescente con cian y magenta).
- Estilos rápidos: Clásico, CMD, Hacker, Noche, Transparente, Vidrio, Neón, Pastel, Papel, Océano y Rosa.
- Título propio, 11 tipos de letra, tamaño de letra, montos en negrita.
- Colores de fondo, letras, ingresos, egresos y filas (paleta o cualquier color), con transparencia
  del fondo y de las filas.
- Forma de las filas (simple, con líneas, rayas o tarjetas) y esquinas redondeadas.
- Mostrar u ocultar títulos de columnas, totales e ingresos por cobrar.

## App

- **Egresos**: la misma lista que el widget. Toca un egreso para editarlo o eliminarlo.
- **Ingresos**: por cobrar (con botón **Llegó**) y recibidos; total, lo usado y lo disponible.
- **Pagados**: historial de lo pagado, con **Deshacer** por si marcaste uno sin querer.
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
