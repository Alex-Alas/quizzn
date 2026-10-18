# Quizz'n — Overlay sobre juegos (2026-10-18)

Estado: **Windows y Android publicados** — [Release v0.2.0](https://github.com/Alex-Alas/quizzn/releases/tag/v0.2.0) ([windows/Program.cs](windows/Program.cs), [android/…/Quizzn.kt](android/src/main/kotlin/io/github/alexalas/quizzn/Quizzn.kt)).

## Decisiones (elegidas por Alex)

| # | Decisión | Por qué |
|---|----------|---------|
| 1 | Windows primero, Android después | Pedido de Alex |
| 2 | App de Windows en **C# (WinForms, .NET Framework 4.8) + WebView2** | Zip de ~260 KB; .NET 4.8 y WebView2 ya vienen en Windows 10/11, así que tus amigos no instalan nada. Electron pesaba ~90 MB y Tauri pedía instalar Rust |
| 3 | La app carga la **misma web** (GitHub Pages) y solo pone la ventana encima | Un solo código de quizzes para web, Windows y luego Android |
| 4 | Pausar el juego = **tomar el foco + mandar Esc** | Esc abre el menú de pausa en la mayoría de juegos; no se inyecta nada en el juego |
| 5 | Regla: **acertar N** (cada fallo agrega otra pregunta) | Obliga a estudiar de verdad |
| 6 | **Salto ganado con aciertos**: cada K aciertos acumulados dan un "⏭ Saltar" | Interpretación de "saltar con cooldown de aciertos"; si era otra cosa, se cambia en `overlayBreak`/`play` |

Defaults (se cambian en la tarjeta "🎮 Pausas de estudio" de la app): cada 20 min, acertar 3, salto cada 10 aciertos, Esc activado. Atajo para una pausa ya: **Ctrl+Alt+Q**.

## Cómo funciona

- La app vive en la bandeja (junto al reloj). Al cerrar la ventana se esconde; para salir de verdad: clic derecho → **Salir** (salida de emergencia, sirve también en plena pausa).
- Cada N minutos: recuerda la ventana activa (el juego) → le manda Esc → muestra Quizz'n en pantalla completa, siempre encima y con el foco → al acertar N y tocar "🎮 Volver al juego" se esconde y le devuelve el foco al juego. El temporizador vuelve a contar desde ahí.
- Durante la pausa Alt+F4 no la cierra.
- Las preguntas salen al azar de **todos** los quizzes de la biblioteca de la app (sin encuestas, abiertas, dibujos ni diapositivas).
- Protocolo app ↔ web: la app llama `overlayBreak()` y `home()`; la web manda `cfg:minutos:esc`, `now` y `done` por `chrome.webview.postMessage`. Android usará el mismo `overlayBreak()`.

## Limitaciones conocidas

- **La biblioteca de la app es aparte de la del navegador.** Para pasar un quiz: 🔗 Compartir en el navegador → en la app ⇪ Importar → pegar el link.
- Necesita internet (carga la web publicada). Si la web no carga, la pausa no aparece: nunca tapa el juego con una ventana rota.
- Tras subir cambios a la web, la app puede tardar hasta ~10 min en verlos (caché de GitHub Pages) o hasta reiniciarla.
- Juegos que no se pausan con Esc siguen corriendo. Los que están en pantalla completa exclusiva se minimizan al perder el foco (eso también los pausa).
- Algunos anti-cheat (p. ej. Vanguard) pueden ignorar el Esc simulado; si molesta, se apaga en los ajustes. No se toca la memoria ni los gráficos del juego.
- El `.exe` no está firmado: Windows SmartScreen avisa la primera vez (Más información → Ejecutar de todas formas).

## Compilar y probar

```bash
cd windows && dotnet build -c Release
```

- Para repartir: zip con `Quizzn.exe`, `Quizzn.exe.config`, `Microsoft.Web.WebView2.Core.dll`, `Microsoft.Web.WebView2.WinForms.dll` y `WebView2Loader.dll` de `bin/Release/net48/`.
- Desarrollo contra la web local: `Quizzn.exe http://localhost:8765/`.
- Web: `index.html?test` cubre la regla de la pausa; `index.html?overlay` abre una pausa en el navegador.
- Probado (2026-10-18) con una ventana "juego falso" que cuenta los Esc: el atajo abre la pausa, el juego recibe Esc, la ventana queda encima de todo y en pantalla completa, Alt+F4 no la cierra, al terminar el foco vuelve al juego, "Probar ahora" regresa a la ventana normal, una segunda instancia solo muestra un aviso, y con la web vieja publicada el atajo no hace nada.

## Android (2026-10-18)

| # | Decisión | Por qué |
|---|----------|---------|
| 7 | App **Kotlin nativa sin dependencias** (Activity, Service, WebView del sistema) | APK de ~30 KB; se reparte por el Release de GitHub |
| 8 | La pausa es una **pantalla completa (BreakActivity)** que se abre encima del juego, no una ventana flotante | Con una Activity funcionan los desplegables `<select>`, el teclado y el botón Atrás; y el juego pasa a segundo plano, así que casi todos se pausan solos |
| 9 | Se usa el permiso **"Mostrar sobre otras apps"** | Es lo que deja abrir la pausa encima de otra app; en Android 15+ además se pone un punto invisible de 1 px justo antes de abrirla (regla nueva de Android) |
| 10 | Salir de la pausa con Inicio o Recientes sin terminar → **vuelve en 1 minuto** | En Android no se puede bloquear el botón Inicio; esto lo hace "obligatorio" sin trucos |
| 11 | Salida de emergencia: botón **"Detener"** en la notificación | Igual que "Salir" en la bandeja de Windows; se reactiva al abrir Quizz'n |

Cómo funciona: un servicio en primer plano (notificación "Pausas de estudio activas") cuenta los minutos; al cumplirse abre `BreakActivity` con la web en `?overlay`. Con la pantalla apagada o bloqueada espera y reintenta cada minuto. Atrás no cierra la pausa. Si la web no carga (sin internet o versión vieja), la pausa se cierra sola. La web reconoce Android por `window.QuizznHost` y oculta la opción de Esc y el atajo de teclado.

Limitaciones en Android:

- Instalar el APK pide permitir "apps desconocidas" (no está en Play Store). Al abrir la app pide "Mostrar sobre otras apps" y notificaciones.
- Dentro de la app no funcionan "Elegir archivo…" (imágenes e importar archivo) ni "⬇ Descargar archivo": para crear quizzes con imágenes, usa la web en Chrome y luego importa el link.
- El APK va firmado con la llave debug de la PC de Alex (`signingConfig` en `android/build.gradle.kts`): las actualizaciones tienen que compilarse en esa PC. Para Play Store hace falta una llave propia.
- Los juegos siguen corriendo debajo si no se pausan solos al pasar a segundo plano.

Compilar (en esta PC Java necesita una carpeta temporal corta para sus sockets; en Android Studio no hace falta):

```bash
cd android && JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=C:/Users/alexa_qd9haq2/.gradle/tmp" ./gradlew assembleRelease
```

El APK queda en `android/build/outputs/apk/release/quizzn-release.apk`.

Probado (2026-10-18) en un emulador con Android 16 y la web publicada:
- el permiso abre la pantalla correcta del sistema;
- servicio y notificación activos;
- la pausa sale sola a los 60 s y a los 122 s encima de otra app;
- Atrás no la cierra, y se responde con toques;
- "Volver al juego" regresa a la app de antes;
- salir con Inicio la trae de vuelta a los 63 s;
- "Detener" apaga el servicio y no salen más pausas;
- el APK de release (minificado) funciona con "Probar ahora";
- la barra de estado y el teclado ya no tapan el contenido.
