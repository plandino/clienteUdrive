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

import org.json.JSONObject;


public class RegistrarseActivity extends AppCompatActivity implements View.OnClickListener {

    Button registrarseButton;
    EditText usuarioEditText;
    EditText contraseniaEditText;

    private static final String QUERY_URL = "http://192.168.0.27:8080/santi";

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
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        String usuarioYContrasenia = usuarioEditText.getText().toString() + contraseniaEditText.getText().toString();
        registrar(usuarioYContrasenia);

    }

    private void pasarAlMain(){

        // Creo el Intent para pasar al MainActivity
        Intent mainIntent = new Intent(this, MainActivity.class);

        // Agrego la informacion al Intent
        mainIntent.putExtra("yaInicio", true);

        // Arranco la Activity con el Intent
        startActivity(mainIntent);
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void registrar(String usuarioYContrasenia) {

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

        // Creo un cliente para mandar la informacio de usuario y contraseña
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(QUERY_URL , new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(JSONObject jsonObject) {
                        Toast.makeText(getApplicationContext(), "Usuario registrado: " + jsonObject.toString(), Toast.LENGTH_LONG).show();
                        pasarAlMain();
                    }

                    @Override
                    public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                        Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                    }
                });
    }

}
