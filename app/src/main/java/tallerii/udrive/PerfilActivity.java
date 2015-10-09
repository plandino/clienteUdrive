package tallerii.udrive;

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

/**
 * Created by panchoubuntu on 29/09/15.
 */
public class PerfilActivity extends AppCompatActivity implements View.OnClickListener {

    Button actualizarButton;
    EditText nombreEditText;
    EditText mailEditText;
    EditText fotoEditText;

    String nombreUsuario = "";
    String email = "";
    String fotopath = "";
    double latitud = 1.0;
    double longitud = 2.0;

    private String token = "";
    private String username = "";

    private String QUERY_URL = MyDataArrays.direccion + "/profile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        token = getIntent().getStringExtra("token");
        username = getIntent().getStringExtra("username");

        QUERY_URL = QUERY_URL + "/" + username;

        // Edit text para ingresar el usuario
        nombreEditText = (EditText) findViewById(R.id.perfil_nombre);

        // Edit text para ingresar el mail
        mailEditText = (EditText) findViewById(R.id.perfil_mail);

        // Edit para el path de la foto
        fotoEditText = (EditText) findViewById(R.id.perfil_foto);

        // Boton para iniciar sesion
        actualizarButton = (Button) findViewById(R.id.actualizar);
        actualizarButton.setOnClickListener(this);


        recibirPerfil();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_actualizar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.actualizar:
                recibirPerfil();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onClick(View v) {
        String nombre   = nombreEditText.getText().toString();
        String mail     = mailEditText.getText().toString();
        String fotoPath = fotoEditText.getText().toString();
        actualizarPerfil(nombre, mail, fotoPath);
    }

    public void actualizarPerfil(final String nombre, String mail, String fotoPath){

        AsyncHttpClient client = new AsyncHttpClient();

        JSONObject profile = new JSONObject();
        JSONObject ultimaUbicacion = new JSONObject();

        try{
            profile.put("nombre", nombre);
            profile.put("email", mail);
            profile.put("path foto de perfil", fotoPath);

            ultimaUbicacion.put("latitud", 15.0);
            ultimaUbicacion.put("longitud", 16.0);
            profile.put("ultima ubicacion",ultimaUbicacion);

        } catch (JSONException e){

        }

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("profile", profile.toString());

        client.put(QUERY_URL, params, new JsonHttpResponseHandler() {
            //
            @Override
            public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
                Toast.makeText(getApplicationContext(), "Perfil actualizado", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void recibirPerfil(){
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);

        client.get(QUERY_URL, params, new JsonHttpResponseHandler() {

                @Override
                public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
                    try {
                        JSONObject perfilazo = jsonObject.getJSONObject("perfil");
                        nombreUsuario = perfilazo.getString("nombre");
                        email = perfilazo.getString("email");
                        fotopath = perfilazo.getString("path foto de perfil");
                        JSONObject ubicacion = perfilazo.getJSONObject("ultima ubicacion");
                        latitud = ubicacion.getDouble("latitud");
                        longitud = ubicacion.getDouble("longitud");
                    } catch (JSONException e){

                    }
                    nombreEditText.setText(nombreUsuario);
                    mailEditText.setText(email);
                    fotoEditText.setText(fotopath);

                }

                @Override
                public void onFailure ( int statusCode, Header[] headers, Throwable
                throwable, JSONObject error){
                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                }
            }

        );
    }
}
