package tallerii.udrive;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
    private String pathArchivo = "";

    private String propietario = "";

    private String QUERY_URL = MyDataArrays.direccion + "/metadata/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadatos);

        token = getIntent().getStringExtra("token");
        username = getIntent().getStringExtra("username");
        pathArchivo = getIntent().getStringExtra("pathArchivo");

        QUERY_URL = QUERY_URL + username  + pathArchivo;

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
                recibirMetadatos();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onClick(View v) {
        String nombre           = nombreEditText.getText().toString();
        String etiquetas        = etiquetasEditText.getText().toString();
        String usrCompartidos   = usuariosCompartidosTextView.getText().toString();

        mandarMetadatos(nombre, etiquetas, usrCompartidos);
    }

    public void mandarMetadatos(final String nombreArchivo, String etiquetas, String usrCompartidos){

        AsyncHttpClient client = new AsyncHttpClient();

        JSONObject jsonMetadatos = new JSONObject();
        String timeStamp = "";
        int index;
        String nombre = "";
        String extension = "";
        JSONArray etiquetasJSONArray =  new JSONArray();
        JSONArray usuariosCompartidosJSONArray = new JSONArray();

        try{
            index = nombreArchivo.lastIndexOf(".");
            nombre = nombreArchivo.substring(0, index);
            extension = nombreArchivo.substring(index + 1);

            jsonMetadatos.put("nombre", nombre);
            jsonMetadatos.put("extension", extension);
            jsonMetadatos.put("propietario", propietario);
            timeStamp = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());       //"mm.ss.dd.MM.yyyyyyyy").format(new Date());

            jsonMetadatos.put("fecha ultima modificacion", timeStamp);
            jsonMetadatos.put("usuario ultima modificacion", username);


            String[] partesEtiquetas = etiquetas.split(";");

            for(String etiqueta : partesEtiquetas){
                etiquetasJSONArray.put(etiqueta);
            }
            jsonMetadatos.put("etiquetas", etiquetasJSONArray);


            String[] partesUsuarios = usrCompartidos.split(";");

            for(String usuario : partesUsuarios){
                usuario = usuario.trim();
                usuariosCompartidosJSONArray.put(usuario);
            }
            jsonMetadatos.put("usuarios", usuariosCompartidosJSONArray);

        } catch (JSONException e){
        }

//        Toast.makeText(getApplicationContext(), "tiempo: " + timeStamp, Toast.LENGTH_LONG).show();
//        Toast.makeText(getApplicationContext(), "usuarios: " + usuariosCompartidosJSONArray.toString(), Toast.LENGTH_LONG).show();
//        Toast.makeText(getApplicationContext(), "etiquetas: " + etiquetasJSONArray.toString(), Toast.LENGTH_LONG).show();

        Toast.makeText(getApplicationContext(), jsonMetadatos.toString(), Toast.LENGTH_LONG).show();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        params.put("metadatos", jsonMetadatos.toString());

        client.put(QUERY_URL, params, new JsonHttpResponseHandler() {
            //
            @Override
            public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
                Toast.makeText(getApplicationContext(), "Archivo actualizado", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void recibirMetadatos(){

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        Toast.makeText(getApplicationContext(), "Mando: " + QUERY_URL, Toast.LENGTH_LONG).show();


        client.get(QUERY_URL, params, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
//                        Toast.makeText(getApplicationContext(), "Recibi metadatos\n" + jsonObject.toString(), Toast.LENGTH_LONG).show();

                        try {

                            JSONObject metadatos = jsonObject.getJSONObject("metadatos");
                            nombreEditText.setText(metadatos.getString("nombre") + "." + metadatos.getString("extension"));
                            fechaTextView.setText(metadatos.getString("fecha ultima modificacion"));
                            usuarioUltModificacionTextView.setText(metadatos.getString("usuario ultima modificacion"));
                            propietario = metadatos.getString("propietario");
                            propietarioTextView.setText(propietario);

                            // Obtengo las etiquetas
                            JSONArray etiquetas = metadatos.getJSONArray("etiquetas");
                            int cantidadEtiquetas = etiquetas.length();
                            String etiquetasString = "";
                            for(int i = 0; i < cantidadEtiquetas; i++){

                                // Si estoy en el ultimo item no agego el ";"
                                if(i == (cantidadEtiquetas - 1)) {
                                    etiquetasString = etiquetasString + etiquetas.getString(i);
                                } else {
                                    etiquetasString = etiquetasString + etiquetas.getString(i) + "; " ;
                                }
                            }
                            etiquetasEditText.setText(etiquetasString);

                            // Obtengo los usuarios con permisos para este archivo
                            JSONArray usrCompartidos = metadatos.getJSONArray("usuarios");
                            int cantidadUsr = usrCompartidos.length();
                            String usuariosString = "";
                            for(int i = 0; i < cantidadUsr; i++){

                                // Si estoy en el ultimo item no agego el ";"
                                if(i == (cantidadUsr - 1)) {
                                    usuariosString = usuariosString + usrCompartidos.getString(i);
                                } else {
                                    usuariosString = usuariosString + usrCompartidos.getString(i) + "; ";
                                }
                            }
                            usuariosCompartidosTextView.setText(usuariosString);

                        } catch (JSONException e){

                        }
                    }

                    @Override
                    public void onFailure ( int statusCode, Header[] headers, Throwable throwable, JSONObject error){
                        Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                    }
                }

        );
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}

