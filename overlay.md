# Quizz'n — Overlay sobre juegos (2026-10-18)

Estado: **Windows construido y probado** ([windows/Program.cs](windows/Program.cs)). Android: pendiente.

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

## Siguiente: Android

App nativa mínima (Kotlin) con permiso "Mostrar sobre otras apps" + servicio en primer plano con el temporizador + un WebView con la misma web encima de todo. La web necesita un adaptador para que `tell()` hable con Android (`window.QuizznHost`) además de `chrome.webview`. En Android no hay Esc: el juego queda tapado, pero sigue corriendo si no se pausa solo.
