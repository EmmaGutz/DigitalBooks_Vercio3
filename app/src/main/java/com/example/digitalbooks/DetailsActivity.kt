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

/**
 * Pantalla de detalle (equivalente a DatellActivity de la app de música).
 * Recibe por Intent los datos del libro que vienen del API y los muestra.
 * Todavía NO usa la base de datos: el botón "Guardar" solo marca el libro,
 * pero ya tiene todos los datos listos para guardarlos después en la BD.
 */
class DetailsActivity : AppCompatActivity() {

    private lateinit var imgPortadaDetalle: ImageView
    private lateinit var tvTituloDetalle: TextView
    private lateinit var tvAutorDetalle: TextView
    private lateinit var tvInfoDetalle: TextView
    private lateinit var btnGuardar: Button
    private lateinit var btnVerLibro: Button
    private lateinit var btnVolver: TextView

    private var estaGuardado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_details)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imgPortadaDetalle = findViewById(R.id.imgPortadaDetalle)
        tvTituloDetalle = findViewById(R.id.tvTituloDetalle)
        tvAutorDetalle = findViewById(R.id.tvAutorDetalle)
        tvInfoDetalle = findViewById(R.id.tvInfoDetalle)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnVerLibro = findViewById(R.id.btnVerLibro)
        btnVolver = findViewById(R.id.btnVolver)

        btnVolver.setOnClickListener { finish() }

        // Estado del botón al abrir la pantalla (se conserva al girar el teléfono)
        estaGuardado = savedInstanceState?.getBoolean("estaGuardado") ?: false
        actualizarBotonGuardar()

        mostrarInformacionLibro()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("estaGuardado", estaGuardado)
    }

    // Lee el Intent con valores por defecto para que un dato nulo no cierre la app
    private fun mostrarInformacionLibro() {
        val titulo = intent.getStringExtra("TituloLibro") ?: "Sin título"
        val autores = intent.getStringExtra("AutoresLibro") ?: "Autor desconocido"
        val enlace = intent.getStringExtra("EnlaceLibro")
        val portada = intent.getStringExtra("PortadaLibro")
        val anio = intent.getStringExtra("AnioLibro")
        val paginas = intent.getIntExtra("PaginasLibro", 0)
        val fuente = intent.getStringExtra("FuenteLibro")

        tvTituloDetalle.text = titulo
        tvAutorDetalle.text = autores

        // Solo mostramos los datos que el API sí devolvió
        val detalles = ArrayList<String>()
        if (!anio.isNullOrBlank()) detalles.add("Año de publicación: $anio")
        if (paginas > 0) detalles.add("Páginas: $paginas")
        if (!fuente.isNullOrBlank()) detalles.add("Fuente: $fuente")
        tvInfoDetalle.text = if (detalles.isEmpty()) "Sin más detalles disponibles"
        else detalles.joinToString("\n")

        // Portada en tamaño grande (Open Library: -M -> -L). Si no hay, queda el fondo oscuro.
        if (!portada.isNullOrBlank()) {
            Glide.with(this)
                .load(portada.replace("-M.jpg", "-L.jpg"))
                .into(imgPortadaDetalle)
        }

        btnGuardar.setOnClickListener {
            estaGuardado = !estaGuardado
            actualizarBotonGuardar()
            if (estaGuardado) {
                // TODO (siguiente etapa): aquí se insertará en la base de datos
                // titulo, autores, portada, enlace, anio, paginas y fuente.
                Toast.makeText(this, "\"$titulo\" guardado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "\"$titulo\" quitado de guardados", Toast.LENGTH_SHORT).show()
            }
        }

        btnVerLibro.setOnClickListener {
            if (enlace.isNullOrBlank()) {
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
