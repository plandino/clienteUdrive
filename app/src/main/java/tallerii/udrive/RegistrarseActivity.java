package tallerii.udrive;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Esta clase se encargar de controlar la activity para registrar un nuevo usuario.
 */
public class RegistrarseActivity extends AppCompatActivity implements View.OnClickListener {

    Button registrarseButton;
    EditText usuarioEditText;
    EditText contraseniaEditText;
    EditText nombreEditText;
    EditText mailEditText;

    public String QUERY_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("REGISTRARSE: ", "Se inicio la RegistrarseActivity.");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrarse);

        // Edit text para el usuario
        usuarioEditText = (EditText) findViewById(R.id.registrarse_usuario);

        // Edit text para la contraseña
        contraseniaEditText = (EditText) findViewById(R.id.registrarse_contraseña);

        // Edit text para el nombre
        nombreEditText = (EditText) findViewById(R.id.registrarse_nombre);

        // Edit text para el mail
        mailEditText = (EditText) findViewById(R.id.registrarse_mail);

        // Boton para registrarse
        registrarseButton = (Button) findViewById(R.id.registrarse);
        registrarseButton.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_all, menu);
        return true;
    }

    @Override
    public void onClick(View v) {

        Log.i("REGISTRARSE: ", "Se hizo click en el boton para registrarse.");

        String usuario      = usuarioEditText.getText().toString();
        String contrasenia  = contraseniaEditText.getText().toString();
        String mail         = mailEditText.getText().toString();
        String nombre       = nombreEditText.getText().toString();

        // TODO: SACAR ESTE HARDOCDEO
        mail = "pancheitor@gmail.com";
//        nombre = "pancheitor";
//        usuario = "p";
//        contrasenia = "pppppppp";

        Log.d("REGISTRARSE: ", "El nombre ingresado es: \"" + nombre + "\".");
        Log.d("REGISTRARSE: ", "El usuario ingresado es: \"" + usuario + "\".");
        Log.d("REGISTRARSE: ", "La contraseña ingresada es: \"" + contrasenia + "\".");
        Log.d("REGISTRARSE: ", "El mail ingresado es: \"" + mail + "\".");

        if(usuario.isEmpty()) {
            Log.d("REGISTRARSE: ", "Se ingreso un usuario vacio.");

            Toast.makeText(getApplicationContext(), "No puede ingresar un nombre de usuario vacio.",
                    Toast.LENGTH_LONG).show();
        }

        if(contrasenia.isEmpty()){
            Log.d("REGISTRARSE: ", "Se ingreso una contraseña vacia.");

            Toast.makeText(getApplicationContext(), "No puede ingresar una contraseña vacia.",
                    Toast.LENGTH_LONG).show();
        }

        if(mail.isEmpty()){
            Log.d("REGISTRARSE: ", "Se ingreso un mail vacio.");

            Toast.makeText(getApplicationContext(), "No puede ingresar un mail vacio.",
                    Toast.LENGTH_LONG).show();
        }

        if(nombre.isEmpty()){
            Log.d("REGISTRARSE: ", "Se ingreso un nombre vacio.");

            Toast.makeText(getApplicationContext(), "No puede ingresar un nombre vacio.",
                    Toast.LENGTH_LONG).show();
        }

        if( (!usuario.isEmpty()) && (!contrasenia.isEmpty()) && (!mail.isEmpty()) && (!nombre.isEmpty())){
            Log.i("REGISTRARSE: ", "Se ingresaron todos los campos completos.");

            registrar(usuario, contrasenia, mail, nombre);
        }
    }

    private void registrar(final String usuario, final String contrasenia, final String mail, final String nombre) {

        Log.i("REGISTRARSE: ", "Voy a registrar un nuevo usuario.");

        QUERY_URL = MyDataArrays.direccion + "/profile";

        Log.d("REGISTRARSE: ", "Le pego a la URL: " + QUERY_URL);

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("user", usuario);
        params.put("pass", contrasenia);
        
        String premium = "false";
        params.put("premium", premium);
        
        String perfil = "{\"nombre\" : \"" + nombre + "\",\"email\" :  \"" + mail + "\"}";
        params.put("profile",  perfil);

        Log.d("REGISTRARSE: ", "Voy a ingresar los siguientes datos en los parametros de la request: ");
        Log.d("REGISTRARSE: ", "El usuario ingresado es: \"" + usuario + "\".");
        Log.d("REGISTRARSE: ", "La contraseña ingresada es: \"" + contrasenia + "\".");
        Log.d("REGISTRARSE: ", "El usuario es premium: \"" + premium + "\".");
        Log.d("REGISTRARSE: ", "El perfil en formato JSON es: \"" + perfil + "\".");

        client.post(QUERY_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int status,JSONObject jsonObject) {
                Log.d("REGISTRARSE: ", "Usuario registrado correctamente: \"" + usuario + "\".");

                Toast.makeText(getApplicationContext(), "Usuario registrado: " + usuario, Toast.LENGTH_LONG).show();
                iniciarSesion(usuario, contrasenia);
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                Log.d("REGISTRARSE: ", "StatusCode: \"" + statusCode + "\".");

                if(error == null){
                    Log.e("REGISTRARSE: ", "No se pudo comunicar con el servidor.");

                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        Log.d("REGISTRARSE: ", "Contraseña o usuario incorrecto.");
                        Log.d("REGISTRARSE: ", error.getString("error"));

                        Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        Log.e("REGISTRARSE: ", e.getMessage());
                        Log.e("REGISTRARSE: ", "No se pudo obtener el mensaje de error del JSON.");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Con este metodo se hace un POST para inicair sesion con el usuario con los siguientes datos
     * @param usuario username del usuario
     * @param contrasenia contraseña del usuario
     */
    private void iniciarSesion(final String usuario, String contrasenia) {

        Log.i("REGISTRARSE: ", "Voy a iniciar sesion.");

        QUERY_URL = MyDataArrays.direccion + "/session";

        Log.d("REGISTRARSE: ", "Le pego a la URL: " + QUERY_URL);

        // Creo un cliente para mandar la informacion de usuario y contraseña
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("user", usuario);
        params.put("pass", contrasenia);

        Log.d("REGISTRARSE: ", "Voy a ingresar los siguientes datos en los parametros de la request: ");
        Log.d("REGISTRARSE: ", "El usuario ingresado es: \"" + usuario + "\".");
        Log.d("REGISTRARSE: ", "La contraseña ingresada es: \"" + contrasenia + "\".");

        client.post(QUERY_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int status, JSONObject jsonObject) {
                Log.d("REGISTRARSE: ", "Pude iniciar sesion correctamente.");

                String token = "";
                try {
                    token = jsonObject.getString("token");
                    Log.d("REGISTRARSE: ", "Obtuve el token: " + token);

                } catch (JSONException e) {
                    Log.e("REGISTRARSE: ", "No se pudo obtener el token del JSON.");
                    Log.e("REGISTRARSE: ", e.getMessage());
                    e.printStackTrace();
                }
                pasarAlMain(token, usuario);
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                Log.d("REGISTRARSE: ", "StatusCode: \"" + statusCode + "\".");

                if(error == null){
                    Log.e("REGISTRARSE: ", "No se pudo comunicar con el servidor.");

                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        Log.d("REGISTRARSE: ", "Contraseña o usuario incorrecto.");
                        Log.d("REGISTRARSE: ", error.getString("error"));

                        Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        Log.e("REGISTRARSE: ", e.getMessage());
                        Log.e("REGISTRARSE: ", "No se pudo obtener el mensaje de error del JSON.");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Paso a la MainActivity luego de recibir el token de la sesion.
     * Le mando los siguientes parametros a traves del Intent.
     *
     * @param token que recibi desde el servidor
     * @param user username del usuario con el que inicie sesion
     */
    private void pasarAlMain(String token, String user){

        Log.i("REGISTRARSE: ", "Voy a pasar a la MainActivity.");

        // Creo un Intent para pasar al main
        Intent mainIntent = new Intent(this, MainActivity.class);


        Log.d("REGISTRARSE: ", "Mando al MainActivity el user: \"" + user + "\".");
        Log.d("REGISTRARSE: ", "Mando al MainActivity el token: \"" + token + "\".");

        // Agrego la informacion que quiero al Intent
        mainIntent.putExtra("username", user);
        mainIntent.putExtra("token", token);

        // Inicio la actividad con el Intent
        startActivity(mainIntent);
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

}
