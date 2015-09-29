package tallerii.udrive;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
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


public class RegistrarseActivity extends AppCompatActivity implements View.OnClickListener {

    Button registrarseButton;
    EditText usuarioEditText;
    EditText contraseniaEditText;

    private String QUERY_URL = "http://192.168.0.31:8080/profile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrarse);

        // Edit text para el usuario
        usuarioEditText = (EditText) findViewById(R.id.registrarse_usuario);

        // Edit text para la contraseña
        contraseniaEditText = (EditText) findViewById(R.id.registrarse_contrasenia);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        String usuario = usuarioEditText.getText().toString();
        String contrasenia = contraseniaEditText.getText().toString();
        if(usuario.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No puede ingresar un nombre de usuario vacio.",
                    Toast.LENGTH_LONG).show();
        }

        if(contrasenia.isEmpty()){
            Toast.makeText(getApplicationContext(), "No puede ingresar una contraseña vacia.",
                    Toast.LENGTH_LONG).show();
        }

        if( (!usuario.isEmpty()) && (!contrasenia.isEmpty())){
            registrar(usuario, contrasenia);
        }
    }

    private void pasarAlMain(String token, String user){

        // Creo el Intent para pasar al MainActivity
        Intent mainIntent = new Intent(this, MainActivity.class);

        // Agrego la informacion al Intent
        mainIntent.putExtra("username", user);
        mainIntent.putExtra("token", token);

        // Arranco la Activity con el Intent
        startActivity(mainIntent);
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void iniciarSesion(final String usuario, String contrasenia) {

        QUERY_URL = "http://192.168.0.31:8080/session";

        // Prepare your search string to be put in a URL
        // It might have reserved characters or something
        //String urlString = "";
        //try {
        //urlString = URLEncoder.encode(searchString, "UTF-8");
        //} catch (UnsupportedEncodingException e) {

        // if this fails for some reason, let the user know why
        //  e.printStackTrace();
        // Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        //}

        // Creo un cliente para mandar la informacion de usuario y contraseña
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("user", usuario);
        params.put("pass", contrasenia);

        client.post(QUERY_URL, params, new JsonHttpResponseHandler() {
            //
            @Override
            public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
                Toast.makeText(getApplicationContext(), "Token: " + jsonObject.toString(), Toast.LENGTH_LONG).show();
                String token = "";
                try {
                    token = jsonObject.getString("token");
                } catch (JSONException e) {

                }
                pasarAlMain(token, usuario);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registrar(final String usuario, final String contrasenia) {

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("user", usuario);
        params.put("pass", contrasenia);
        params.put("profile", "holaadadsad" );

        client.post(QUERY_URL, params, new JsonHttpResponseHandler() {
            //
            @Override
            public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
                Toast.makeText(getApplicationContext(), "Usuario registrado: ", Toast.LENGTH_LONG).show();
                iniciarSesion(usuario,contrasenia);
//                pasarAlMain();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

}
