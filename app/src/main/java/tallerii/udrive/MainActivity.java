package tallerii.udrive;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements FilesFragment.OnFragmentInteractionListener{

    FragmentManager fragmentManager;

    private String token = "";
    private String username = "";

    private String QUERY_URL = MyDataArrays.direccion + "/profile";

    private String QUERY_URL_CARPETAS = MyDataArrays.direccion + "/folder/";
    private String QUERY_URL_METADATOS;

    private String PATH_ACTUAL = "/";

    private String estructuraCarpetas;
    private JSONObject estructuraCarpetasJSON;
    private JSONObject usuariosParaCompartir;

    HashMap<String, String> hashTipoArchivos;
    HashMap<String, String> URLArchivos;

    private ArrayAdapter<String> arrayAdapter;
    private TextView textoFallido;


    private static final int METADATOS = 2;

    private static final int PICKFILE_RESULT_CODE = 1;

    MyLocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        token = intent.getStringExtra("token");
        username = intent.getStringExtra("username");
        usuariosParaCompartir = new JSONObject();

        QUERY_URL_CARPETAS = QUERY_URL_CARPETAS + username + "/";

        fragmentManager = getFragmentManager();

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

        get(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.subir_archivo:
                subirArchivo();
                return true;
            case R.id.crear_carpeta:
                crearCarpeta();
                return true;
            case R.id.carpetas_compartidas:
                get(MyDataArrays.caracterReservado + "permisos");
                return true;
            case R.id.papelera:
                get(MyDataArrays.caracterReservado + "trash");
                return true;
//            case R.id.buscar_archivo:
//                return true;
            case R.id.ver_perfil:
                pasarAVerPerfil();
                return true;
            case R.id.cerrar_sesion:
                cerrarSesion();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        switch(requestCode){
            case PICKFILE_RESULT_CODE:
                if(resultCode==RESULT_OK){

                    if (null == data) return;

                    String selectedImagePath;
                    Uri selectedImageUri = data.getData();

                    // Tengo que obtener el path del archivo. Uso el FilePathGetter para traducir
                    // de una Uri a un file path absoluto
                    selectedImagePath = FilePathGetter.getPath(getApplicationContext(), selectedImageUri);
                    Log.i("Image File Path", "" + selectedImagePath);

                    subir(selectedImagePath);
                    Toast.makeText(getApplicationContext(), "Path: " + selectedImagePath, Toast.LENGTH_LONG).show();
                }
                break;
            case METADATOS:
                if(resultCode==RESULT_OK){
                    get(null);
                }

        }
    }

    private void get(String id) {

        if(id != null){
            if(id.equals(MyDataArrays.caracterReservado + "permisos")){
                QUERY_URL_CARPETAS =  MyDataArrays.direccion + "/folder/"  + id + "/" + username ;
                Toast.makeText(getApplicationContext(), QUERY_URL_CARPETAS, Toast.LENGTH_LONG).show();
                PATH_ACTUAL = "/" + id + "/";

            } else if(id.equals(MyDataArrays.caracterReservado + "trash")){
                QUERY_URL_CARPETAS =  MyDataArrays.direccion + "/folder/" + username + "/" + id + "/";
                PATH_ACTUAL = "/" + id + "/";
            } else {
                QUERY_URL_CARPETAS = QUERY_URL_CARPETAS + id + "/";
                PATH_ACTUAL = PATH_ACTUAL + id + "/";
            }
        }

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);

        client.get(QUERY_URL_CARPETAS, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    estructuraCarpetas = jsonObject.getString("estructura");
                    estructuraCarpetasJSON = jsonObject.getJSONObject("estructura");
                    guardarMapaArchivos(estructuraCarpetasJSON);
                    guardarURLArchivos(estructuraCarpetasJSON);
//                    Toast.makeText(getApplicationContext(), estructuraCarpetas, Toast.LENGTH_LONG).show();


                } catch (JSONException e) {

                }
                crearNuevoFragmento(estructuraCarpetas);
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "No pude acceder a la carpeta.", Toast.LENGTH_LONG).show();
                try {
                    Toast.makeText(getApplicationContext(), statusCode + error.getString("error"), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {

                }
            }
        });
    }

    private void getUsuarios(String id) {

        String QUERY_URL_USUARIOS = MyDataArrays.direccion + "/profile";

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        Toast.makeText(getApplicationContext(), "\"" + id + "\"", Toast.LENGTH_LONG).show();
        params.put("busqueda", id);

        client.get(QUERY_URL_USUARIOS, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
//                    JSONObject busqueda = jsonObject.getJSONObject("busqueda");
                    usuariosParaCompartir = jsonObject.getJSONObject("busqueda");

                        String usuarios = "";
                        try {
                            usuarios = usuariosParaCompartir.getString("usuarios");
                        } catch (JSONException e) {

                        }
                        if (usuarios.length() > 0) {
                            textoFallido.setVisibility(View.INVISIBLE);
                            String[] partes = usuarios.split(Character.toString(MyDataArrays.caracterReservado));
                            for (int i = 0; i < partes.length; i++) {
                                arrayAdapter.add(partes[i]);
                            }
                        } else {
                            textoFallido.setVisibility(View.VISIBLE);
                        }

//                    Toast.makeText(getApplicationContext(), usuariosParaCompartir.toString(), Toast.LENGTH_LONG).show();


                } catch (JSONException e) {

                }
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "No pude obtener los usuarios", Toast.LENGTH_LONG).show();
//                try {
//                    Toast.makeText(getApplicationContext(), statusCode + error.getString("error"), Toast.LENGTH_LONG).show();
//                } catch (JSONException e) {
//
//                }
            }
        });
    }

    @Override
    public void onGroupClick(String id) {
        String tipoDeArchivo = obtenerTipoDeArchivo(id);

        if(tipoDeArchivo.equals(MyDataArrays.caracterReservado + "folder")) {
            get(id);
        } else {
            obtenerMetadatos(id);
        }
    }

    @Override
    public void onDownScroll(){
        get(null);
        Toast.makeText(getApplicationContext(), "Actualizado", Toast.LENGTH_LONG).show();
    }

    @Override
    public void eliminar(){
        Toast.makeText(getApplicationContext(), "elimino", Toast.LENGTH_LONG).show();
    }


    public void crearNuevoFragmento(String jsonObject){
        // Aca recibo desde el fragmento, la carpeta que seleccione y tengo que pedirle al servidor
        // que me devuelva la estructura de carpetas y archivos que estan adentro de esa.
        // Luego creo un nuevo fragmento con esa estructura y reemplazo el anterior

        //Paso 3: Creo un nuevo fragmento
        FilesFragment fragment = new FilesFragment();

        // Creo un Bundle y le agrego informacion del tipo <Key, Value>
        Bundle bundle = new Bundle();
        bundle.putString("estructura", jsonObject);

        // Le agrego el Bundle al fragmento
        fragment.setArguments(bundle);

        //Paso 2: Crear una nueva transacción
        FragmentTransaction transaction2 = fragmentManager.beginTransaction().replace(R.id.contenedor, fragment);

        //Paso 4: Confirmar el cambio
        transaction2.commit();
    }

    @Override
    public void onOptionClick(String id, String opcion) {

        if(opcion.equals("Descargar")){
            descargarArchivo(id);
        }

        if(opcion.equals("Compartir")){
            compartirArchivo(id);
        }

        if(opcion.equals("Eliminar")){
            confirmarEliminarORestaurar(id, false);
        }

        if(opcion.equals("Restaurar")){
            confirmarEliminarORestaurar(id, true);
        }
    }

    private void subir(String path){

        int index = path.lastIndexOf("/");
        String filename = path.substring(index + 1);
        QUERY_URL = MyDataArrays.direccion + "/file" + "/" + username +  PATH_ACTUAL + filename;

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        double latitud = locationListener.getLatitud();
        double longitud = locationListener.getLongitud();
        params.put("latitud", String.valueOf(latitud));
        params.put("longitud", String.valueOf(longitud));
        Toast.makeText(getApplicationContext(), "Longitud: " + longitud + " Latitud: " + latitud, Toast.LENGTH_LONG).show();

        try{
            File archivo = new File(path);
            params.put("file", archivo);
        } catch (FileNotFoundException e){
            Toast.makeText(getApplicationContext(), "No se pudo encontrar el archivo", Toast.LENGTH_LONG).show();
        }


        // URI:
        // username :
        // filename : el path al archivo en el servidor, con la jerarquia de carpetas

        // Parametros:
        // user : el mio, el que pide el request
        // token : mi token

        client.put(QUERY_URL, params, new JsonHttpResponseHandler() {
            //
            @Override
            public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
                Toast.makeText(getApplicationContext(), "Archivo subido", Toast.LENGTH_LONG).show();
                get(null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void subirArchivo(){

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICKFILE_RESULT_CODE);


//            FileDialog fileOpenDialog =  new FileDialog(MainActivity.this, "FileOpen..",
//                    new FileDialog.SimpleFileDialogListener() {
//
//                        @Override
//                        public void onChosenDir(String chosenDir)
//                        {
//                            // Aca tengo que mandar la carpeta
////                            Toast.makeText(getApplicationContext(), "PATH: " + chosenDir, Toast.LENGTH_LONG).show();
//                            subir(chosenDir);
//                        }
//                    }
//            );
//            fileOpenDialog.default_file_name = getApplicationContext().getFilesDir().getAbsolutePath(); //"/data/data/tallerii.udrive/files";
//            fileOpenDialog.chooseFile_or_Dir(fileOpenDialog.default_file_name);
    }

    private void descargarArchivo(final String id){

        QUERY_URL = MyDataArrays.direccion + "/file/" + username + PATH_ACTUAL + id ;
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);

        final String savedFilePath = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + id; // "/data/data/tallerii.udrive/files/" + nombreArchivo;

        File file = new File(savedFilePath);
        client.get(QUERY_URL, params, new FileAsyncHttpResponseHandler(file) {
//                        @Override
//                        public void onProgress(int bytesWritten, int totalSize) {
//                            Toast.makeText(getApplicationContext(), "Descargando...", Toast.LENGTH_LONG).show();
//                        }

                    @Override
                    public void onSuccess(File file) {

                        // Cuando la descarga es exitosa, cambio a otra activity que muestra el archivo
                        Intent intentDisplay = new Intent(MainActivity.this, DisplayActivity.class);

                        intentDisplay.putExtra("extensionArchivo", obtenerTipoDeArchivo(id));
                        intentDisplay.putExtra("filePath", savedFilePath);
                        intentDisplay.putExtra("nombreArchivo", id);
                        startActivity(intentDisplay);
                    }

                    @Override
                    public void onFailure(Throwable e, File response) {
                        Toast.makeText(getApplicationContext(), "No se pudo descargar el archivo", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void crearCarpeta(){
        // Muestro una ventana emergente para que introduzca el nombre de la carpeta a crear
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Crear carpeta");
        alert.setMessage("Introduzca el nombre de la nueva carpeta");
        final EditText nombreCarpeta = new EditText(this);
        alert.setView(nombreCarpeta);

        // El boton "Crear" crea la carpeta
        alert.setPositiveButton("Crear", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {

                agregarCarpeta(nombreCarpeta.getText().toString());
            }
        });

        // Un boton de cancelar, que no hace nada (se cierra la ventana emergente)
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    private void agregarCarpeta(String nombreCarpeta){


        QUERY_URL = MyDataArrays.direccion + "/folder/" + username + PATH_ACTUAL + nombreCarpeta;

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);

        client.put(QUERY_URL, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
                get(null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmarEliminarORestaurar(final String id, final boolean restaurar){

        // Muestro una ventana emergente para confirmar que quiere eliminar
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        if(restaurar){
            alert.setTitle("Restaurar!");
            alert.setMessage("¿Esta seguro que quiere restaurar " + id + "?");
            // El boton "OK" confirma la restauracion del archivo o carpeta
            alert.setPositiveButton("Restaurar", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    eliminarORestaurar(id, restaurar);
                }
            });
        } else {
            alert.setTitle("Eliminar!");
            alert.setMessage("¿Esta seguro que quiere eliminar " + id + "?");
            // El boton "OK" confirma la eliminacion del archivo o carpeta
            alert.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    eliminarORestaurar(id, restaurar);
                }
            });
        }




        // Un boton de cancelar, que no hace nada (se cierra la ventana emergente)
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    private void eliminarORestaurar(final String id, boolean restore){
        String tipoDeArchivo = obtenerTipoDeArchivo(id);

        if(tipoDeArchivo.equals(MyDataArrays.caracterReservado + "folder")){
            QUERY_URL = MyDataArrays.direccion + "/folder/";
        } else {
            QUERY_URL = MyDataArrays.direccion + "/file/";
        }

        String extension = "";
        if( obtenerTipoDeArchivo(id) != null)
            extension = "." + obtenerTipoDeArchivo(id);
//        QUERY_URL = QUERY_URL + username + PATH_ACTUAL + id + extension;
        QUERY_URL = QUERY_URL +  obtenerURLArchivos(id + extension);


//        Toast.makeText(getApplicationContext(), id + extension, Toast.LENGTH_LONG).show();
//        Toast.makeText(getApplicationContext(), QUERY_URL, Toast.LENGTH_LONG).show();



        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        if(restore){
            params.put("restore", "true");
        }

        // El delete pide que le pase Headers, le pongo basura
        Header[] header = {
                new BasicHeader("Content-type", "application/x-www-form-urlencoded")
                ,new BasicHeader("Content-type", "application/x-www-form-urlencoded")
                ,new BasicHeader("Accep", "text/html,text/xml,application/xml")
                ,new BasicHeader("Connection", "keep-alive")
                ,new BasicHeader("keep-alive", "115")
                ,new BasicHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2")
        };

        client.delete(getApplicationContext(), QUERY_URL, header, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(getApplicationContext(), id + " se ha movido a la papelera.", Toast.LENGTH_LONG).show();
                get(null);
            }


            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor" + statusCode, Toast.LENGTH_LONG).show();

                Toast.makeText(getApplicationContext(), QUERY_URL, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cerrarSesion(){

        QUERY_URL = MyDataArrays.direccion + "/session/" + token;

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("user", username);

        // El delete pide que le pase Headers, le pongo basura
        Header[] header = {
                new BasicHeader("Content-type", "application/x-www-form-urlencoded")
                ,new BasicHeader("Content-type", "application/x-www-form-urlencoded")
                ,new BasicHeader("Accep", "text/html,text/xml,application/xml")
                ,new BasicHeader("Connection", "keep-alive")
                ,new BasicHeader("keep-alive", "115")
                , new BasicHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2")
        };

        client.delete(getApplicationContext(), QUERY_URL, header, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(getApplicationContext(), "Sesión cerrada", Toast.LENGTH_LONG).show();
                pasarAElegir();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void obtenerMetadatos(String id){

        String extension = "";
        if( obtenerTipoDeArchivo(id) != null)
            extension = "." + obtenerTipoDeArchivo(id);

        // Creo un Intent para pasar a la activity de mostrar metadatos
        Intent metadatosIntent = new Intent(this, MetadatosActivity.class);

        // Agrego la informacion que quiero al Intent
        metadatosIntent.putExtra("token", token);
        metadatosIntent.putExtra("username", username);
        metadatosIntent.putExtra("pathArchivo", PATH_ACTUAL + id + extension);

        // Inicio la actividad con el Intent
        startActivityForResult(metadatosIntent, METADATOS);
    }

    // Este metodo es para volver para atras en los fragmentos
    // y cerrar la aplicacion cuando volvi al inicio de todo
    @Override
    public void onBackPressed() {

        // Este boolean sirve para saber si quiero volver a la carpeta superior
        boolean volver = true;
        int index = -1 , indice =  -1;
        if(PATH_ACTUAL.equals("/")){
            getFragmentManager().popBackStack();
            getFragmentManager().popBackStack();
            this.finish();
            volver = false; // Ya llegue a la raiz, no quiero volver mas
        }

        if(PATH_ACTUAL.contains("permisos")){
            PATH_ACTUAL = "//";
            QUERY_URL_CARPETAS = MyDataArrays.direccion + "/folder/" + username + "//";
        }

        for( int i = 0; volver && i < 2; i++){

             index = QUERY_URL_CARPETAS.lastIndexOf("/");
            QUERY_URL_CARPETAS = QUERY_URL_CARPETAS.substring(0, index);

            indice = PATH_ACTUAL.lastIndexOf("/");
            PATH_ACTUAL = PATH_ACTUAL.substring(0, indice);

        }
        QUERY_URL_CARPETAS = QUERY_URL_CARPETAS + "/";
        PATH_ACTUAL = PATH_ACTUAL + "/";


        if(volver)  get(null);
    }

    // Este metodo sirve para ir a la activity de iniciar sesion o registrarse
    private void pasarAElegir(){

        // Creo un Intent para pasar a la activity de elegir para iniciar sesion o registrarse
        Intent elegirIntent = new Intent(this, ElegirSesionActivity.class);

        // Inicio la actividad con el Intent
        startActivity(elegirIntent);
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    // Este metodo sirve para ir a la activity de ver y actualizar perfil
    private void pasarAVerPerfil(){
        // Creo un Intent para pasar a la activity de ver y actualizar perfil
        Intent perfilIntent = new Intent(this, PerfilActivity.class);

        // Agrego la informacion que quiero al Intent
        perfilIntent.putExtra("token", token);
        perfilIntent.putExtra("username", username);

        // Inicio la actividad con el Intent
        startActivity(perfilIntent);
    }

    private String obtenerTipoDeArchivo(String id){
        String tipoDeArchivo = hashTipoArchivos.get(id);
        return tipoDeArchivo;
    }

    private void guardarMapaArchivos(JSONObject estructuraCarpetasJSON){
        hashTipoArchivos = new HashMap<String, String>();

        Iterator<?> keys = estructuraCarpetasJSON.keys();

        while( keys.hasNext() ) {
            String key = (String)keys.next();
            String value = "";
            try{
                value = estructuraCarpetasJSON.getString(key);
            } catch (JSONException e){

            }
            int index = value.lastIndexOf(".");
            String nombre = "";
            String extension = "";
            if(index >= 0){
                nombre = value.substring(0, index);
                extension = value.substring(index+1);
            } else {
                nombre = value;
            }
            hashTipoArchivos.put(nombre, extension);

        }
    }

    private void guardarURLArchivos(JSONObject estructuraCarpetasJSON){
        URLArchivos = new HashMap<String, String>();

        Iterator<?> keys = estructuraCarpetasJSON.keys();

        while( keys.hasNext() ) {
            String key = (String)keys.next();
            String value = "";
            try{
                value = estructuraCarpetasJSON.getString(key);
            } catch (JSONException e){

            }
//            int index = value.lastIndexOf(".");
//            String nombre = "";
//            String extension = "";
//            if(index >= 0){
//                nombre = value.substring(0, index);
//                extension = value.substring(index+1);
//            } else {
//                nombre = value;
//            }
            URLArchivos.put(value, key);

        }
    }

    private String obtenerURLArchivos(String id){
        String tipoDeArchivo = URLArchivos.get(id);
        return tipoDeArchivo;
    }

    private void compartirArchivo(final String idArchivo){

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setIcon(R.drawable.files);
        builderSingle.setTitle("Introducir nombre: ");
        arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.select_dialog_singlechoice);
//        arrayAdapter.add("Hardik");
//        arrayAdapter.add("Archit");
//        arrayAdapter.add("Jignesh");
//        arrayAdapter.add("Umang");
//        arrayAdapter.add("Gatti");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText usuario = new EditText(this);
        textoFallido = new TextView(this);
        usuario.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                arrayAdapter.clear();
                if( s.length() > 0 ){
                    getUsuarios(s.toString());
                } else {
                    arrayAdapter.clear();
                }
                boolean noSaque = true;



//                try{
////                    usuarios = usuariosParaCompartir.getString("usuarios");
//                } catch (JSONException e ){
//
//                }
//                arrayAdapter.add(usuarios);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                //if statement here I guess
//                usuario.setText(usuario.getText() + "Hello, world");
            }
        });
//        builderSingle.setView(usuario);


        textoFallido.setText("La busqueda de usuarios no dio resultados.");
        textoFallido.setVisibility(View.INVISIBLE);
        layout.addView(textoFallido);
        layout.addView(usuario);

        builderSingle.setView(layout);
//        builderSingle.setView(texto);

        builderSingle.setNegativeButton("cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strName = arrayAdapter.getItem(which);
                        actualizarMetadatos(idArchivo, strName);

//                        AlertDialog.Builder builderInner = new AlertDialog.Builder(MainActivity.this);
//                        builderInner.setMessage(strName);
//                        builderInner.setTitle("Your Selected Item is");
//                        builderInner.setPositiveButton("Ok",
//                                new DialogInterface.OnClickListener() {
//
//                                    @Override
//                                    public void onClick(
//                                            DialogInterface dialog, int which) {
//                                        dialog.dismiss();
//                                    }
//                                });
//                        builderInner.show();
                    }
                });
        builderSingle.create();
        builderSingle.show();
    }

    public void actualizarMetadatos(String idArchivo, final String idUsuario){

//        Toast.makeText(getApplicationContext(), "Mando: " + idArchivo + obtenerTipoDeArchivo(idArchivo), Toast.LENGTH_LONG).show();


        QUERY_URL_METADATOS = MyDataArrays.direccion + "/metadata/" + obtenerURLArchivos(idArchivo + "." + obtenerTipoDeArchivo(idArchivo)); //idArchivo + "." + obtenerTipoDeArchivo(idArchivo);

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        Toast.makeText(getApplicationContext(), "Mando: " + QUERY_URL_METADATOS, Toast.LENGTH_LONG).show();


        client.get(QUERY_URL_METADATOS, params, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
//                        Toast.makeText(getApplicationContext(), "Recibi metadatos\n" + jsonObject.toString(), Toast.LENGTH_LONG).show();

                        try {

                            JSONObject metadatos = jsonObject.getJSONObject("metadatos");

                            JSONArray usrCompartidos = metadatos.getJSONArray("usuarios");

                            int cantidadUsr = usrCompartidos.length();
                            usrCompartidos.put(cantidadUsr, idUsuario);
//                            for(int i = 0; i < cantidadUsr; i++){
//
//                                // Si estoy en el ultimo item no agego el ";"
//                                if(i == (cantidadUsr - 1)) {
//                                    usuariosString = usuariosString + usrCompartidos.getString(i);
//                                } else {
//                                    usuariosString = usuariosString + usrCompartidos.getString(i) + "; ";
//                                }
//                            }
                            metadatos.put("usuarios", usrCompartidos);
//                            metadatos.put("usuarios", metadatos.getString("usuarios").get + MyDataArrays.caracterReservado + idUsuario);
                                    Toast.makeText(getApplicationContext(), metadatos.toString() , Toast.LENGTH_LONG).show();
                            mandarMetadatos(metadatos);


//                            metadatos.getString("nombre") + metadatos.getString("extension");
//                            metadatos.getString("fecha ultima modificacion");
//                            metadatos.getString("usuario ultima modificacion");
//                            metadatos.getString("propietario");

                            // Obtengo las etiquetas
//                            JSONArray etiquetas = metadatos.getJSONArray("etiquetas");
//                            int cantidadEtiquetas = etiquetas.length();
//                            String etiquetasString = "";
//                            for(int i = 0; i < cantidadEtiquetas; i++){
//
//                                // Si estoy en el ultimo item no agego el ";"
//                                if(i == (cantidadEtiquetas - 1)) {
//                                    etiquetasString = etiquetasString + etiquetas.getString(i);
//                                } else {
//                                    etiquetasString = etiquetasString + etiquetas.getString(i) + "; " ;
//                                }
//                            }
//                            etiquetasEditText.setText(etiquetasString);
//
//                            // Obtengo los usuarios con permisos para este archivo
//                            JSONArray usrCompartidos = metadatos.getJSONArray("usuarios");
//                            int cantidadUsr = usrCompartidos.length();
//                            String usuariosString = "";
//                            for(int i = 0; i < cantidadUsr; i++){
//
//                                // Si estoy en el ultimo item no agego el ";"
//                                if(i == (cantidadUsr - 1)) {
//                                    usuariosString = usuariosString + usrCompartidos.getString(i);
//                                } else {
//                                    usuariosString = usuariosString + usrCompartidos.getString(i) + "; ";
//                                }
//                            }
//                            usuariosCompartidosTextView.setText(usuariosString);

                        } catch (JSONException e){

                        }
                    }

                    @Override
                    public void onFailure ( int statusCode, Header[] headers, Throwable throwable, JSONObject error){
                        Toast.makeText(getApplicationContext(), "Error al conectar con el servidor en recibir", Toast.LENGTH_LONG).show();
                    }
                }

        );
    }

    public void mandarMetadatos(JSONObject metadatos){ //final String nombre, String extension, String etiquetas, String usrCompartidos){

        AsyncHttpClient client = new AsyncHttpClient();

//        JSONObject jsonMetadatos = new JSONObject();
//        String timeStamp = "";
//        int index;
//        String nombre = "";
//        String extension = "";
//        JSONArray etiquetasJSONArray =  new JSONArray();
//        JSONArray usuariosCompartidosJSONArray = new JSONArray();
//
//        try{
//            index = nombreArchivo.lastIndexOf(".");
//            nombre = nombreArchivo.substring(0, index);
//            extension = nombreArchivo.substring(index + 1);
//
//            jsonMetadatos.put("nombre", nombre);
//            jsonMetadatos.put("extension", extension);
//            jsonMetadatos.put("propietario", propietario);
//            timeStamp = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());       //"mm.ss.dd.MM.yyyyyyyy").format(new Date());
//
//            jsonMetadatos.put("fecha ultima modificacion", timeStamp);
//            jsonMetadatos.put("usuario ultima modificacion", username);
//
//
//            String[] partesEtiquetas = etiquetas.split(";");
//
//            for(String etiqueta : partesEtiquetas){
//                etiquetasJSONArray.put(etiqueta);
//            }
//            jsonMetadatos.put("etiquetas", etiquetasJSONArray);
//
//
//            String[] partesUsuarios = usrCompartidos.split(";");
//
//            for(String usuario : partesUsuarios){
//                usuario = usuario.trim();
//                usuariosCompartidosJSONArray.put(usuario);
//            }
//            jsonMetadatos.put("usuarios", usuariosCompartidosJSONArray);
//
//        } catch (JSONException e){
//        }

//        Toast.makeText(getApplicationContext(), "tiempo: " + timeStamp, Toast.LENGTH_LONG).show();
//        Toast.makeText(getApplicationContext(), "usuarios: " + usuariosCompartidosJSONArray.toString(), Toast.LENGTH_LONG).show();
//        Toast.makeText(getApplicationContext(), "etiquetas: " + etiquetasJSONArray.toString(), Toast.LENGTH_LONG).show();

//        Toast.makeText(getApplicationContext(), jsonMetadatos.toString(), Toast.LENGTH_LONG).show();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        params.put("metadatos", metadatos.toString());

        client.put(QUERY_URL_METADATOS, params, new JsonHttpResponseHandler() {
            //
            @Override
            public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
                Toast.makeText(getApplicationContext(), "Archivo actualizado", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor en mandar", Toast.LENGTH_LONG).show();
            }
        });
    }
}
