package tallerii.udrive;

import android.content.Intent;
import android.content.SharedPreferences;
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
 * Esta clase se encarga de manejar la Activity para inciar sesion con un usuario ya existente.
 */
public class IniciarSesionActivity extends AppCompatActivity implements View.OnClickListener  {

    Button iniciarSesionButton;
    EditText usuarioEditText;
    EditText contraseniaEditText;

    private String QUERY_URL = MyDataArrays.direccion + "/session";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("INICIAR_SESION: ", "Se inicio la IniciarSesionActivity.");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iniciar_sesion);

        // Edit text para ingresar el usuario
        usuarioEditText = (EditText) findViewById(R.id.iniciar_sesion_usuario);

        // Edit text para ingresar la contraseña
        contraseniaEditText = (EditText) findViewById(R.id.iniciar_sesion_contraseña);

        // Boton para iniciar sesion
        iniciarSesionButton = (Button) findViewById(R.id.iniciar_sesion);
        iniciarSesionButton.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_all, menu);
        return true;
    }

    @Override
    public void onClick(View v) {

        Log.i("INICIAR_SESION: ", "Se hizo click en el boton para iniciar sesion.");

        String usuario = usuarioEditText.getText().toString();
        String contrasenia = contraseniaEditText.getText().toString();

        Log.d("INICIAR_SESION: ", "El usuario ingresado es: \"" + usuario + "\".");
        Log.d("INICIAR_SESION: ", "La contraseña ingresada es: \"" + contrasenia + "\".");

//        usuario = "p";
//        contrasenia = "pppppppp";
        if(usuario.isEmpty()) {
            Log.d("INICIAR_SESION: ", "Se ingreso un nombre de usuario vacio.");

            Toast.makeText(getApplicationContext(), "No puede ingresar un nombre de usuario vacio",
                    Toast.LENGTH_LONG).show();
        }

        if(contrasenia.isEmpty()){
            Log.d("INICIAR_SESION: ", "Se ingreso una contraseña vacia.");

            Toast.makeText(getApplicationContext(), "No puede ingresar una contraseña vacia.",
                    Toast.LENGTH_LONG).show();
        }

        if( (!usuario.isEmpty()) && (!contrasenia.isEmpty())){

            Log.i("INICIAR_SESION: ", "Se ingresaron ambos campos de usuario y contraseña completos.");

            iniciarSesion(usuario, contrasenia);
        }
    }

    /**
     * Manda un POST a la QUERY_URL para iniciar sesion y obtener el token de la sesion.
     *
     * @param usuario username con el cual se desea iniciar sesion
     * @param contrasenia contraseña del user con el cual se desea iniciar sesion
     */
    private void iniciarSesion(final String usuario, String contrasenia) {

        Log.d("INICIAR_SESION: ", "Voy a iniciar sesion.");

        // Creo un cliente para mandar la informacion de usuario y contraseña
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("user", usuario);
        params.put("pass", contrasenia);

        Log.d("INICIAR_SESION: ", "Le pego a la URL: " + QUERY_URL);

        Log.d("INICIAR_SESION: ", "Voy a ingresar los siguientes datos en los parametros de la request: ");
        Log.d("INICIAR_SESION: ", "El usuario ingresado es: \"" + usuario + "\".");
        Log.d("INICIAR_SESION: ", "La contraseña ingresada es: \"" + contrasenia + "\".");


        client.post(QUERY_URL, params, new JsonHttpResponseHandler() {
            //
            @Override
            public void onSuccess(int status, JSONObject jsonObject) {
                String token = "";
                Log.d("INICIAR_SESION: ", "Sesion iniciada.");
                try {
                    token = jsonObject.getString("token");
                    Log.d("INICIAR_SESION: ", "Obtuve el token: " + token);

                } catch (JSONException e) {
                    Log.e("INICIAR_SESION: ", "No se pudo obtener el token del JSON.");
                    Log.e("INICIAR_SESION: ", e.getMessage());
                    e.printStackTrace();
                }

                guardarDatosUsuario(token, usuario);
                pasarAlMain(token, usuario);
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {

                Log.d("INICIAR_SESION: ", "StatusCode: \"" + statusCode + "\".");

                if(error == null){
                    Log.e("INICIAR_SESION: ", "No se pudo comunicar con el servidor.");

                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        Log.d("INICIAR_SESION: ", "Contraseña o usuario incorrecto.");
                        Log.d("INICIAR_SESION: ", error.getString("error"));

                        Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        Log.e("INICIAR_SESION: ", e.getMessage());
                        Log.e("INICIAR_SESION: ", "No se pudo obtener el mensaje de error del JSON.");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void guardarDatosUsuario(String token, String usuario){

        SharedPreferences mSharedPreferences;

        // Accedo a los datos guardados
        mSharedPreferences = getSharedPreferences(MyDataArrays.SESION_DATA, MODE_PRIVATE);

        // Guardo en la memoria, el username y el token
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.putString(MyDataArrays.USERNAME, usuario);
        e.putString(MyDataArrays.TOKEN, token);
        e.apply();
    }

    /**
     * Paso a la MainActivity luego de recibir el token de la sesion.
     * Le mando los siguientes parametros a traves del Intent.
     *
     * @param token que recibi desde el servidor
     * @param user username del usuario con el que inicie sesion
     */
    private void pasarAlMain(String token, String user){

        Log.i("INICIAR_SESION: ", "Voy a pasar a la MainActivity.");

        // Creo un Intent para pasar al main
        Intent mainIntent = new Intent(this, MainActivity.class);


        Log.d("INICIAR_SESION: ", "Mando al MainActivity el user: \"" + user + "\".");
        Log.d("INICIAR_SESION: ", "Mando al MainActivity el token: \"" + token + "\".");

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