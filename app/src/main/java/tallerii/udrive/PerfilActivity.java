package tallerii.udrive;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Esta clase se encarga de manejar la Activity que recibe y manda los datos del perfil del usuario
 *
 */
public class PerfilActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback {

    ImageView fotoPerfil;
    Button actualizarButton;
    EditText nombreEditText;
    EditText mailEditText;

    String nombreUsuario = "";
    String email = "";
    double latitud;
    double longitud;

    private String token = "";
    private String username = "";

    private String QUERY_URL;

    private String savedPhotoFilePath;

    MyLocationListener locationListener;

    private CircularProgressView progressView;
    private GoogleMap mMap;

    ScrollView mScrollView;

    private static final int PICKFILE_RESULT_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("PERFIL: ", "Se inicio la PerfilActivity.");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        // Creo el LocationManager y le digo que se puede ubicar usando Internet o el GPS
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        locationListener = new MyLocationListener();

        try{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Log.i("PERFIL: ", "Inicie el GPS.");

        } catch (SecurityException e){
            Log.e("PERFIL:", "Error al iniciar el GPS.");
            Log.e("PERFIL:", e.getMessage());
            Toast.makeText(getApplicationContext(), "No se pudo inicializar el GPS.", Toast.LENGTH_LONG).show();
        }

        try{
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            Log.i("PERFIL: ", "Inicie el GPS a traves de Internet.");

        } catch (SecurityException e){
            Log.e("PERFIL:", "Error al iniciar el GPS a traves de la conexion de Internet.");
            Log.e("PERFIL:", e.getMessage());
            Toast.makeText(getApplicationContext(), "No se pudo inicializar el GPS.", Toast.LENGTH_LONG).show();
        }


        token = getIntent().getStringExtra("token");
        username = getIntent().getStringExtra("username");

        Log.d("PERFIL: ", "Saque del Intent el token: \"" + token + "\".");
        Log.d("PERFIL: ", "Saque del Intent el username: \"" + username + "\".");

        savedPhotoFilePath = getApplicationContext().getFilesDir().getAbsolutePath();

        Log.d("PERFIL: ", "La ubicacion de mi foto de perfil interna es: \"" + savedPhotoFilePath + "\".");

        // Edit text para ingresar el usuario
        nombreEditText = (EditText) findViewById(R.id.perfil_nombre);

        // Edit text para ingresar el mail
        mailEditText = (EditText) findViewById(R.id.perfil_mail);

        // Rueda circular que muestra como se carga o descarga una foto
        progressView = (CircularProgressView) findViewById(R.id.progress_view);
        progressView.setVisibility(View.INVISIBLE);

        // Boton para iniciar sesion
        actualizarButton = (Button) findViewById(R.id.actualizar);
        actualizarButton.setOnClickListener(this);

        fotoPerfil = (ImageView) findViewById(R.id.foto);
        fotoPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MAIN: ", "Voy a visualizar la foto de perfil.");

                // Cuando la descarga es una foto o un archivo de texto, cambio a otra
                // activity para ver el archivo
                Intent intentDisplay = new Intent(PerfilActivity.this, DisplayActivity.class);

                intentDisplay.putExtra("filePath", savedPhotoFilePath);
                intentDisplay.putExtra("editable", true);

                Log.d("MAIN: ", "Le agrego al Intent el bool editable: \"" + true + "\".");
                Log.d("MAIN: ", "Le agrego al Intent el path a la foto guardada: \"" + savedPhotoFilePath + "\".");

                startActivityForResult(intentDisplay, PICKFILE_RESULT_CODE);
            }
        });

        recibirPerfil();

        // Tengo que crear un MapFragment propio, para poder hacer scroll adentro del mapa
        // teniendo el MapFragment adentro de un ScrollView
        mMap = ((CustomMapFragment) getFragmentManager().findFragmentById(R.id.map_fragment)).getMap();
        mScrollView = (ScrollView) findViewById(R.id.scrollView);

        ((CustomMapFragment) getFragmentManager().findFragmentById(R.id.map_fragment))
                .setListener(new CustomMapFragment.OnTouchListener() {
            @Override
            public void onTouch() {
                mScrollView.requestDisallowInterceptTouchEvent(true);
            }
        });

        Log.i("PERFIL: ", "Termine de obtener el mapa.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i("PERFIL: ", "La activity anterior me dio un resultado.");
        Log.d("PERFIL: ", "El request code es: \"" + requestCode + "\".");

        switch(requestCode){
            case PICKFILE_RESULT_CODE:
                if(resultCode==RESULT_OK){

                    savedPhotoFilePath = data.getStringExtra("pathFotoNueva");

                    Log.i("PERFIL: ", "El resultado de la activity es OK.");

                    Log.d("PERFIL: ", "El path a la nueva foto es: \"" + savedPhotoFilePath + "\".");

                    Log.i("PERFIL: ", "Muestro la nueva foto.");

                    Bitmap bitmap = BitmapFactory.decodeFile(savedPhotoFilePath);
                    fotoPerfil.setImageBitmap(bitmap);
                }
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.i("PERFIL: ", "Ubico el mapa.");

        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(latitud, longitud);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Ultima Ubicación"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
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
                Log.i("PERFIL: ", "Hice click en el boton de actualizar de la barra superior.");
                recibirPerfil();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onClick(View v) {
        String nombre = nombreEditText.getText().toString();
        String mail = mailEditText.getText().toString();

        Log.i("PERFIL: ", "Se hizo click en el boton para actualizar el perfil.");

        Log.d("PERFIL: ", "El nombre ingresado es: \"" + nombre + "\".");
        Log.d("PERFIL: ", "El mail ingresado es: \"" + mail + "\".");

        if (nombre.isEmpty()) {
            Log.d("PERFIL: ", "Se ingreso un nombre vacio.");

            Toast.makeText(getApplicationContext(), "No puede ingresar un nombre vacio",
                    Toast.LENGTH_LONG).show();
        } else if (mail.isEmpty()) {
            Log.d("PERFIL: ", "Se ingreso un mail vacio.");

            Toast.makeText(getApplicationContext(), "No puede ingresar un mail vacio.",
                    Toast.LENGTH_LONG).show();
        } else {
            actualizarPerfil(nombre, mail);
        }
    }

    /**
     * Se encarga de mandar los datos actualizados del perfil del usuario, al servidor.
     * @param nombre nombre actualizado del usuario
     * @param mail mail actualizado del usuario
     */
    public void actualizarPerfil(final String nombre, String mail){

        Log.i("PERFIL: ", "Voy mandar el perfil actualizado.");

        QUERY_URL = MyDataArrays.direccion + "/profile/" + username;

        Log.d("PERFIL: ", "Le pego a la URL: " + QUERY_URL);

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("nombre", nombre);
        params.put("email", mail);

        Log.d("PERFIL: ", "Voy a ingresar los siguientes datos en los parametros del PUT: ");
        Log.d("PERFIL: ", "El nombre del usuario ingresado es: \"" + nombre + "\".");
        Log.d("PERFIL: ", "El token ingresado es: \"" + token + "\".");
        Log.d("PERFIL: ", "El mail ingresados es: \"" + mail + "\".");

        try{
            Log.d("PERFIL: ", "El path a la foto es: \"" + savedPhotoFilePath + "\".");
            if(savedPhotoFilePath.lastIndexOf("/") > 0 ){
                File archivo = new File(savedPhotoFilePath);
                params.put("picture", archivo);
            }
        } catch (FileNotFoundException e){
            Log.e("PERFIL: ", "No pude abrir la foto de perfil a subir.");
            Log.e("PERFIL: ", e.getMessage());
            e.printStackTrace();

            Toast.makeText(getApplicationContext(), "No se pudo encontrar el archivo de la foto de perfil", Toast.LENGTH_LONG).show();
        }

        // Creo un spinner circular para mostrar que la foto se esta subiendo
        progressView.setVisibility(View.VISIBLE);
        progressView.startAnimation();

        client.put(QUERY_URL, params, new JsonHttpResponseHandler() {
            //
            @Override
            public void onSuccess(int statusCode, JSONObject jsonObject) {

                recibirPerfil();

                Log.i("PERFIL: ", "Pude actualizar el perfil.");
                Log.d("PERFIL: ", "StatusCode: \"" + statusCode + "\".");

                // Una vez que se subio la foto freno el spinner
                progressView.setVisibility(View.INVISIBLE);

                Toast.makeText(getApplicationContext(), "Perfil actualizado", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                Log.d("PERFIL: ", "StatusCode: \"" + statusCode + "\".");
                progressView.setVisibility(View.INVISIBLE);

                if (error == null) {
                    Log.e("PERFIL: ", "No se pudo comunicar con el servidor.");

                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        Log.e("PERFIL: ", "Hubieron un problema al mandar los datos nuevos del perfil.");
                        Log.e("PERFIL: ", error.getString("error"));

                        Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        Log.e("PERFIL: ", e.getMessage());
                        Log.e("PERFIL: ", "No se pudo obtener el mensaje de error del JSON.");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Hace un request al servidor, solicitando los datos del perfil del usuario.
     * Al obtener los datos se encarga de mostrarlos correctamente.
     */
    public void recibirPerfil(){

        Log.i("PERFIL: ", "Voy a recibir el perfil.");

        QUERY_URL = MyDataArrays.direccion + "/profile/" + username;

        Log.d("PERFIL: ", "Le pego a la URL: " + QUERY_URL);

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);

        Log.d("PERFIL: ", "Voy a ingresar los siguientes datos en los parametros del GET: ");
        Log.d("PERFIL: ", "El username ingresado es: \"" + username + "\".");
        Log.d("PERFIL: ", "El token ingresado es: \"" + token + "\".");

        client.get(QUERY_URL, params, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, JSONObject jsonObject) {

                        Log.i("PERFIL: ", "Obtuve el perfil desde el servidor.");
                        Log.d("PERFIL: ", "StatusCode: \"" + statusCode + "\".");

                        String fotopath = "";

                        try {

                            JSONObject perfil = jsonObject.getJSONObject("perfil");
                            Log.d("PERFIL: ", "El perfil en formato JSON: \"" + perfil.toString() + "\".");

                            nombreUsuario = perfil.getString("nombre");
                            nombreEditText.setText(nombreUsuario);
                            Log.d("PERFIL: ", "El nombre del usuario obtenido es: \"" + nombreUsuario + "\".");

                            email = perfil.getString("email");
                            mailEditText.setText(email);
                            Log.d("PERFIL: ", "El mail obtenido es: \"" + email + "\".");

                            fotopath = perfil.getString("path foto de perfil");
                            Log.d("PERFIL: ", "El path a la foto de perfil obtenido es: \"" + fotopath + "\".");

                            int index = fotopath.lastIndexOf("/");
                            String nombreFoto = fotopath.substring(index + 1);

                            savedPhotoFilePath = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + nombreFoto;
                            Log.d("PERFIL: ", "Nuevo path para la foto de perfil guardada: \"" + savedPhotoFilePath + "\".");


                            JSONObject ubicacion = perfil.getJSONObject("ultima ubicacion");
                            Log.d("PERFIL: ", "La ultima ubicacion en formato JSON: \"" + ubicacion.toString() + "\".");

                            latitud = ubicacion.getDouble("latitud");
                            longitud = ubicacion.getDouble("longitud");
                            Log.d("PERFIL: ", "Latitud: \"" + latitud + "\" || Longitud: \"" + longitud + "\".");

                        } catch (JSONException e) {
                            Log.e("PERFIL: ", "Error al obtener los datos del JSON, al recibir el perfil.");
                            Log.e("PERFIL: ", e.getMessage());
                            e.printStackTrace();
                        }

                        // TODO: ESTO ES XQ TARDA EN UBICARSE, VER COMO SOLUCIONAR
                        boolean ubicado = false;
                        while (!ubicado) {
                            ubicado = obtenerDireccion(latitud, longitud);
                            if ((latitud == 0) || (longitud == 0)) {
                                ubicado = true;
                            }
                        }

                        obtenerFotoPerfil(fotopath);

                    }

                    @Override
                    public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                        Log.d("PERFIL: ", "StatusCode: \"" + statusCode + "\".");

                        if (error == null) {
                            Log.e("PERFIL: ", "No se pudo comunicar con el servidor.");

                            Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                        } else {
                            try {
                                Log.e("PERFIL: ", "Hubieron un problema en la recepción de la foto de perfil.");
                                Log.e("PERFIL: ", error.getString("error"));

                                Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Log.e("PERFIL: ", e.getMessage());
                                Log.e("PERFIL: ", "No se pudo obtener el mensaje de error del JSON.");
                                e.printStackTrace();
                            }
                        }
                    }
                }

        );
    }

    /**
     * A partir del fotoPath, descarga desde el servidor la foto de perfil del usuario a mostrar
     * @param fotopath path de la foto de perfil del usuario, dentro del servidor
     */
    private void obtenerFotoPerfil(String fotopath){

        Log.i("PERFIL: ", "Voy a descargar la foto de perfil.");

        QUERY_URL = MyDataArrays.direccion + "/profile/" + fotopath ;

        Log.d("PERFIL: ", "Le pego a la URL: \"" + QUERY_URL + "\".");

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);

        Log.d("PERFIL: ", "Inclui en los parametros del GET los siguientes parametros.");
        Log.d("PERFIL: ", "Username: \"" + username + "\" con el token: \"" + token + "\".");

        Log.d("PERFIL: ", "Guardo la foto en: \"" + savedPhotoFilePath + "\".");

        File file=new File(savedPhotoFilePath);

        // Creo un spinner circular para mostrar que la foto se esta subiendo
        progressView.setVisibility(View.VISIBLE);
        progressView.startAnimation();

        client.get(QUERY_URL, params, new FileAsyncHttpResponseHandler(file) {

                    @Override
                    public void onSuccess(File file) {
                        Log.i("PERFIL: ", "Recibi la foto.");

                        progressView.setVisibility(View.INVISIBLE);

                        Bitmap bitmap = BitmapFactory.decodeFile(savedPhotoFilePath);
                        fotoPerfil.setImageBitmap(bitmap);

                        Log.i("PERFIL: ", "Mostre la foto.");

                    }

                    @Override
                    public void onFailure(int statusCode, Throwable e, File response) {

                        progressView.setVisibility(View.INVISIBLE);

                        Log.d("PERFIL: ", "StatusCode: \"" + statusCode + "\".");
                        Log.d("PERFIL: ", "No se pudo descargar la foto de perfil.");
                        Toast.makeText(getApplicationContext(), "Error al descargar la foto", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /**
     * A partir de las siguientes coordenadas obtiene una direccion de una calle, en un barrio y una ciudad
     * TODO: El formato de la direccion esta en INGLES, pero no pude encontrar el formato en español
     * @param latitud las coordenadas de latitud de la ubicacion
     * @param longitud las coordenadas de longitud de la ubicacion
     * @return devuelve verdadero o falso si pudo encontrar la direccion o no
     */
    public boolean obtenerDireccion(double latitud, double longitud){

        Log.i("PERFIL: ", "Voy a obtener la direccion a partir de las coordenadas.");
        Log.d("PERFIL: ", "Latitud: \"" + latitud + "\" || Longitud: \"" + longitud + "\".");

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

                Log.d("PERFIL: ", "Obtuve la siguiente ubicacion a partir de las coordenadas: \""
                        + strAddress.toString() + "\".");
                actualizarMapa(strAddress.toString());
                return true;

            } else{
                Log.d("PERFIL: ", "No pude obtener ninguna ubicacion a partir de las coordenadas.");
                return false;
            }


        }
        catch (IOException e) {
            Log.e("PERFIL: ", "Error al obtener la ubicacion a partir de las coordenadas.");
            Log.e("PERFIL: ", e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Actualiza el mapa, ubicando un marcador en las coordenadas de latitud y longitud correspondientes,
     * colocando la direccion como titulo en el marcador.
     * Luego hace una pequeña animacion de acercamiento de la camara hacia el marcador.
     * @param direccion titulo del marcador a colocar.
     */
    private void actualizarMapa(String direccion){

        Log.e("PERFIL: ", "Actualizo la ubicacion en el mapa.");

        // Agrego el marcador con la posicion y muevo la camara
        LatLng marker = new LatLng(latitud, longitud);
        mMap.addMarker(new MarkerOptions().position(marker).title(direccion));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(marker));

        CameraPosition cameraPosition = CameraPosition.builder()
                .target(marker)
                .zoom(13)
                .bearing(90)
                .build();

        // Anima la camara acercandola por 3 segundos
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 3000, null);
    }

}
