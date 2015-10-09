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
    EditText nombreEditText;
    EditText mailEditText;

    public String QUERY_URL = MyDataArrays.direccion + "/profile";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        String usuario      = usuarioEditText.getText().toString();
        String contrasenia  = contraseniaEditText.getText().toString();
        String mail         = mailEditText.getText().toString();
        String nombre       = nombreEditText.getText().toString();

        // TODO: SACAR ESTE HARDOCDEO
//        mail = "pancheitor@gmail.com";
//        nombre = "pancheitor";
        if(usuario.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No puede ingresar un nombre de usuario vacio.",
                    Toast.LENGTH_LONG).show();
        }

        if(contrasenia.isEmpty()){
            Toast.makeText(getApplicationContext(), "No puede ingresar una contraseña vacia.",
                    Toast.LENGTH_LONG).show();
        }

        if( (!usuario.isEmpty()) && (!contrasenia.isEmpty())){
            registrar(usuario, contrasenia, mail, nombre);
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

        QUERY_URL = MyDataArrays.direccion + "/session";

        // Creo un cliente para mandar la informacion de usuario y contraseña
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("user", usuario);
        params.put("pass", contrasenia);

        client.post(QUERY_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
                String token = "";
                try {
                    token = jsonObject.getString("token");
                } catch (JSONException e) {

                }
                pasarAlMain(token, usuario);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor en iniciar", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registrar(final String usuario, final String contrasenia, final String mail, final String nombre) {

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("user", usuario);
        params.put("pass", contrasenia);
        params.put("profile", "{\"nombre\" : \"" + nombre + "\",\"email\" :  \"" + mail + "\"}" );
//
//        Toast.makeText(getApplicationContext(), "{\"nombre\" : \" " + nombre + "\"," +
//                "               \"email\" :  \" " + mail + "\"}", Toast.LENGTH_LONG).show();

        client.post(QUERY_URL, params, new JsonHttpResponseHandler() {
            //
            @Override
            public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
                Toast.makeText(getApplicationContext(), "Usuario registrado: " + usuario, Toast.LENGTH_LONG).show();
                iniciarSesion(usuario,contrasenia);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor en registrar", Toast.LENGTH_LONG).show();
            }
        });
    }

}
