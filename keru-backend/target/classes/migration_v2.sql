-- ============================================================
-- KERU Sistema v2 - Migración de base de datos
-- Ejecutar en SSMS sobre InventarioDTI_2026
-- ============================================================

-- 1. Agregar columna PDF al activo
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('activos_maestro') AND name = 'ruta_pdf_equipo'
)
BEGIN
    ALTER TABLE activos_maestro ADD ruta_pdf_equipo NVARCHAR(MAX) NULL;
    PRINT 'Columna ruta_pdf_equipo agregada a activos_maestro';
END

-- 2. Inicializar correlativos de actas si no existen
IF NOT EXISTS (SELECT 1 FROM correlativos_actas WHERE tipo_acta = 'ASIGNACION')
    INSERT INTO correlativos_actas (tipo_acta, ultimo_numero, ano) VALUES ('ASIGNACION', 0, 2026);

IF NOT EXISTS (SELECT 1 FROM correlativos_actas WHERE tipo_acta = 'DEVOLUCION')
    INSERT INTO correlativos_actas (tipo_acta, ultimo_numero, ano) VALUES ('DEVOLUCION', 0, 2026);

IF NOT EXISTS (SELECT 1 FROM correlativos_actas WHERE tipo_acta = 'PRESTAMO')
    INSERT INTO correlativos_actas (tipo_acta, ultimo_numero, ano) VALUES ('PRESTAMO', 0, 2026);

IF NOT EXISTS (SELECT 1 FROM correlativos_actas WHERE tipo_acta = 'MOVILIZACION')
    INSERT INTO correlativos_actas (tipo_acta, ultimo_numero, ano) VALUES ('MOVILIZACION', 0, 2026);

PRINT 'Migración v2 completada';

-- ── v3 additions ──────────────────────────────────────────────────────────
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('activos_maestro') AND name = 'modelo_comercial'
)
BEGIN
    ALTER TABLE activos_maestro ADD modelo_comercial NVARCHAR(150) NULL;
    PRINT 'Columna modelo_comercial agregada';
END

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('activos_maestro') AND name = 'es_nuevo'
)
BEGIN
    ALTER TABLE activos_maestro ADD es_nuevo BIT DEFAULT 0 NULL;
    PRINT 'Columna es_nuevo agregada';
END

-- ── v4: serie_equipo en movimientos_historial ─────────────────────────────
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('movimientos_historial') AND name = 'serie_equipo'
)
BEGIN
    ALTER TABLE movimientos_historial ADD serie_equipo NVARCHAR(100) NULL;
    PRINT 'Columna serie_equipo agregada a movimientos_historial';
END

-- ── GPU en especificaciones_computo ──────────────────────────────────────────
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('especificaciones_computo') AND name = 'gpu'
)
BEGIN
    ALTER TABLE especificaciones_computo ADD gpu NVARCHAR(150) NULL;
    PRINT 'Columna gpu agregada a especificaciones_computo';
END

-- ── jefe_inmediato en usuarios (si no existe) ────────────────────────────────
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('usuarios') AND name = 'jefe_inmediato'
)
BEGIN
    ALTER TABLE usuarios ADD jefe_inmediato NVARCHAR(255) NULL;
    PRINT 'Columna jefe_inmediato agregada a usuarios';
END
