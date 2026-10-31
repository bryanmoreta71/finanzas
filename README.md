# Finanzas

App Android (Kotlin) para llevar egresos e ingresos, pensada para usarse desde un **widget en la pantalla de inicio**.

## Widget

```
 Viernes 31 de octubre                      ⚙
 Otra  Monto  Motivo          Ingresos   Pagar
 [ ]   $50    Juan            ~~$50~~     [ ]
 [ ]   $120   Luz             ~~$120~~    [ ]
 [ ]   $300   Renta           Faltan $270
 Egresos: $470                  Ingresos: $200
```

- Arriba: la fecha de hoy y la tuerquita ⚙, que abre la app.
- La lista de egresos pendientes va del monto **más pequeño al más alto**.
- Cada fila: monto, motivo o persona y la celda de **Ingresos**. Con tus ingresos disponibles se van
  tachando en verde, empezando por los más pequeños, los egresos que alcanzas a pagar. Si no alcanza,
  la celda dice cuánto falta.
- **Visto izquierdo**: el egreso se pagó con otra fuente. Sale de la lista y los ingresos no cambian.
- **Visto derecho** (solo en los tachados en verde): el egreso se paga con los ingresos. Sale de la lista
  y su monto se resta de los ingresos.
- Abajo: total de egresos pendientes y total de ingresos disponibles.

## App

- **Egresos**: la misma lista que el widget. Toca un egreso para editarlo o eliminarlo.
- **Ingresos**: total, lo usado para pagar egresos y lo disponible. Toca uno para editarlo o eliminarlo.
- **Pagados**: historial de lo que marcaste con un visto, con botón **Deshacer** por si tocaste uno sin querer.
- Botón **Añadir egreso / Añadir ingreso**.

## Compilar

Abre el proyecto en Android Studio, o desde la terminal (con el SDK de Android configurado):

```
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Después de instalarlo, mantén presionada la
pantalla de inicio → Widgets → **Finanzas / Egresos**.

Hecho con Jetpack Compose (app), Jetpack Glance (widget) y Room (base de datos local). Android 8.0+.
