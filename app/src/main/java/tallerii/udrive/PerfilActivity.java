package tallerii.udrive;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by panchoubuntu on 29/09/15.
 */
public class PerfilActivity extends AppCompatActivity implements View.OnClickListener {

    Button actualizarButton;
    EditText nombreEditText;
    EditText mailEditText;
    EditText fotoEditText;
    EditText ubicacionEditText;

    String nombreUsuario = "";
    String email = "";
    String fotopath = "";
    double latitud = 1.0;
    double longitud = 2.0;

    private String token = "";
    private String username = "";

    private String QUERY_URL = MyDataArrays.direccion + "/profile";

    MyLocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        // Creo el LocationManager y le digo que se puede ubicar usando Internet o el GPS
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        locationListener = new MyLocationListener();

        try{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException e){
            Log.e("GPS ACCESS PROBLEM:", e.getMessage());
        }
        try{
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException e){
            Log.e("NETWORK ACCESS PROBLEM:", e.getMessage());
        }


        token = getIntent().getStringExtra("token");
        username = getIntent().getStringExtra("username");

        QUERY_URL = QUERY_URL + "/" + username;

        // Edit text para ingresar el usuario
        nombreEditText = (EditText) findViewById(R.id.perfil_nombre);

        // Edit text para ingresar el mail
        mailEditText = (EditText) findViewById(R.id.perfil_mail);

        // Edit para el path de la foto
        fotoEditText = (EditText) findViewById(R.id.perfil_foto);

        // Edit para la ubicacion
        ubicacionEditText = (EditText) findViewById(R.id.perfil_ubicacion);


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

        double latitud = locationListener.getLatitud();
        double longitud = locationListener.getLongitud();

        try{
            profile.put("nombre", nombre);
            profile.put("email", mail);
            profile.put("path foto de perfil", fotoPath);

            ultimaUbicacion.put("latitud", latitud);
            ultimaUbicacion.put("longitud", longitud);
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

                        } catch (JSONException e) {

                        }
                        nombreEditText.setText(nombreUsuario);
                        mailEditText.setText(email);
                        fotoEditText.setText(fotopath);
                        obtenerDireccion(latitud, longitud);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable
                            throwable, JSONObject error) {
                        Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                    }
                }

        );
    }

    public void obtenerDireccion(double latitud, double longitud){

        Geocoder geocoder= new Geocoder(this, Locale.ENGLISH);
        try {

            List<Address> addresses = geocoder.getFromLocation(latitud, longitud, 1);

            if( (addresses != null) && (addresses.size() != 0)){

                Address fetchedAddress = addresses.get(0);
                StringBuilder strAddress = new StringBuilder();

                for(int i=0; i<fetchedAddress.getMaxAddressLineIndex(); i++) {

                    strAddress.append(fetchedAddress.getAddressLine(i));

                    if(i < fetchedAddress.getMaxAddressLineIndex() - 1){
                        strAddress.append("\n");
                    }
                }

                ubicacionEditText.setText(strAddress.toString());

            }

            else
                ubicacionEditText.setText("Ubicacion fallida");

        }
        catch (IOException e) {
            Log.w("UBICACION: ", e.getMessage());
            e.printStackTrace();
        }
    }


}
