# KERU Sistema — Backend Java Spring Boot
## DTI · Universidad Peruana Cayetano Heredia

---

## 🗂 Estructura del proyecto

```
keru-backend/
├── pom.xml
└── src/main/
    ├── java/com/dti/keru/
    │   ├── KeruApplication.java
    │   ├── config/
    │   │   └── CorsConfig.java
    │   ├── entity/
    │   │   ├── Ubicacion.java
    │   │   ├── Usuario.java
    │   │   ├── ActivoMaestro.java
    │   │   ├── EspecificacionComputo.java
    │   │   ├── MovimientoHistorial.java
    │   │   └── CorrelativoActa.java
    │   ├── repository/   (6 interfaces JPA)
    │   ├── service/
    │   │   ├── ExcelImportService.java  ← LEE EXCEL Y DISTRIBUYE EN BD
    │   │   ├── ActivoService.java
    │   │   ├── DashboardService.java
    │   │   └── ActaService.java
    │   └── controller/
    │       ├── ImportController.java    ← POST /api/import/*
    │       ├── ActivoController.java    ← GET  /api/activos/*
    │       ├── UsuarioController.java   ← GET  /api/usuarios
    │       ├── DashboardController.java ← GET  /api/dashboard/stats
    │       └── ActaController.java      ← GET/POST /api/actas
    └── resources/
        ├── application.properties       ← CONFIGURAR AQUÍ
        └── static/
            └── index.html               ← Frontend (acceso: http://servidor:8080)
```

---

## ⚙️ REQUISITOS

- Java 21 (ya instalado en el servidor)
- Maven 3.8+
- SQL Server Express con la BD `InventarioDTI_2026` ya creada (tu script .sql ejecutado)

---

## 🚀 PASOS PARA INSTALAR

### 1. Configurar la conexión a SQL Server

Edita `src/main/resources/application.properties`:

```properties
# Si tu instancia se llama diferente:
spring.datasource.url=jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=InventarioDTI_2026;encrypt=false;trustServerCertificate=true

# Cambia el usuario y contraseña:
spring.datasource.username=sa
spring.datasource.password=TU_PASSWORD_AQUI
```

> Si usas autenticación de Windows en vez de SQL Server:
> Agrega `;integratedSecurity=true` al final de la URL y quita username/password.

### 2. Compilar y ejecutar

```bash
# Compilar
mvn clean package -DskipTests

# Ejecutar
java -jar target/keru-1.0.0.jar
```

O directamente con Maven:
```bash
mvn spring-boot:run
```

### 3. Verificar que levantó

Abre en el navegador:
```
http://localhost:8080
```
Deberías ver el sistema KERU. Si el punto verde "API conectada" aparece, todo está funcionando.

---

## 📥 IMPORTAR DATOS EXCEL

Una vez levantado el sistema, ve a **"📥 Importar Excel"** en el menú:

| Archivo | Endpoint | Tablas que llena |
|---|---|---|
| `PLANILLA.xlsx` | `POST /api/import/planilla` | `usuarios` |
| `EQUIPO-COMPUTO.xlsx` | `POST /api/import/computo` | `activos_maestro` + `especificaciones_computo` + `ubicaciones` |
| `PERIFERICOS.xlsx` | `POST /api/import/perifericos` | `activos_maestro` + `ubicaciones` |

El proceso es **incremental**: si ya existe un registro (misma serie o DNI), lo actualiza. Si es nuevo, lo inserta.

---

## 🌐 ENDPOINTS DISPONIBLES

```
GET  /api/dashboard/stats
GET  /api/activos/laptops?estado=&gestion=&campus=&search=&page=0&size=50
GET  /api/activos/desktops
GET  /api/activos/aio
GET  /api/activos/otros-computo
GET  /api/activos/monitores
GET  /api/activos/impresoras
GET  /api/activos/proyectores
GET  /api/activos/otros-perifericos
GET  /api/activos/serie/{serie}
GET  /api/activos/campuses
GET  /api/usuarios?search=&departamento=&page=0&size=50
GET  /api/usuarios/{dni}
GET  /api/usuarios/departamentos
GET  /api/actas?page=0&size=20
POST /api/actas
GET  /api/actas/siguiente-nro?tipo=ASIGNACION
POST /api/import/planilla      (multipart/form-data, campo: file)
POST /api/import/computo
POST /api/import/perifericos
```

---

## 🔧 ACCESO DESDE OTROS EQUIPOS DE LA RED

Si el servidor tiene IP `192.168.1.50`, los demás equipos acceden en:
```
http://192.168.1.50:8080
```

Para habilitar CORS desde esa IP, agrega en `application.properties`:
```properties
keru.cors.allowed-origins=http://192.168.1.50:8080,http://localhost:8080
```

---

## ❓ PROBLEMAS COMUNES

**Error: "Cannot open database InventarioDTI_2026"**
→ Verifica que ejecutaste el script `BD_INVENTARIO2026.sql` en SQL Server Management Studio.

**Error: "Login failed for user 'sa'"**
→ Verifica usuario/contraseña en `application.properties`. Activa login SQL en SSMS > Propiedades del servidor > Seguridad.

**Error: "The TCP/IP connection to... failed"**
→ Activa TCP/IP en SQL Server Configuration Manager > SQL Server Network Configuration > Protocols for SQLEXPRESS > TCP/IP: Enabled.

**Tabla no encontrada (SchemaValidationException)**
→ Ejecuta primero el script SQL completo. El backend usa `ddl-auto=validate` (no crea tablas, solo las usa).
