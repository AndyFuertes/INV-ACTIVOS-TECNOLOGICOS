# GUÍA COMPLETA DE INSTALACIÓN — KERU SISTEMA
# DTI · Universidad Peruana Cayetano Heredia
# Servidor interno Windows con SQL Server Express
=======================================================

## ¿QUÉ NECESITAS TENER INSTALADO?

| Herramienta      | Versión mínima | Cómo verificar (cmd)         |
|------------------|----------------|------------------------------|
| Java JDK         | 21             | java -version                |
| Maven            | 3.8+           | mvn -version                 |
| SQL Server Express | 2019+        | (ver SSMS)                   |

Si te falta alguno, sigue los pasos de instalación abajo.

=======================================================
## PASO 1 — INSTALAR JAVA 21 (si no lo tienes)
=======================================================

1. Descarga desde: https://adoptium.net/
   → Elige: Temurin 21 (LTS) → Windows → x64 → .msi

2. Ejecuta el instalador. Marca la opción:
   ✅ "Set JAVA_HOME variable"
   ✅ "Add to PATH"

3. Verifica en cmd (abre nuevo cmd después de instalar):
   > java -version
   Debes ver: openjdk version "21.x.x"

=======================================================
## PASO 2 — INSTALAR MAVEN (si no lo tienes)
=======================================================

1. Descarga desde: https://maven.apache.org/download.cgi
   → apache-maven-3.9.x-bin.zip

2. Extrae en: C:\maven\

3. Agrega al PATH del sistema:
   Panel de Control → Sistema → Variables de entorno
   → En "Variables del sistema" → PATH → Editar
   → Agregar: C:\maven\bin

4. Verifica en cmd (nuevo cmd):
   > mvn -version
   Debes ver: Apache Maven 3.9.x

=======================================================
## PASO 3 — CONFIGURAR SQL SERVER EXPRESS
=======================================================

### 3A. Habilitar TCP/IP (necesario para JDBC)

1. Abre: SQL Server Configuration Manager
   (Busca en el menú Inicio)

2. Ve a: SQL Server Network Configuration
   → Protocols for SQLEXPRESS
   → TCP/IP → doble clic → Enabled: YES

3. En la pestaña "IP Addresses":
   → Busca "IPAll" al final
   → TCP Port: 1433
   → TCP Dynamic Ports: (dejarlo vacío si ya tienes 1433)

4. Reinicia el servicio:
   SQL Server Configuration Manager
   → SQL Server Services
   → SQL Server (SQLEXPRESS) → Restart

### 3B. Habilitar autenticación SQL Server (login "sa")

1. Abre SQL Server Management Studio (SSMS)
2. Clic derecho en el servidor → Properties → Security
   → Server authentication: SQL Server and Windows Authentication mode → OK

3. En Object Explorer → Security → Logins → sa
   → Doble clic → Status → Login: Enabled
   → General → poner una contraseña

4. Reinicia el servicio SQL Server (igual que 3A paso 4)

### 3C. Ejecutar el script de la base de datos

1. En SSMS → New Query
2. Abre el archivo: BD_INVENTARIO2026.sql
3. Ejecuta (F5)
4. Verifica: debe aparecer la BD "InventarioDTI_2026"
   con todas las tablas: activos_maestro, usuarios, etc.

### 3D. Habilitar el firewall (para acceso desde red)

Abre PowerShell como administrador:

  netsh advfirewall firewall add rule name="SQL Server 1433" ^
    protocol=TCP dir=in localport=1433 action=allow

  netsh advfirewall firewall add rule name="KERU Sistema 8080" ^
    protocol=TCP dir=in localport=8080 action=allow

=======================================================
## PASO 4 — CONFIGURAR EL PROYECTO KERU
=======================================================

1. Descomprime keru-backend.zip en:
   C:\keru-sistema\keru-backend\

2. Edita el archivo de configuración:
   C:\keru-sistema\keru-backend\src\main\resources\application.properties

   Cambia estas líneas:
   ─────────────────────────────────────────────────────
   spring.datasource.password=TU_PASSWORD_AQUI
   ─────────────────────────────────────────────────────
   Reemplaza TU_PASSWORD_AQUI con la contraseña del usuario sa.

   Si tu IP de red es por ejemplo 192.168.1.50, también agrega:
   ─────────────────────────────────────────────────────
   keru.cors.allowed-origins=http://localhost:8080,http://192.168.1.50:8080
   ─────────────────────────────────────────────────────

3. Crea las carpetas de almacenamiento de PDFs:
   (puedes hacerlo manualmente o con cmd)

   > mkdir C:\keru-storage\actas-pdf
   > mkdir C:\keru-storage\actas-firmadas

=======================================================
## PASO 5 — COMPILAR EL PROYECTO
=======================================================

Abre cmd como administrador y ejecuta:

  > cd C:\keru-sistema\keru-backend
  > mvn clean package -DskipTests

Espera que descargue las dependencias (primera vez ~3-5 min).
Al final debes ver: BUILD SUCCESS

El archivo ejecutable quedará en:
  C:\keru-sistema\keru-backend\target\keru-1.0.0.jar

=======================================================
## PASO 6 — EJECUTAR EL SISTEMA
=======================================================

### Opción A: Ejecución manual (para pruebas)

  > cd C:\keru-sistema\keru-backend
  > java -jar target\keru-1.0.0.jar

Verás en pantalla:
  ========================================
    KERU Sistema - DTI UPCH
    Servidor corriendo en puerto 8080
    http://localhost:8080/api
  ========================================

### Opción B: Instalar como servicio de Windows (producción)
Para que arranque automáticamente con el servidor:

Descarga NSSM (Non-Sucking Service Manager):
https://nssm.cc/download → nssm.exe

Luego en cmd como administrador:

  > nssm install KeruSistema
    Path:           C:\Program Files\Eclipse Adoptium\jdk-21...\bin\java.exe
    Arguments:      -jar C:\keru-sistema\keru-backend\target\keru-1.0.0.jar
    Startup dir:    C:\keru-sistema\keru-backend

  > nssm start KeruSistema

Para detenerlo:
  > nssm stop KeruSistema

Para ver el log:
  > nssm edit KeruSistema  (pestaña I/O, agrega Log file)

=======================================================
## PASO 7 — ACCEDER AL SISTEMA
=======================================================

Desde el mismo servidor:
  http://localhost:8080

Desde cualquier PC de la red universitaria
(reemplaza 192.168.1.50 con la IP real del servidor):
  http://192.168.1.50:8080

Para saber la IP del servidor:
  > ipconfig
  Busca "IPv4 Address" de tu adaptador de red.

=======================================================
## PASO 8 — IMPORTAR LOS DATOS EXCEL
=======================================================

Una vez en el navegador:

1. Ve a "📥 Importar Excel" en el menú izquierdo
2. Pestaña "Planilla" → sube PLANILLA.xlsx
   → Llena la tabla: usuarios (3,835 registros)

3. Pestaña "Equipos Cómputo" → sube EQUIPO-COMPUTO.xlsx
   → Llena: activos_maestro + especificaciones_computo + ubicaciones
   → ~5,647 equipos

4. Pestaña "Periféricos" → sube PERIFERICOS.xlsx
   → Llena: activos_maestro (monitores, impresoras, proyectores...)
   → ~2,543 periféricos

La importación puede tomar 2-5 minutos por archivo.
El log muestra el progreso en tiempo real.

=======================================================
## FLUJO COMPLETO: GENERAR Y FIRMAR UN ACTA
=======================================================

1. En el menú → "📝 Nueva Entrega" (o Devolución/Préstamo)
2. Escribe el DNI del usuario → se rellena automáticamente
3. Escribe la serie del equipo → se rellena automáticamente
4. Clic "👁️ Vista Previa" → revisas el acta
5. Clic "💾 Guardar en BD"
   → El sistema guarda el movimiento en SQL Server
   → Genera el PDF automáticamente en: C:\keru-storage\actas-pdf\

6. En la lista de Actas → clic en el acta → "⬇️ Descargar PDF"
7. Imprime el PDF, obtén las firmas físicas, escanea el documento
8. En el acta → "📎 Subir PDF Firmado"
   → El PDF escaneado se guarda en: C:\keru-storage\actas-firmadas\
   → Queda vinculado al registro en la BD (campo ruta_pdf_firmado)

=======================================================
## SOLUCIÓN DE PROBLEMAS COMUNES
=======================================================

❌ "Connection refused" al iniciar
→ Verifica: TCP/IP habilitado en SQL Server Configuration Manager
→ Puerto 1433 activo y firewall abierto

❌ "Login failed for user 'sa'"
→ Verifica contraseña en application.properties
→ Verifica que el modo mixto esté habilitado (paso 3B)

❌ "SchemaValidationException" - tabla no encontrada
→ Ejecuta BD_INVENTARIO2026.sql completo en SSMS

❌ BUILD FAILURE en Maven
→ Verifica conexión a internet (descarga dependencias)
→ Asegúrate de estar en la carpeta correcta (donde está pom.xml)

❌ Puerto 8080 en uso
→ Cambia en application.properties: server.port=8090
→ Accede en http://localhost:8090

❌ No se genera el PDF
→ Verifica que existan las carpetas:
   C:\keru-storage\actas-pdf\
   C:\keru-storage\actas-firmadas\
→ Verifica permisos de escritura en esas carpetas
