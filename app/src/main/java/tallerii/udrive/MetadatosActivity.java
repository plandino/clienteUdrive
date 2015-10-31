package tallerii.udrive;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Esta clase maneja la Activity donde se muestran y modifican los metadatos.
 */
public class MetadatosActivity extends AppCompatActivity implements View.OnClickListener {

    Button   actualizarButton;

    EditText nombreEditText;
    EditText etiquetasEditText;

    TextView usuariosCompartidosTextView;
    TextView fechaTextView;
    TextView usuarioUltModificacionTextView;
    TextView propietarioTextView;

    private String token = "";
    private String username = "";

    private String propietario = "";
    private JSONArray usrCompartidosJSONArray;

    private String QUERY_URL = MyDataArrays.direccion + "/metadata/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("METADATOS: ", "Se inicio la MetadatosActivity.");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadatos);

        token = getIntent().getStringExtra("token");
        username = getIntent().getStringExtra("username");
        String pathArchivo = getIntent().getStringExtra("pathArchivo");

        Log.d("METADATOS: ", "Saque del Intent el token: \"" + token + "\".");
        Log.d("METADATOS: ", "Saque del Intent el username: \"" + username + "\".");
        Log.d("METADATOS: ", "Saque del Intent el pathArchivo: \"" + pathArchivo + "\".");

        QUERY_URL = QUERY_URL + username  + pathArchivo;

        Log.d("METADATOS: ", "Forme el URL: \"" + QUERY_URL + "\".");

        nombreEditText                  = (EditText) findViewById(R.id.metadatos_nombre);
        etiquetasEditText               = (EditText) findViewById(R.id.metadatos_etiquetas);
        usuariosCompartidosTextView     = (TextView) findViewById(R.id.metadatos_usrCompartidos);

        fechaTextView                   = (TextView) findViewById(R.id.metadatos_fecha);
        usuarioUltModificacionTextView  = (TextView) findViewById(R.id.metadatos_usrUltimaModificacion);
        propietarioTextView             = (TextView) findViewById(R.id.metadatos_propietario);

        // Boton para iniciar sesion
        actualizarButton = (Button) findViewById(R.id.actualizar);
        actualizarButton.setOnClickListener(this);

        recibirMetadatos();
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
                Log.i("METADATOS: ", "Hice click en el boton de actualizar de la barra superior.");
                recibirMetadatos();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /**
     * Se encarga de hacer un GET al servidor para obtener los metadatos del archivo.
     */
    public void recibirMetadatos(){

        Log.i("METADATOS: ", "Voy a recibir los metadatos.");

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);

        Log.d("METADATOS: ", "Inclui en los parametros del GET los siguientes parametros.");
        Log.d("METADATOS: ", "Username: \"" + username + "\" con el token: \"" + token + "\".");

        client.get(QUERY_URL, params, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {

                        Log.i("METADATOS: ", "Recibi los metadatos.");

                        try {

                            JSONObject metadatos = jsonObject.getJSONObject("metadatos");
                            Log.d("METADATOS: ", "Metadatos en formato JSON: \"" + metadatos.toString() + "\".");


                            String nombre = metadatos.getString("nombre");
                            String extension = metadatos.getString("extension");
                            String nombreArchivo;

                            // Si hay extension tengo que agregar el punto y la extension
                            // sino no agrego
                            if( !extension.isEmpty()){
                                nombreArchivo = nombre + "." + extension;
                            } else {
                                nombreArchivo = nombre;
                            }
                            Log.d("METADATOS: ", "Nombre del archivo: \"" + nombreArchivo + "\".");
                            nombreEditText.setText(nombreArchivo);

                            String fechaUltimaModificacion = metadatos.getString("fecha ultima modificacion");
                            Log.d("METADATOS: ", "Fecha ultima modificacion: \"" + fechaUltimaModificacion + "\".");
                            fechaTextView.setText(fechaUltimaModificacion);

                            String userUltimaModificacion = metadatos.getString("usuario ultima modificacion");
                            Log.d("METADATOS: ", "Usuario que hizo la ultima modificacion: \"" + userUltimaModificacion + "\".");
                            usuarioUltModificacionTextView.setText(userUltimaModificacion);

                            propietario = metadatos.getString("propietario");
                            Log.d("METADATOS: ", "Propietario: \"" + propietario + "\".");
                            propietarioTextView.setText(propietario);

                            // Obtengo las etiquetas
                            JSONArray etiquetas = metadatos.getJSONArray("etiquetas");
                            Log.d("METADATOS: ", "Etiquetas en formato JSON: \"" + etiquetas.toString() + "\".");

                            // Voy a concatenar las etiquetas para poder mostrarlas
                            // Uso MyDataArrays.divisor para dividir las etiquetas
                            int cantidadEtiquetas = etiquetas.length();
                            String etiquetasString = "";
                            for (int i = 0; i < cantidadEtiquetas; i++) {

                                // Si estoy en el ultimo item no agego el divisor
                                if (i == (cantidadEtiquetas - 1)) {
                                    etiquetasString = etiquetasString + etiquetas.getString(i);
                                } else {
                                    etiquetasString = etiquetasString + etiquetas.getString(i) + MyDataArrays.divisor;
                                }
                            }

                            Log.d("METADATOS: ", "Muestro las etiquetas: \"" + etiquetasString + "\".");
                            etiquetasEditText.setText(etiquetasString);

                            // Voy a concatenar los usuarios compartidos para poder mostrarlos
                            // Uso MyDataArrays.divisor para dividir los usuarios
                            usrCompartidosJSONArray = metadatos.getJSONArray("usuarios");
                            Log.d("METADATOS: ", "Usuarios compartidos en formato JSON: \"" + usrCompartidosJSONArray.toString() + "\".");

                            int cantidadUsr = usrCompartidosJSONArray.length();
                            String usuariosString = "";
                            for (int i = 0; i < cantidadUsr; i++) {

                                // Si estoy en el ultimo item no agego el divisor
                                if (i == (cantidadUsr - 1)) {
                                    usuariosString = usuariosString + usrCompartidosJSONArray.getString(i);
                                } else {
                                    usuariosString = usuariosString + usrCompartidosJSONArray.getString(i) + MyDataArrays.divisor;
                                }
                            }

                            Log.d("METADATOS: ", "Muestro los usuarios compartidos: \"" + usuariosString + "\".");
                            usuariosCompartidosTextView.setText(usuariosString);

                        } catch (JSONException e) {
                            Log.e("METADATOS: ", "Hubo un error al obtener algun valor de los metadatos.");
                            Log.e("METADATOS: ", e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {

                        Log.d("METADATOS: ", "StatusCode: \"" + statusCode + "\".");

                        if(error == null){
                            Log.e("METADATOS: ", "No se pudo comunicar con el servidor.");

                            Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                        } else {
                            try {
                                Log.d("METADATOS: ", "Hubo un problema en la recepción de los metadatos.");
                                Log.d("METADATOS: ", error.getString("error"));

                                Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Log.e("METADATOS: ", e.getMessage());
                                Log.e("METADATOS: ", "No se pudo obtener el mensaje de error del JSON.");
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );
    }

    @Override
    public void onClick(View v) {
        String nombre           = nombreEditText.getText().toString();
        String etiquetas        = etiquetasEditText.getText().toString();

        Log.d("METADATOS: ", "El nombre ingresado es: \"" + nombre + "\".");
        Log.d("METADATOS: ", "Las etiquetas ingresadas son: \"" + etiquetas + "\".");

        if( !nombre.isEmpty()){

            Log.i("METADATOS: ", "Se ingreso un nombre en el campo.");
            mandarMetadatos(nombre, etiquetas);
        } else {
            Log.d("METADATOS: ", "El campo para el nombre esta vacio.");
            Toast.makeText(getApplicationContext(), "Por favor complete el campo con el nombre", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Se encarga de mandar los metadatos actualizados al servidor.
     * Manda un PUT, poniendo un JSON en un parametro, con los datos adentro.
     * @param nombreArchivo es el nombre del archivo a enviar con los metadatos
     * @param etiquetas son las etiquetas a enviar con los metadatos
     */
    public void mandarMetadatos(final String nombreArchivo, String etiquetas){

        Log.i("METADATOS: ", "Voy a mandar los metadatos.");

        AsyncHttpClient client = new AsyncHttpClient();

        JSONObject jsonMetadatos = new JSONObject();

        try{

            // Divido el nombre del archivo en nombre y extension para poder mandarlos
            int index = nombreArchivo.lastIndexOf(".");
            String nombre;
            String extension = "";

            // Si el index es menor a 0, significa que no hay ningun punto en el archivo
            if(index > 0){
                nombre = nombreArchivo.substring(0, index);
                extension = nombreArchivo.substring(index + 1);
            } else {
                nombre = nombreArchivo;
            }

            jsonMetadatos.put("nombre", nombre);
            Log.d("METADATOS: ", "Ingrese al JSON el nombre: " + nombre);

            jsonMetadatos.put("extension", extension);
            Log.d("METADATOS: ", "Ingrese al JSON la extension: " + extension);

            jsonMetadatos.put("propietario", propietario);
            Log.d("METADATOS: ", "Ingrese al JSON el propietario: " + propietario);

            String timeStamp = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());       //"mm.ss.dd.MM.yyyyyyyy").format(new Date());

            jsonMetadatos.put("fecha ultima modificacion", timeStamp);
            Log.d("METADATOS: ", "Ingrese al JSON la fecha: " + timeStamp);

            jsonMetadatos.put("usuario ultima modificacion", username);
            Log.d("METADATOS: ", "Ingrese al JSON el username: " + username);

            JSONArray etiquetasJSONArray =  new JSONArray();

            // Voy a concatenar las etiquetas separadas por el divisor y las pongo en un JSONArray
            String[] partesEtiquetas = etiquetas.split( String.valueOf(MyDataArrays.divisor) );

            for(String etiqueta : partesEtiquetas){
                etiqueta = etiqueta.trim();
                if( !etiqueta.isEmpty()) {
                    etiquetasJSONArray.put(etiqueta);
                }
            }

            jsonMetadatos.put("etiquetas", etiquetasJSONArray);
            Log.d("METADATOS: ", "Ingrese al JSON las etiquetas en formato JSONArray: " + etiquetasJSONArray.toString());

            jsonMetadatos.put("usuarios", usrCompartidosJSONArray);
            Log.d("METADATOS: ", "Ingrese al JSON los usarios compartidos en formato JSONArray: " + usrCompartidosJSONArray.toString());

        } catch (JSONException e){
            Log.e("METADATOS: ", "Hubo problemas al ingresar los metadatos en el JSON.");
            Log.e("METADATOS: ", e.getMessage());
            e.printStackTrace();
        }

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        params.put("metadatos", jsonMetadatos.toString());

        Log.d("METADATOS: ", "Le pego a la URL: " + QUERY_URL);

        Log.d("METADATOS: ", "Voy a ingresar los siguientes datos en los parametros de la request: ");
        Log.d("METADATOS: ", "El usuario ingresado es: \"" + username + "\".");
        Log.d("METADATOS: ", "El token ingresado es: \"" + token + "\".");
        Log.d("METADATOS: ", "Los metadatos ingresados en formato JSON son: \"" + jsonMetadatos.toString() + "\".");

        client.put(QUERY_URL, params, new JsonHttpResponseHandler() {
            //
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject jsonObject) {
                Toast.makeText(getApplicationContext(), "Archivo actualizado", Toast.LENGTH_LONG).show();

                Log.i("METADATOS: ", "Archivo actualizado.");
                Log.i("METADATOS: ", "StatusCode: \"" + statusCode + "\".");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {

                Log.d("METADATOS: ", "StatusCode: \"" + statusCode + "\".");

                if(error == null){
                    Log.e("METADATOS: ", "No se pudo comunicar con el servidor.");

                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        Log.d("METADATOS: ", "Hubo un problema en el envio de los metadatos.");
                        Log.d("METADATOS: ", error.getString("error"));

                        Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        Log.e("METADATOS: ", e.getMessage());
                        Log.e("METADATOS: ", "No se pudo obtener el mensaje de error del JSON.");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {

        Log.d("METADATOS: ", "Aprete para atras.");

        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}