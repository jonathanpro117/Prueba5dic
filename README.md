# DarkBOT

How to run:
- Clone the repository
- Get the latest full-release on darkbot's discord https://discord.gg/uXHnZJ9
- Unzip the release in a known folder outside of the project
- Run `mvn clean install`
- Add Run/Debug configuration:
  - Main class: com.github.manolo8.darkbot.Bot
  - Working directory: wherever you unzipped the release

Distribution & support for the bot can be found over at discord: https://discord.gg/bEFgxCy

Everyone is allowed to make, publish & redistribute videos & content about the software.

Bugpoint is not affiliated in any way with this software. They claim themselves as the owners in DMCA claims, which are all invalid.

## Using a custom verifier

If you need to point DarkBOT to a different `verifier.jar` (for example, one built for another Discord server), set one of the following before launching the bot:

- JVM property: `-Ddarkbot.verifier.path=/full/path/to/verifier.jar`
- Environment variable: `DARKBOT_VERIFIER_PATH=/full/path/to/verifier.jar`

If neither is set, DarkBOT will load `lib/verifier.jar` from the working directory as before.

### Explicación rápida (sin saber programar)

- **¿Qué es el `verifier.jar`?** Es un archivo que comprueba quién puede usar el bot (por ejemplo, si pertenece a tu servidor de Discord).
- **¿Qué cambia ahora?** Puedes decirle al bot dónde está tu propio `verifier.jar` sin tocar el código.
- **Cómo usarlo:**
  1. Coloca tu `verifier.jar` en alguna carpeta de tu PC.
  2. Al iniciar el bot, indica la ruta completa de ese archivo de una de estas dos formas:
     - Añadiendo en los parámetros de inicio: `-Ddarkbot.verifier.path=C:/ruta/completa/verifier.jar` (o la ruta en Linux/Mac).
     - O definiendo la variable de entorno `DARKBOT_VERIFIER_PATH` con esa ruta antes de abrir el bot.
  3. Si no haces nada de lo anterior, el bot seguirá usando el `lib/verifier.jar` que viene por defecto.
