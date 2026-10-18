# Quizz'n — MVP v0 (2026-10-18)

Estado: **construido y probado en navegador**. Código: [index.html](index.html) (un solo archivo, sin dependencias ni build).

## Decisiones (pendientes de confirmar por Alex)

| # | Decisión | Por qué |
|---|----------|---------|
| 1 | El MVP es el **quiz web compartible**; el overlay que pausa juegos queda para la fase 2 y reutiliza los mismos quizzes | Pediste "usable ya" y "fácil de compartir"; un overlay requiere app de escritorio por SO |
| 2 | Un solo `index.html` vanilla JS, datos en `localStorage` | Se comparte como link o como archivo; sin servidor ni cuentas |
| 3 | Compartir = el quiz viaja comprimido (deflate + base64url) dentro del `#hash` del link; alternativa: archivo `.quizzn.json` | Cero backend. Límite: con imágenes el link se vuelve largo (aviso a partir de ~6 KB) |
| 4 | Imágenes: se reducen a 640 px JPEG y se incrustan en el quiz | Un quiz es un solo archivo/link, sin hosting de imágenes |
| 5 | IA: botón "Copiar prompt para IA" + Importar JSON | Cubre la feature 1 de la idea sin atar a un proveedor; MCP/API directa va después |
| 6 | Publicación: repo público `Alex-Alas/quizzn` + GitHub Pages (`https://alex-alas.github.io/quizzn/`) | Elegido por Alex en la sesión anterior; falta ejecutarlo |

## Formatos de pregunta (14) vs. Wayground

| Wayground | Quizz'n |
|-----------|---------|
| Multiple Choice | Opción múltiple (V/F = atajo) |
| Checkbox | Casillas |
| Fill in the Blank | Completar (texto) |
| Poll | Encuesta |
| Open Ended | Respuesta abierta |
| Match | Relacionar (desplegables) |
| Reorder | Ordenar (botones ↑↓) |
| Drop Down | Completar con opciones |
| Drag and Drop | Arrastrar y soltar (tocar palabra → hueco) |
| Categorize | Categorizar (desplegables) |
| Hotspot | Zona en imagen |
| Labeling | Etiquetar imagen (puntos numerados + desplegables) |
| Draw | Dibujo |
| Slide | Texto / diapositiva |

Todos admiten imagen y explicación. **No** están (no básicos o fase 2): Math Response, Graphing, Audio/Video Response, tiempo por pregunta, modo en vivo multijugador, arrastre real con el dedo (hoy es tocar/desplegables).

## Probar

- `index.html?test` → el título de la pestaña dice `TEST OK` si pasan las comprobaciones de calificación, compartir (pack/unpack) e importación.
- Local: `python -m http.server 8765` (o `.claude/launch.json`) y abrir `http://localhost:8765`.
- Verificado (375×812): los 14 formatos del quiz de ejemplo jugados desde un link compartido, editor de "Etiquetar imagen", sin errores de consola.

## Pendiente

- Publicar en GitHub Pages para que el link funcione con otras personas.
- Probar con tu hermano/compañeros y anotar qué formato falta o estorba.
