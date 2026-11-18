package com.example.digitalbooks


import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "DigitalBooks.db"
        private const val DATABASE_VERSION = 3

        // Variables de usuarios
        private const val TABLE_USUARIOS = "usuarios"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NOMBRE = "nombre"
        private const val COLUMN_CORREO = "correo"
        private const val COLUMN_PASSWORD = "password"

        // Variables de libros favoritos
        private const val TABLE_LIBROS_FAVORITOS = "favoritos"
        private const val COLUMN_ID_FAVORITO = "id_favorito"
        private const val COLUMN_ID_USUARIO = "id_usuario"
        private const val COLUMN_TITULO = "titulo"
        private const val COLUMN_AUTOR = "autor"
        private const val COLUMN_PORTADA = "portada"
        private const val COLUMN_ENLACE = "enlace"

        // Crear tabla de usuarios
        private val CREATE_TABLE_USUARIOS = """
            CREATE TABLE $TABLE_USUARIOS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOMBRE TEXT NOT NULL,
                $COLUMN_CORREO TEXT NOT NULL UNIQUE,
                $COLUMN_PASSWORD TEXT NOT NULL
            )
        """

        // Crear tabla de libros favoritos
        private val CREATE_TABLE_LIBROS_FAVORITOS = """
            CREATE TABLE $TABLE_LIBROS_FAVORITOS (
                $COLUMN_ID_FAVORITO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ID_USUARIO INTEGER NOT NULL,
                $COLUMN_TITULO TEXT NOT NULL,
                $COLUMN_AUTOR TEXT NOT NULL,
                $COLUMN_PORTADA TEXT NOT NULL,
                $COLUMN_ENLACE TEXT NOT NULL,
                UNIQUE($COLUMN_ID_USUARIO, $COLUMN_TITULO)
            )
        """
    }

    // Crear la base de datos y sus tablas
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE_USUARIOS)
        db?.execSQL(CREATE_TABLE_LIBROS_FAVORITOS)
    }

    // Actualizar la base de datos
    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        // La tabla de favoritos cambió de estructura (UNIQUE, sin FOREIGN KEY).
        // Se vuelve a crear; la tabla de usuarios NO se toca.
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_LIBROS_FAVORITOS")
        db?.execSQL(CREATE_TABLE_LIBROS_FAVORITOS)
    }

    // Método para registrar usuarios
    fun registrarUsuario(
        nombre: String,
        correo: String,
        password: String
    ): Boolean {

        // Obtener permisos de escritura
        val db = writableDatabase

        // Guardar la información
        val datos = ContentValues().apply {
            put(COLUMN_NOMBRE, nombre)
            put(COLUMN_CORREO, correo)
            put(COLUMN_PASSWORD, password)
        }

        // Insertar los datos
        val resultado = db.insert(
            TABLE_USUARIOS,
            null,
            datos
        )

        return resultado != -1L
    }

    // Método para iniciar sesión
    fun iniciarSesion(
        correo: String,
        password: String
    ): Boolean {

        val db = readableDatabase

        val sql = "SELECT $COLUMN_ID FROM $TABLE_USUARIOS " +
                "WHERE $COLUMN_CORREO = ? AND $COLUMN_PASSWORD = ?"

        val cursor = db.rawQuery(
            sql,
            arrayOf(correo, password)
        )

        // Verificar si existe el usuario
        val usuarioEncontrado = cursor.count > 0

        cursor.close()

        return usuarioEncontrado
    }

    // Devuelve el id del usuario si el login es correcto, o -1 si no
    fun obtenerIdUsuario(correo: String, password: String): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT $COLUMN_ID FROM $TABLE_USUARIOS " +
                    "WHERE $COLUMN_CORREO = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(correo, password)
        )
        val id = if (cursor.moveToFirst()) cursor.getInt(0) else -1
        cursor.close()
        return id
    }

    // Método para guardar libros favoritos
    fun guardarLibroFavorito(
        idUsuario: Int,
        titulo: String,
        autor: String,
        portada: String,
        enlace: String
    ): Boolean {

        return try {
            val datos = ContentValues().apply {
                put(COLUMN_ID_USUARIO, idUsuario)
                put(COLUMN_TITULO, titulo)
                put(COLUMN_AUTOR, autor)
                put(COLUMN_PORTADA, portada)
                put(COLUMN_ENLACE, enlace)
            }
            val resultado = writableDatabase.insertWithOnConflict(
                TABLE_LIBROS_FAVORITOS,
                null,
                datos,
                SQLiteDatabase.CONFLICT_IGNORE
            )
            // -1 = ya existía o falló; si ya existía lo damos por guardado
            resultado != -1L || existeLibroFavorito(idUsuario, titulo)
        } catch (e: Exception) {
            android.util.Log.e("DatabaseHelper", "Error al guardar favorito", e)
            false
        }
    }

    // Método para eliminar libros favoritos
    fun eliminarLibroFavorito(
        idUsuario: Int,
        titulo: String
    ): Boolean {

        return try {
            writableDatabase.delete(
                TABLE_LIBROS_FAVORITOS,
                "$COLUMN_ID_USUARIO = ? AND $COLUMN_TITULO = ?",
                arrayOf(idUsuario.toString(), titulo)
            ) > 0
        } catch (e: Exception) {
            android.util.Log.e("DatabaseHelper", "Error al eliminar favorito", e)
            false
        }
    }

    // Método para consultar si un libro ya es favorito
    fun existeLibroFavorito(
        idUsuario: Int,
        titulo: String
    ): Boolean {

        return try {
            val cursor = readableDatabase.rawQuery(
                "SELECT $COLUMN_TITULO FROM $TABLE_LIBROS_FAVORITOS " +
                        "WHERE $COLUMN_ID_USUARIO = ? AND $COLUMN_TITULO = ?",
                arrayOf(idUsuario.toString(), titulo)
            )
            val existe = cursor.moveToFirst()
            cursor.close()
            existe
        } catch (e: Exception) {
            android.util.Log.e("DatabaseHelper", "Error al consultar favorito", e)
            false
        }
    }
}