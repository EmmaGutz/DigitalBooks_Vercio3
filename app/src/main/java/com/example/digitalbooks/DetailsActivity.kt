package com.example.digitalbooks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class DetailsActivity : AppCompatActivity() {

    private lateinit var imgPortadaDetalle: ImageView
    private lateinit var tvTituloDetalle: TextView
    private lateinit var tvAutorDetalle: TextView
    private lateinit var tvInfoDetalle: TextView
    private lateinit var btnGuardar: Button
    private lateinit var btnVerLibro: Button
    private lateinit var btnVolver: TextView

    // Instancia de nuestra base de datos
    private lateinit var dbHelper: DatabaseHelper
    private var estaGuardado = false

    // ID del usuario actual (se guarda en SharedPreferences al iniciar sesión)
    private var idUsuarioActual = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_details)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar el DatabaseHelper
        dbHelper = DatabaseHelper(this)
        idUsuarioActual = getSharedPreferences("sesion", MODE_PRIVATE).getInt("id_usuario", 0)

        imgPortadaDetalle = findViewById(R.id.imgPortadaDetalle)
        tvTituloDetalle = findViewById(R.id.tvTituloDetalle)
        tvAutorDetalle = findViewById(R.id.tvAutorDetalle)
        tvInfoDetalle = findViewById(R.id.tvInfoDetalle)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnVerLibro = findViewById(R.id.btnVerLibro)
        btnVolver = findViewById(R.id.btnVolver)

        btnVolver.setOnClickListener { finish() }

        mostrarInformacionLibro()
    }

    private fun mostrarInformacionLibro() {
        val titulo = intent.getStringExtra("TituloLibro") ?: "Sin título"
        val autores = intent.getStringExtra("AutoresLibro") ?: "Autor desconocido"
        val enlace = intent.getStringExtra("EnlaceLibro") ?: ""
        val portada = intent.getStringExtra("PortadaLibro") ?: ""
        val anio = intent.getStringExtra("AnioLibro") ?: ""
        val paginas = intent.getIntExtra("PaginasLibro", 0)
        val fuente = intent.getStringExtra("FuenteLibro")

        tvTituloDetalle.text = titulo
        tvAutorDetalle.text = autores

        // Verificar en la base de datos si este libro ya es favorito de este usuario
        estaGuardado = dbHelper.existeLibroFavorito(idUsuarioActual, titulo)
        actualizarBotonGuardar()

        val detalles = ArrayList<String>()
        if (!anio.isNullOrBlank()) detalles.add("Año de publicación: $anio")
        if (paginas > 0) detalles.add("Páginas: $paginas")
        if (!fuente.isNullOrBlank()) detalles.add("Fuente: $fuente")
        tvInfoDetalle.text = if (detalles.isEmpty()) "Sin más detalles disponibles"
        else detalles.joinToString("\n")

        if (portada.isNotBlank()) {
            Glide.with(this)
                .load(portada.replace("-M.jpg", "-L.jpg"))
                .into(imgPortadaDetalle)
        }

        btnGuardar.setOnClickListener {
            if (!estaGuardado) {
                // Intentar guardar en la base de datos
                val guardadoExitoso = dbHelper.guardarLibroFavorito(
                    idUsuario = idUsuarioActual,
                    titulo = titulo,
                    autor = autores,
                    portada = portada,
                    enlace = enlace
                )

                if (guardadoExitoso) {
                    estaGuardado = true
                    actualizarBotonGuardar()
                    Toast.makeText(this, "\"$titulo\" guardado en favoritos", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al guardar el libro", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Eliminar de la base de datos si ya estaba guardado
                val eliminadoExitoso = dbHelper.eliminarLibroFavorito(idUsuarioActual, titulo)

                if (eliminadoExitoso) {
                    estaGuardado = false
                    actualizarBotonGuardar()
                    Toast.makeText(this, "\"$titulo\" quitado de favoritos", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al quitar de favoritos", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnVerLibro.setOnClickListener {
            if (enlace.isBlank()) {
                Toast.makeText(this, "Enlace no disponible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(enlace)))
            } catch (e: Exception) {
                Toast.makeText(this, "No se encontró una aplicación para abrir el enlace", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarBotonGuardar() {
        if (estaGuardado) {
            btnGuardar.text = "Guardado"
            btnGuardar.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_guardado, 0, 0, 0)
        } else {
            btnGuardar.text = "Guardar libro"
            btnGuardar.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_no_guardado, 0, 0, 0)
        }
    }
}