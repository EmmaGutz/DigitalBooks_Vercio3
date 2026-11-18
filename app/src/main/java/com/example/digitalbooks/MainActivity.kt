package com.example.digitalbooks

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Solo los campos que EXISTEN en activity_main.xml
    private lateinit var etCorreoLogin: EditText
    private lateinit var etPasswordLogin: EditText
    private lateinit var btnIniciar: Button
    private lateinit var btnCrearCuenta: TextView
    private lateinit var dbHelper: DatabaseHelper
    // crear autenticación con google
    private lateinit var credentialManager: CredentialManager
    private lateinit var auth: FirebaseAuth
    private lateinit var  btnGoogle: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Los IDs deben coincidir con los del XML
        etCorreoLogin = findViewById(R.id.etCorreoLogin)
        etPasswordLogin = findViewById(R.id.etPasswordLogin)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta)

        dbHelper = DatabaseHelper(this)
        btnGoogle = findViewById(R.id.btnGoogle)
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        // Botón de iniciar sesión
        btnIniciar.setOnClickListener {
            iniciarSesion()
        }
        btnGoogle.setOnClickListener {
            iniciarSesionGoogle()
        }


        // Texto "Crear una Cuenta" -> pantalla de registro
        btnCrearCuenta.setOnClickListener {
            startActivity(Intent(this, InicioSecion::class.java))
        }
    }

    // La función va FUERA de onCreate, a nivel de clase
    private fun iniciarSesion() {
        val correo = etCorreoLogin.text.toString().trim().lowercase()
        val password = etPasswordLogin.text.toString().trim()

        // 1. Campos vacíos
        if (correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Formato de correo
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreoLogin.error = "Correo electrónico no válido"
            etCorreoLogin.requestFocus()
            return
        }

        // 3. Verificar en la base de datos
        val idUsuario = dbHelper.obtenerIdUsuario(correo, password)
        if (idUsuario != -1) {
            getSharedPreferences("sesion", MODE_PRIVATE).edit()
                .putInt("id_usuario", idUsuario).apply()
            Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } else {
            // Sin finish(): el usuario debe poder reintentar
            Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. MÉTODO PARA AUTENTICAR EN FIREBSAE
    private fun autenticarFirebase(idToken: String){

        val credencialFirebase = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credencialFirebase).addOnCompleteListener(this) {

                inicio ->
            //validar si el inicio fue exitoso
            if (inicio.isSuccessful) {
                //si el inicio fue exitoso, se obtiene el usuario actual
                val usuario = auth.currentUser
                // Usuario de Google: no tiene fila en la tabla usuarios, se usa id 0
                getSharedPreferences("sesion", MODE_PRIVATE).edit()
                    .putInt("id_usuario", 0).apply()
                Toast.makeText(this, "Bienvenido papu ${usuario?.displayName}", Toast.LENGTH_SHORT)
                    .show()
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Error en la autenticacion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 4. MÉTODO PARA PROCESAR LA CREDENCIAL
    private fun procesarCredencial(credential: Credential){

        if(credential is CustomCredential && credential.type ==
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){

            try {
                //variable que permita identificar la credencia del usuario
                val credencialGoogle = GoogleIdTokenCredential.createFrom(credential.data)
                autenticarFirebase(credencialGoogle.idToken)


            }catch (e : GoogleIdTokenParsingException){

                Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
            }
        }

    }

    // 5. MÉTODO PARA INICIAR SESIÓN
    private fun iniciarSesionGoogle(){

        val inicioGoogle = GetSignInWithGoogleOption.Builder(
            getString(R.string.default_web_client_id)).build()

        val solicitud = GetCredentialRequest.Builder()
            .addCredentialOption(inicioGoogle)
            .build()

        lifecycleScope.launch {
            try {
                //verificar si las credeciales se pueden usar
                val resultado = credentialManager.getCredential(context = this@MainActivity, request = solicitud)
                //prosesar credencial para saber si el usuario entra o no
                procesarCredencial(resultado.credential)
            } catch (e: GetCredentialException) {
                android.util.Log.e("GOOGLE_LOGIN", "Tipo: ${e.type} | Mensaje: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

    }

}