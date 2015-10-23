package tallerii.udrive;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

public class IniciarSesionActivity extends AppCompatActivity implements View.OnClickListener  {

    Button iniciarSesionButton;
    EditText usuarioEditText;
    EditText contraseniaEditText;
//    String jsonEstructuraCarpetas;

    // FACULTAD
    // private String QUERY_URL = "http://192.168.0.31:8080/profile";

    // MI CASA
//    private String QUERY_URL = "http://192.168.0.27:8080/session";

    // Compu santi mi casa
//    private String QUERY_URL = "http://192.168.0.39:8080/session";

    private String QUERY_URL = MyDataArrays.direccion + "/session";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        String usuario = usuarioEditText.getText().toString();
        String contrasenia = contraseniaEditText.getText().toString();
//        usuario = "p";
//        contrasenia = "pppppppp";
        if(usuario.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No puede ingresar un nombre de usuario vacio.",
                    Toast.LENGTH_LONG).show();
        }

        if(contrasenia.isEmpty()){
            Toast.makeText(getApplicationContext(), "No puede ingresar una contraseña vacia.",
                    Toast.LENGTH_LONG).show();
        }

        if( (!usuario.isEmpty()) && (!contrasenia.isEmpty())){
            iniciarSesion(usuario, contrasenia);
        }
    }

    private void pasarAlMain(String token, String user){

        // Creo un Intent para pasar al main
        Intent mainIntent = new Intent(this, MainActivity.class);

        // Agrego la informacion que quiero al Intent
        mainIntent.putExtra("username", user);
        mainIntent.putExtra("token", token);
//        mainIntent.putExtra("estructuraCarpetas", jsonEstructuraCarpetas);

        // Inicio la actividad con el Intent
        startActivity(mainIntent);
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void iniciarSesion(final String usuario, String contrasenia) {

//        JSONObject jsonEstructuraCarpetas;

        // Creo un cliente para mandar la informacion de usuario y contraseña
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("user", usuario);
        params.put("pass", contrasenia);

        client.post(QUERY_URL, params, new JsonHttpResponseHandler() {
            //
            @Override
            public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
//                Toast.makeText(getApplicationContext(), "Token: " + jsonObject.toString(), Toast.LENGTH_LONG).show();
                String token = "";
                try{
                    token                   = jsonObject.getString("token");
                } catch (JSONException e){

                }
                pasarAlMain(token, usuario);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }
}