# Handoff — Quizz'n (2026-10-18)

Para pegar en una sesión nueva de Claude Code (en tu máquina). Idioma: español, y trata a Alex de tú, tono casual. Alex quiere trazabilidad: presenta opciones antes de decisiones importantes y documenta lo acordado en `.md`.

## Qué es Quizz'n

Herramienta de estudio que aprovecha el momento de juego: a futuro, un overlay que pausa/interrumpe un videojuego y fuerza estudio rápido (quizzes, ejercicios), con IA que genera el material y proyecto open source con plugins. Es el **primer proyecto video-documentado** de Alex (marca personal en LinkedIn/IG/TikTok). Idea original: `quizzn/idea.md` (en la carpeta del proyecto).

## Alcance acordado para el MVP

El MVP es **primero un quiz web compartible**; el overlay sobre juegos es fase 2 y reutiliza los mismos quizzes. Alex pidió:

1. Fácil de compartir (hermano, compañeros de clase y de equipo).
2. Varios formatos de pregunta con imágenes, al menos los básicos de Wayground (antes Quizizz).

Alex pidió además modo `ponytail` (la solución más simple que funcione): sin dependencias, sin build, un solo archivo.

## Estado

**Hecho y probado:** `index.html`, un solo archivo (~29 KB), vanilla JS.

- 14 formatos: opción múltiple, casillas, completar texto, encuesta, respuesta abierta, relacionar, ordenar, completar con desplegables (`{{correcta|falsa}}`), arrastrar y soltar (banco de palabras), categorizar, zona en imagen (hotspot), etiquetar imagen, dibujo, diapositiva. Verdadero/Falso es atajo de opción múltiple. Todos admiten imagen (se reduce a 640 px JPEG y se incrusta) y explicación.
- Editor + jugador + resultados. Datos en `localStorage` (`quizzn.lib`).
- Compartir: el quiz viaja comprimido (deflate-raw + base64url) en el `#q=` del link, o como archivo `.quizzn.json`. Aviso si el link supera ~6 KB.
- IA: botón "Copiar prompt para IA" (`AI_PROMPT`) + Importar (JSON, link o archivo). `clean()` normaliza lo que devuelva la IA.
- Autocomprobación: abrir `index.html?test` → el título de la pestaña dice `TEST OK` o `TEST FAIL: …`.
- Probado con Playwright (390×844): se jugaron los 12 formatos del quiz de ejemplo (8/8 calificables), link compartido abierto en otra pestaña, importación de JSON tipo IA. Sin errores de consola.

**Pendiente / bloqueado:**

- **Publicación.** Alex eligió "repo `quizzn` público + GitHub Pages". La integración de la sesión en la nube no pudo crear el repo (403 en `POST /user/repos`). Mientras no esté publicada, el link de compartir solo funciona si la app se sirve desde una web; abierta como `file://` solo sirve el archivo `.json`. Desde tu máquina: `gh repo create Alex-Alas/quizzn --public`, subir `index.html` en la raíz y activar Pages (rama `main`, carpeta raíz).
- Dos decisiones siguen sin confirmar por Alex (ver `mvp.md`): web primero y overlay después, y el esquema de compartir por `#hash`.

## Siguiente paso sugerido

1. Crear el repo público y publicar en Pages (`https://alex-alas.github.io/quizzn/`); probar el link de compartir desde un teléfono real con las imágenes del quiz.
2. Que Alex lo use con su hermano y compañeros y anote qué formato falta o estorba. Candidatos ya identificados, sin hacer: arrastrar y soltar con el dedo (hoy ordenar usa ↑↓ y relacionar/categorizar usan desplegables), etiquetar imagen con varios puntos, tiempo por pregunta, audio/video, modo en vivo.
3. Cuando el MVP web esté validado, sesión de decisiones (una pregunta a la vez, con opciones y trade-offs) para el overlay: plataforma objetivo, cómo se pausa un juego de forma fiable por SO (juegos online/anti-cheat), disparador de interrupciones, licencia. Preguntas abiertas completas en `idea.md`.
4. Documentar cada decisión en `.md` y planear qué se graba en video desde el día 0.

## Archivos

- `index.html` — la app (código de arriba).
- `mvp.md` — decisiones y alcance del MVP.
- `idea.md` — idea original y preguntas abiertas (no está en esta carpeta).

Estructura interna de `index.html`: utilidades (`h`, `btn`, `norm`), compartir (`pack`/`unpack`), tabla de tipos `T` (cada tipo define `blank`, `edit`, `play`, `grade`, `key`), importación (`clean`, `AI_PROMPT`, `SAMPLE`), pantallas (`home`, `edit`, `play`, `share`, `importView`) y `selfTest`. Un tipo nuevo = una entrada en `T`. Ojo: `top` choca con `window.top`, por eso el encabezado se llama `hdr`.
