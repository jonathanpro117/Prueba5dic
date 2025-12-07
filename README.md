# DarkBOT

How to run:
- Clone the repository
- Get the latest full-release on darkbot's discord https://discord.gg/uXHnZJ9
- Unzip the release in a known folder outside of the project
- Run `mvn clean install`
- Add Run/Debug configuration:
  - Main class: com.github.manolo8.darkbot.Bot
  - Working directory: wherever you unzipped the release

## Cómo obtener el ejecutable `DarkBot.jar`

Si quieres el JAR ejecutable generado desde el código fuente (en vez de descargarlo desde Discord), sigue estos pasos:

1. Instala JDK 11 o superior.
2. En una terminal, sitúate en la carpeta del proyecto y ejecuta:
   ```bash
   ./gradlew shadowJar proguard
   ```
   (En Windows usa `gradlew.bat shadowJar proguard`.)
3. Al terminar, encontrarás el archivo ejecutable en `build/DarkBot.jar`.
   - `shadowJar` empaqueta todas las dependencias en un único JAR.
   - `proguard` aplica la configuración del proyecto y produce el `DarkBot.jar` final en la carpeta `build`.

Con ese archivo (`build/DarkBot.jar`) ya puedes ejecutar el bot con tu JDK/Java Runtime habitual.

Distribution & support for the bot can be found over at discord: https://discord.gg/bEFgxCy

Everyone is allowed to make, publish & redistribute videos & content about the software.

Bugpoint is not affiliated in any way with this software. They claim themselves as the owners in DMCA claims, which are all invalid.

## Using a custom verifier

If you need to point DarkBOT to a different `verifier.jar` (for example, one built for another Discord server), set one of the following before launching the bot:

- JVM property: `-Ddarkbot.verifier.path=/full/path/to/verifier.jar`
- Environment variable: `DARKBOT_VERIFIER_PATH=/full/path/to/verifier.jar`

If neither is set, DarkBOT will load `lib/verifier.jar` from the working directory as before.

If you already have a `verifier.jar` in place but want to bypass it and force the built-in verifier (for example, to avoid plugin signature errors on unsigned builds), start DarkBOT with:

- JVM property: `-Ddarkbot.verifier.mode=builtin`
- Environment variable: `DARKBOT_VERIFIER_MODE=builtin`

If the bundled `verifier.jar` rejects your build (for example, an unsigned or locally built JAR), DarkBOT will now fall back automatically to the built-in verifier unless you explicitly provided a custom verifier path.

### Verificador integrado (sin Discord)

Si el archivo `verifier.jar` no está presente (o no defines ninguna ruta personalizada), el bot usará ahora un verificador integrado:

- No consulta servidores externos ni Discord.
- Genera un identificador único en `data/auth.id` la primera vez que se ejecuta y lo reutiliza.
- Permite la carga de plugins sin comprobar firmas externas.

Esto sirve como modo “offline” básico para pruebas locales o usos sin Discord.

### Explicación rápida (sin saber programar)

- **¿Qué es el `verifier.jar`?** Es un archivo que comprueba quién puede usar el bot (por ejemplo, si pertenece a tu servidor de Discord).
- **¿Qué cambia ahora?** Puedes decirle al bot dónde está tu propio `verifier.jar` sin tocar el código.
- **Cómo usarlo:**
  1. Coloca tu `verifier.jar` en alguna carpeta de tu PC.
  2. Al iniciar el bot, indica la ruta completa de ese archivo de una de estas dos formas:
     - Añadiendo en los parámetros de inicio: `-Ddarkbot.verifier.path=C:/ruta/completa/verifier.jar` (o la ruta en Linux/Mac).
     - O definiendo la variable de entorno `DARKBOT_VERIFIER_PATH` con esa ruta antes de abrir el bot.
  3. Si no haces nada de lo anterior, el bot seguirá usando el `lib/verifier.jar` que viene por defecto.

### Crear tu propio `verifier.jar`

Si quieres un verificador que no dependa del Discord oficial (o que use tu propio servidor), necesitas compilar un JAR con una clase concreta:

1. **Implementa la interfaz esperada:** crea una clase `eu.darkbot.verifier.AuthAPIImpl` que implemente `eu.darkbot.api.managers.AuthAPI`. Esa clase debe tener los métodos `setupAuth()`, `isAuthenticated()`, `isDonor()`, `requireDonor()`, `getAuthId()` y `checkPluginJarSignature(...)`.
2. **Ejemplo mínimo (sin Discord):**
   ```java
   package eu.darkbot.verifier;

   import eu.darkbot.api.managers.AuthAPI;
   import java.io.IOException;
   import java.util.jar.JarFile;

   public class AuthAPIImpl implements AuthAPI {
       @Override public void setupAuth() {} // Nada que preparar
       @Override public boolean isAuthenticated() { return true; }
       @Override public boolean isDonor() { return true; }
       @Override public boolean requireDonor() { return true; }
       @Override public String getAuthId() { return "offline-user"; }
       @Override public Boolean checkPluginJarSignature(JarFile jar) throws IOException { return null; }
   }
   ```
   Compila este archivo con Java 8+ y empaquétalo en un JAR llamado `verifier.jar` (el nombre importa para que coincida con la ruta habitual).
3. **Cómo compilar rápido sin Gradle/Maven:**
   - Guarda el archivo anterior en `src/eu/darkbot/verifier/AuthAPIImpl.java` dentro de una carpeta vacía.
   - Abre una terminal en esa carpeta y ejecuta:
     ```bash
     javac -cp "ruta/a/DarkBot.jar" -d out src/eu/darkbot/verifier/AuthAPIImpl.java
     jar cfe verifier.jar eu.darkbot.verifier.AuthAPIImpl -C out .
     ```
     (Sustituye `ruta/a/DarkBot.jar` por la ruta real si necesitas acceder a las interfaces en tiempo de compilación).
4. **Dile a DarkBOT que use tu JAR:** inicia el bot con `-Ddarkbot.verifier.path=/ruta/completa/verifier.jar` o con la variable `DARKBOT_VERIFIER_PATH` apuntando a ese archivo. Si no pones nada, seguirá cargando `lib/verifier.jar` como siempre.

Con esto tendrás un verificador básico (o el punto de partida para integrar tu propio sistema) sin depender del Discord oficial.
