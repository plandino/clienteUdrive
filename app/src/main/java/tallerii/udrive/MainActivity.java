package tallerii.udrive;

import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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


    private ArrayAdapter<String> arAdapter;

    private String usersYaCompartidos;

    AlertDialog.Builder alertCompartidos;

    ListView listaCompartidos;

    SharedPreferences mSharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("MAIN: ", "Se inicio la MainActivity.");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        token = intent.getStringExtra("token");
        username = intent.getStringExtra("username");
        usuariosParaCompartir = new JSONObject();

        Log.d("MAIN: ", "Saque del Intent el token: \"" + token + "\".");
        Log.d("MAIN: ", "Saque del Intent el username: \"" + username + "\".");

        QUERY_URL_CARPETAS = QUERY_URL_CARPETAS + username + "/";

        Log.d("MAIN: ", "Forme el URL_CARPETAS: \"" + QUERY_URL_CARPETAS + "\".");

        fragmentManager = getFragmentManager();

        // Creo el LocationManager y le digo que se puede ubicar usando Internet o el GPS
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        locationListener = new MyLocationListener();

        try{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Log.i("MAIN: ", "Inicie el GPS.");

        } catch (SecurityException e){
            Log.e("MAIN:", "Error al iniciar el GPS.");
            Log.e("MAIN:", e.getMessage());
            Toast.makeText(getApplicationContext(), "No se pudo inicializar el GPS.", Toast.LENGTH_LONG).show();
        }

        try{
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            Log.i("MAIN: ", "Inicie el GPS a traves de Internet.");

        } catch (SecurityException e){
            Log.e("MAIN:", "Error al iniciar el GPS a traves de la conexion de Internet.");
            Log.e("MAIN:", e.getMessage());
            Toast.makeText(getApplicationContext(), "No se pudo inicializar el GPS.", Toast.LENGTH_LONG).show();
        }

        get(null);
    }

    @Override
    public void subirFAB(){
        seleccionarArchivo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.buscar_archivo).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

//        final SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                onSearchRequested();
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                // Do something
//                onSearchRequested();
//
//                return true;
//            }
//        };
//
//        searchView.setOnQueryTextListener(queryTextListener);

        return true;

//        // Get the SearchView and set the searchable configuration
//        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        SearchView searchView = (SearchView) menu.findItem(R.id.buscar_archivo).getActionView();
//        // Assumes current activity is the searchable activity
//        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//        searchView.setIconifiedByDefault(true); // Do not iconify the widget; expand it by default
//        final SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                onSearchRequested();
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                // Do something
//                onSearchRequested();
//
//                return true;
//            }
//        };
//
//        searchView.setOnQueryTextListener(queryTextListener);
//        return true;
    }

    @Override
    public boolean onSearchRequested() {
        Bundle appDataBundle = new Bundle();
        appDataBundle.putString("user", username);
        appDataBundle.putString("token", token);
        startSearch(null, false, appDataBundle, false);
        return true;
    }

    @Override
    public void startActivity(Intent intent) {
        // check if search intent
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            intent.putExtra("user", username);
            intent.putExtra("token", token);
        }

        super.startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.buscar_archivo:
                Log.i("MAIN: ", "Hice click en el boton de buscar archivo.");
                onSearchRequested();
                return true;
            case R.id.subir_archivo:
                Log.i("MAIN: ", "Hice click en el boton de subir archivo.");
                seleccionarArchivo();
                return true;
            case R.id.crear_carpeta:
                Log.i("MAIN: ", "Hice click en el boton de crear carpeta.");
                crearCarpeta();
                return true;
            case R.id.carpetas_compartidas:
                Log.i("MAIN: ", "Hice click en el boton para acceder a los archivos compartidos.");
                get(MyDataArrays.caracterReservado + "permisos");
                return true;
            case R.id.papelera:
                Log.i("MAIN: ", "Hice click en el boton para acceder a la papelera.");
                get(MyDataArrays.caracterReservado + "trash");
                return true;
            case R.id.ver_perfil:
                Log.i("MAIN: ", "Hice click en el boton para ver el perfil.");
                pasarAVerPerfil();
                return true;
            case R.id.cerrar_sesion:
                Log.i("MAIN: ", "Hice click en el boton para cerrar sesión.");
                cerrarSesion();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i("MAIN: ", "La activity anterior me dio un resultado.");
        Log.d("MAIN: ", "El request code es: \"" + requestCode + "\".");

        switch(requestCode){
            case PICKFILE_RESULT_CODE:
                Log.i("MAIN: ", "El request fue buscar un archivo.");

                if(resultCode==RESULT_OK){

                    Log.i("MAIN: ", "El resultado de la activity es OK.");

                    if (data == null){
                        Log.i("MAIN: ", "No hay data en el resultado de la activity.");
                        return;
                    }

                    String selectedImagePath;
                    Uri selectedImageUri = data.getData();

                    // Tengo que obtener el path del archivo. Uso el FilePathGetter para traducir
                    // de una Uri a un file path absoluto
                    selectedImagePath = FilePathGetter.getPath(getApplicationContext(), selectedImageUri);
                    Log.d("MAIN: ", "El path del archivo seleccionado es: " + selectedImagePath);

                    subir(selectedImagePath);
                }
                break;
            case METADATOS:
                Log.i("MAIN: ", "El request fue acceder a los metadatos.");

                if(resultCode==RESULT_OK){
                    Log.i("MAIN: ", "El resultado de la activity es OK.");
                    get(null);
                }

        }
    }

    /**
     * Hace un GET al archivo o carpeta segun corresponda
     * @param id nombre del archivo o carpeta de la cual se quiere obtener informacion
     */
    private void get(String id) {
        Log.d("MAIN: ", "Voy a obtener la estrcutra interna de la carpeta: \"" + id + "\".");

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

        Log.d("MAIN: ", "El URL al que le pego es: \"" + QUERY_URL_CARPETAS + "\".");

        client.get(QUERY_URL_CARPETAS, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                Log.i("MAIN: ", "Exito al obtener la estructura interna de la carpeta.");

                try {
                    estructuraCarpetas = jsonObject.getString("estructura");
                    Log.d("MAIN: ", "La estructura de la carpeta en formato String: \"" + estructuraCarpetas + "\".");

                    estructuraCarpetasJSON = jsonObject.getJSONObject("estructura");
                    Log.d("MAIN: ", "La estructura de la carpeta en formato JSON: \"" + estructuraCarpetasJSON + "\".");

                    guardarMapaArchivos(estructuraCarpetasJSON);
                    guardarURLArchivos(estructuraCarpetasJSON);


                } catch (JSONException e) {
                    Log.e("MAIN: ", e.getMessage());
                    Log.e("MAIN: ", "No pude obtener los datos del JSON.");
                    e.printStackTrace();
                }
                crearNuevoFragmento(estructuraCarpetas);
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");

                if (error == null) {
                    Log.e("MAIN: ", "No se pudo comunicar con el servidor.");

                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        Log.e("MAIN: ", "Hubo un problema al obtener la estructura de la carpeta.");
                        Log.e("MAIN: ", error.getString("error"));

                        Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        Log.e("MAIN: ", e.getMessage());
                        Log.e("MAIN: ", "No se pudo obtener el mensaje de error del JSON.");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Manda un GET al servidor, para hacer una busqueda de los usuarios que concuerdan con el texto ingresado
     * para poder compartir un archivo.
     * Agrego los usuarios que concuerdan con la busqueda a una lista visible en un AlertDialog.
     * @param id String con la porcion del nombre del usuario a buscar.
     */
    private void getUsuarios(String id) {

        Log.i("MAIN: ", "Voy a obtener la busqueda de usuarios para compartir un archivo.");

        String QUERY_URL_USUARIOS = MyDataArrays.direccion + "/profile";

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        params.put("busqueda", id);

        Log.d("MAIN: ", "Voy a ingresar los siguientes datos en los parametros del GET: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");
        Log.d("MAIN: ", "El token ingresado es: \"" + token + "\".");
        Log.d("MAIN: ", "La busqueda es: \"" + id + "\".");

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL_USUARIOS + "\".");

        client.get(QUERY_URL_USUARIOS, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                Log.i("MAIN: ", "Busqueda de usuarios para compartir exitosa.");

                try {
                    usuariosParaCompartir = jsonObject.getJSONObject("busqueda");
                    Log.d("MAIN: ", "Los usuarios que concuerdan con la busqueda en formato JSON son: \"" + usuariosParaCompartir.toString() + "\".");

                    String usuarios = usuariosParaCompartir.getString("usuarios");
                    Log.d("MAIN: ", "Los usuarios que concuerdan con la busqueda en formato STRING son: \"" + usuarios + "\".");

                    if (usuarios.length() > 0) {
                        alertCompartidos.setMessage("");
                        Log.d("MAIN: ", "Agrego los usuarios a la lista.");

//                        textoFallido.setVisibility(View.INVISIBLE);
                        String[] partes = usuarios.split(Character.toString(MyDataArrays.caracterReservado));
                        String nuevo = usuarios.replaceAll(String.valueOf(MyDataArrays.caracterReservado), "\",\"");
                        Log.d("MAIN: ", "El String para mostrar los usuarios para compartir es: \"" + nuevo + "\".");

                        for(String user : partes){
                            if(!user.equals(username)){
                                arAdapter.add(user);
                            }
                        }
                        Log.d("MAIN: ", "Agregue todos los usuarios a la lista.");

                    } else {
                        alertCompartidos.setMessage("No hubo ninguna coincidencia con la busqueda");
                        Log.i("MAIN: ", "Ningun usuario concuerda con la busqueda.");
//                        textoFallido.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    Log.e("MAIN: ", e.getMessage());
                    Log.e("MAIN: ", "No se pudo obtener los datos del JSON.");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");

                if (error == null) {
                    Log.e("MAIN: ", "No se pudo comunicar con el servidor.");

                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        Log.e("MAIN: ", "Hubo un problema al realizar una busqueda de un usuario para compartir un archivo.");
                        Log.e("MAIN: ", error.getString("error"));

                        Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        Log.e("MAIN: ", e.getMessage());
                        Log.e("MAIN: ", "No se pudo obtener el mensaje de error del JSON.");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Maneja los clicks hechos sobre un padre en la ExpandableList.
     * Cuando es una carpeta, accede a ella y obtiene la estructura interna.
     * Cuando es un archivo, busca los metadatos del archivo.
     * @param id nombre del item padre(carpeta o archivo) del cual se hizo click.
     */
    @Override
    public void onGroupClick(String id) {

        Log.d("MAIN: ", "Hice click sobre: \"" + id + "\".");;

        String tipoDeArchivo = obtenerTipoDeArchivo(id);

        Log.d("MAIN: ", "El tipo de archivo es: \"" + tipoDeArchivo + "\".");;

        if(tipoDeArchivo.equals(MyDataArrays.caracterReservado + "folder")) {
            Log.i("MAIN: ", "Accedo y obtengo la estructura interna de la carpeta.");;
            get(id);
        } else {
            Log.i("MAIN: ", "Obtengo los metadatos del archivo.");;
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


    public void crearNuevoFragmento(String estructuraCarpetas){
        Log.i("MAIN: ", "Voy a crear un nuevo fragmento.");;
        Log.d("MAIN: ", "La estructura a usar es: \"" + estructuraCarpetas + "\".");;


        // Aca recibo desde el fragmento, la carpeta que seleccione y tengo que pedirle al servidor
        // que me devuelva la estructura de carpetas y archivos que estan adentro de esa.
        // Luego creo un nuevo fragmento con esa estructura y reemplazo el anterior

        //Paso 3: Creo un nuevo fragmento
        FilesFragment fragment = new FilesFragment();

        // Creo un Bundle y le agrego informacion del tipo <Key, Value>
        Bundle bundle = new Bundle();
        bundle.putString("estructura", estructuraCarpetas);

        // Le agrego el Bundle al fragmento
        fragment.setArguments(bundle);

        //Paso 2: Crear una nueva transacción
//        FragmentTransaction transaction2 =
        fragmentManager.beginTransaction().replace(R.id.contenedor, fragment).commit();

        //Paso 4: Confirmar el cambio
//        transaction2.commit();
    }

    @Override
    public void onOptionClick(String id, String opcion) {

        if(opcion.equals("Descargar")){
            Log.i("MAIN: ", "Hice click en la opcion DESCARGAR.");;
            descargarArchivo(id);
        }

        if(opcion.equals("Compartir")){
            Log.i("MAIN: ", "Hice click en la opcion COMPARTIR.");;
            compartirArchivo(id);
        }

        if(opcion.equals("Eliminar")){
            Log.i("MAIN: ", "Hice click en la opcion ELIMINAR.");;
            confirmarEliminarORestaurar(id, MyDataArrays.ELIMINAR);
        }

        if(opcion.equals("Restaurar")){
            Log.i("MAIN: ", "Hice click en la opcion RESTAURAR.");;
            confirmarEliminarORestaurar(id, MyDataArrays.RESTAURAR);
        }
    }

    private void subir(String path){

        Log.i("MAIN: ", "Voy a subir un archivo.");;

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

        Log.d("MAIN: ", "Voy a ingresar los siguientes datos en los parametros del PUT: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");
        Log.d("MAIN: ", "El token ingresado es: \"" + token + "\".");
        Log.d("MAIN: ", "Latitud: \"" + latitud + "\" || Longitud: \"" + longitud + "\".");

        try{
            File archivo = new File(path);
            params.put("file", archivo);
            Log.i("MAIN: ", "Pude incluir el archivo en los parametros.");
        } catch (FileNotFoundException e){
            Log.e("MAIN: ", e.getMessage());
            Log.e("MAIN: ", "No se pudo encontrar el archivo.");
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "No se pudo encontrar el archivo", Toast.LENGTH_LONG).show();
        }

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL + "\".");

        client.put(QUERY_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, JSONObject jsonObject) {
                Log.i("MAIN: ", "Exito al subir un archivo.");
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");

                Toast.makeText(getApplicationContext(), "Archivo subido", Toast.LENGTH_LONG).show();
                // Luego de subir un archivo actualizo la estructura de la carpeta
                get(null);
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");

                if (error == null) {
                    Log.e("MAIN: ", "No se pudo comunicar con el servidor.");

                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        Log.e("MAIN: ", "Hubo un problema al subir un archivo.");
                        Log.e("MAIN: ", error.getString("error"));

                        Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        Log.e("MAIN: ", e.getMessage());
                        Log.e("MAIN: ", "No se pudo obtener el mensaje de error del JSON.");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void seleccionarArchivo(){

        Log.i("MAIN: ", "Voy a seleccionar un archivo para subir.");

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICKFILE_RESULT_CODE);
    }

    private void descargarArchivo(final String id){

        Log.i("MAIN: ", "Voy a descargar un archivo.");

        QUERY_URL = MyDataArrays.direccion + "/file/" +  obtenerURLArchivos(id + "." + obtenerTipoDeArchivo(id)) ;
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);

        Log.d("MAIN: ", "Voy a ingresar los siguientes datos en los parametros del GET: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");
        Log.d("MAIN: ", "El token ingresado es: \"" + token + "\".");

        final String savedFilePath = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + id; // "/data/data/tallerii.udrive/files/" + nombreArchivo;

        Log.d("MAIN: ", "Guardo el archivo en el path: \"" + savedFilePath + "\".");

        File file = new File(savedFilePath);

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL + "\".");

        client.get(QUERY_URL, params, new FileAsyncHttpResponseHandler(file) {
                    //                        @Override
                    //                        public void onProgress(int bytesWritten, int totalSize) {
                    //                            Toast.makeText(getApplicationContext(), "Descargando...", Toast.LENGTH_LONG).show();
                    //                        }

                    @Override
                    public void onSuccess(File file) {

                        Log.i("MAIN: ", "Exito al descarga el archivo.");

                        String extension = obtenerTipoDeArchivo(id);
                        Log.d("MAIN: ", "La extension del archivo es: \"" + extension + "\".");

                        if (extension.equals("txt") || extension.equals("jpg") || extension.equals("png") || extension.equals("jpeg")) {

                            Log.i("MAIN: ", "Voy a visualizar el archivo.");

                            // Cuando la descarga es una foto o un archivo de texto, cambio a otra
                            // activity para ver el archivo
                            Intent intentDisplay = new Intent(MainActivity.this, DisplayActivity.class);

                            intentDisplay.putExtra("extensionArchivo", extension);
                            intentDisplay.putExtra("filePath", savedFilePath);
                            intentDisplay.putExtra("nombreArchivo", id);

                            Log.d("MAIN: ", "Le agrego al Intent la extension: \"" + extension + "\".");
                            Log.d("MAIN: ", "Le agrego al Intent el path al archivo guardado: \"" + savedFilePath + "\".");
                            Log.d("MAIN: ", "Le agrego al Intent el nombre: \"" + id + "\".");

                            startActivity(intentDisplay);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Throwable e, File response) {
                        Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");
                        Log.e("MAIN: ", "Hubo un problema al descargar un archivo.");

                        Toast.makeText(getApplicationContext(), "No se pudo descargar el archivo.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void crearCarpeta(){

        Log.i("MAIN: ", "Voy a crear una carpeta.");

        // Muestro una ventana emergente para que introduzca el nombre de la carpeta a crear
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Crear carpeta");
        alert.setMessage("Introduzca el nombre de la nueva carpeta");
        final EditText nombreCarpeta = new EditText(this);
        alert.setView(nombreCarpeta);

        // El boton "Crear" crea la carpeta
        alert.setPositiveButton("Crear", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("MAIN: ", "Hice click en Crear. El nombre introducido es: \"" + nombreCarpeta.getText().toString() + "\".");

                agregarCarpeta(nombreCarpeta.getText().toString());
            }
        });

        // Un boton de cancelar, que no hace nada (se cierra la ventana emergente)
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("MAIN: ", "Hice click en Cancelar.");
            }
        });

        alert.show();

        Log.i("MAIN: ", "Mostre un dialogo para que introduzca el nombre de la capreta.");

    }

    private void agregarCarpeta(String nombreCarpeta){

        Log.i("MAIN: ", "Voy a mandar la carpeta creada al servidor.");

        QUERY_URL = MyDataArrays.direccion + "/folder/" + username + PATH_ACTUAL + nombreCarpeta;

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);

        Log.d("MAIN: ", "Ingrese los siguientes datos en los parametros del PUT: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");
        Log.d("MAIN: ", "El token ingresado es: \"" + token + "\".");

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL + "\".");

        client.put(QUERY_URL, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, JSONObject jsonObject) {
                Log.i("MAIN: ", "Exito al crear la carpeta en el servidor.");
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");
                //Luego de crear la carpeta, actualizo la vista
                get(null);
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");

                if (error == null) {
                    Log.e("MAIN: ", "No se pudo comunicar con el servidor.");

                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        Log.e("MAIN: ", "Hubo un problema al crear una carpeta.");
                        Log.e("MAIN: ", error.getString("error"));

                        Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        Log.e("MAIN: ", e.getMessage());
                        Log.e("MAIN: ", "No se pudo obtener el mensaje de error del JSON.");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void confirmarEliminarORestaurar(final String id, final boolean restaurar){

        if(restaurar){
            Log.i("MAIN: ", "Voy a confirmar para restaurar un archivo.");
        } else {
            Log.i("MAIN: ", "Voy a confirmar para eliminar un archivo.");
        }

        // Muestro una ventana emergente para confirmar que quiere eliminar
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        if(restaurar == MyDataArrays.RESTAURAR){
            alert.setTitle("Restaurar!");
            alert.setMessage("¿Esta seguro que quiere restaurar " + id + "?");
            // El boton "OK" confirma la restauracion del archivo o carpeta
            alert.setPositiveButton("Restaurar", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    Log.i("MAIN: ", "Hice click en Restaurar.");
                    eliminarORestaurar(id, restaurar);
                }
            });
        } else {
            alert.setTitle("Eliminar!");
            alert.setMessage("¿Esta seguro que quiere eliminar " + id + "?");
            // El boton "OK" confirma la eliminacion del archivo o carpeta
            alert.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    Log.i("MAIN: ", "Hice click en Eliminar.");
                    eliminarORestaurar(id, restaurar);
                }
            });
        }

        // Un boton de cancelar, que no hace nada (se cierra la ventana emergente)
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("MAIN: ", "Hice click en cancelar.");
            }
        });

        alert.show();

        Log.i("MAIN: ", "Mostre un Alert Dialog para confirmar que quiero eliminar o restaurar un archivo.");
    }

    private void eliminarORestaurar(final String id, boolean restore){

        if(restore){
            Log.i("MAIN: ", "Voy a mandar restaurar un archivo de la papelera en el servidor.");
        } else {
            Log.i("MAIN: ", "Voy a eliminar un archivo.");

        }

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

        AsyncHttpClient client = new AsyncHttpClient();

        final String restaurar;
        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        if(restore){
            restaurar = "true";
            params.put("restore", restaurar);
        } else {
            restaurar = "false";
            params.put("restore", restaurar);
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

        Log.d("MAIN: ", "Ingrese los siguientes datos en los parametros del DELETE: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");
        Log.d("MAIN: ", "El token ingresado es: \"" + token + "\".");
        Log.d("MAIN: ", "Quiero restaurar: \"" + restaurar + "\".");

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL + "\".");

        client.delete(getApplicationContext(), QUERY_URL, header, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (restaurar.equals("true")) {
                    Log.i("MAIN: ", "Se ha restaurado: \"" + id + "\" exitosamente.");
                    Toast.makeText(getApplicationContext(), "Se ha restaurado " + id, Toast.LENGTH_LONG).show();
                } else {
                    Log.i("MAIN: ", "Se ha eliminado: \"" + id + "\" exitosamente.");
                    Toast.makeText(getApplicationContext(), "Se ha eliminado " + id, Toast.LENGTH_LONG).show();
                }
                // Luego actualizo la vista
                get(null);
            }


            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");
                if (restaurar.equals("true")) {
                    Log.e("MAIN: ", "Hubo un problema al restaurar: \"" + id + "\".");
                    Toast.makeText(getApplicationContext(), "Hubo un problema al restaurar: \"" + id + "\".", Toast.LENGTH_LONG).show();
                } else {
                    Log.e("MAIN: ", "Hubo un problema al eliminar: \"" + id + "\".");
                    Toast.makeText(getApplicationContext(), "Hubo un problema al eliminar: \"" + id + "\".", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void cerrarSesion(){

        Log.i("MAIN: ", "Voy a cerrar sesion.");

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

        Log.d("MAIN: ", "Ingrese los siguientes datos en los parametros del DELETE: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL + "\".");

        client.delete(getApplicationContext(), QUERY_URL, header, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");
                Log.i("MAIN: ", "Sesion cerrada correctamente.");

                Toast.makeText(getApplicationContext(), "Sesión cerrada", Toast.LENGTH_LONG).show();

                // Accedo a los datos guardados
                mSharedPreferences = getSharedPreferences(MyDataArrays.SESION_DATA, MODE_PRIVATE);

                // Guardo en la memoria, el username y el token
                SharedPreferences.Editor e = mSharedPreferences.edit();
                e.remove(MyDataArrays.USERNAME);
                e.remove(MyDataArrays.TOKEN);
                e.commit();

                pasarAElegir();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");
                Log.e("MAIN: ", "Hubo un problema al cerrar sesion.");
                Toast.makeText(getApplicationContext(), "Hubo un problema al cerrar sesion", Toast.LENGTH_LONG).show();
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
        int index , indice ;
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
        return hashTipoArchivos.get(id);
    }

    private void guardarMapaArchivos(JSONObject estructuraCarpetasJSON){
        hashTipoArchivos = new HashMap<>();

        Iterator<?> keys = estructuraCarpetasJSON.keys();

        while( keys.hasNext() ) {
            String key = (String)keys.next();
            String value = "";
            try{
                value = estructuraCarpetasJSON.getString(key);
            } catch (JSONException e){

            }
            int index = value.lastIndexOf(".");
            String nombre;
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
        URLArchivos = new HashMap<>();

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
        return URLArchivos.get(id);
    }

    private void compartirArchivo(final String id){

        Log.i("MAIN: ", "Voy a crear un Alert Dialog para seleccionar los usuarios con los que " +
                "quiero compartir el archivo.");

        listaCompartidos = new ListView(this);
        arAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_multichoice);
        listaCompartidos.setAdapter(arAdapter);
        listaCompartidos.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        alertCompartidos = new AlertDialog.Builder(this);

        alertCompartidos.setTitle("Compartir");
        alertCompartidos.setMessage("");
        // El boton "Compartir" actualizo los metadatos con todos los usuarios tildados
        alertCompartidos.setPositiveButton("Compartir", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("MAIN: ", "Hice click en Compartir.");

                int cntChoice = listaCompartidos.getCount();

                String checked = "";
                SparseBooleanArray sparseBooleanArray = listaCompartidos.getCheckedItemPositions();

                for (int i = 0; i < cntChoice; i++) {
                    if (sparseBooleanArray.get(i)) {
                        checked += listaCompartidos.getItemAtPosition(i).toString() + MyDataArrays.caracterReservado;
                    }
                }
                actualizarMetadatos(id, checked, true);
            }
        });


        // Un boton de cancelar, que no hace nada (se cierra la ventana emergente)
        alertCompartidos.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("MAIN: ", "Hice click en cancelar.");
            }
        });



        listaCompartidos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {

                listaCompartidos.setItemChecked(pos, true);

//                if(lista.isItemChecked(pos)){
//                    lista.setItemChecked(pos, false);
//                } else {
//                    lista.setItemChecked(pos, true);
//                }
            }
        });


        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        float pxTodp = 300 / getResources().getDisplayMetrics().density;
        layout.setPadding((int)pxTodp, 0,(int) pxTodp, 0);
        final EditText usuario = new EditText(this);
//        textoFallido = new TextView(this);
        usuario.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                arAdapter.clear();
                if (s.length() > 0) {
                    getUsuarios(s.toString());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
//        layout.addView(textoFallido);
        layout.addView(listaCompartidos);
        layout.addView(usuario);

        alertCompartidos.setView(layout);

        actualizarMetadatos(id, "",false );

        alertCompartidos.show();

    }

    public void actualizarMetadatos(String idArchivo, final String usuariosACompartir, final boolean mandar){

        Log.i("MAIN: ", "Voy a los metadatos del archivo: \"" + idArchivo +"\".");
        QUERY_URL_METADATOS = MyDataArrays.direccion + "/metadata/" + obtenerURLArchivos(idArchivo + "." + obtenerTipoDeArchivo(idArchivo)); //idArchivo + "." + obtenerTipoDeArchivo(idArchivo);

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);

        Log.d("MAIN: ", "Ingrese los siguientes datos en los parametros del GET: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");
        Log.d("MAIN: ", "El token ingresado es: \"" + token + "\".");

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL_METADATOS + "\".");

        client.get(QUERY_URL_METADATOS, params, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, JSONObject jsonObject) {
                        Log.i("MAIN: ", "Recibi los metadatos para agregar usuarios compartidos, exitosamente.");

                        try {

                            JSONObject metadatos = jsonObject.getJSONObject("metadatos");
                            Log.d("MAIN: ", "Los metadatos en formato JSON son: \"" + metadatos.toString() + "\".");

                            JSONArray usrCompartidos = metadatos.getJSONArray("usuarios");
                            Log.d("MAIN: ", "Los usuarios compartidos en formato JSONArray son: \"" + usrCompartidos.toString() + "\".");

                            String[] partes = usuariosACompartir.split(String.valueOf(MyDataArrays.caracterReservado));
                            for(int i = 0; i < partes.length ; i++){
                                // Obtengo la cantidad de usuarios con lo que esta compartido este archivo
                                // y le agrego el usuario nuevo al final
                                int cantidadUsr = usrCompartidos.length();
                                Log.d("MAIN: ", "La cantidad de usuarios compartidos anteriormente es: \"" + cantidadUsr + "\".");
                                usrCompartidos.put(cantidadUsr, partes[i]);
                                Log.d("MAIN: ", "Los nuevos usuarios compartidos en formato JSONArray son: \"" + usrCompartidos.toString() + "\".");
                            }


                            if(mandar){
                                // Le pongo los nuevos usuarios compartidos a los metadatos y los mando
                                metadatos.put("usuarios", usrCompartidos);
                                mandarMetadatos(metadatos);
                            } else {
                                int cant = usrCompartidos.length();
                                for(int i = 0; i < cant -1 ; i++){
                                    arAdapter.add(usrCompartidos.getString(i));
                                    listaCompartidos.setItemChecked(i, true);
                                }
                            }


                        } catch (JSONException e){
                            Log.e("MAIN: ", e.getMessage());
                            Log.e("MAIN: ", "No se pudo obtener o reempazar los metadatos-usuariosCompartidos del JSON.");
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure ( int statusCode, Throwable throwable, JSONObject error){
                        Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");

                        if (error == null) {
                            Log.e("MAIN: ", "No se pudo comunicar con el servidor.");

                            Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                        } else {
                            try {
                                Log.e("MAIN: ", "Hubo un problema al recibir los metadatos con los usuarios compartidos que coinciden con la busqueda.");
                                Log.e("MAIN: ", error.getString("error"));

                                Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Log.e("MAIN: ", e.getMessage());
                                Log.e("MAIN: ", "No se pudo obtener el mensaje de error del JSON.");
                                e.printStackTrace();
                            }
                        }
                    }
                }

        );
    }

    public void mandarMetadatos(JSONObject metadatos){

        Log.i("MAIN: ", "Voy a mandar los metadatos con los nuevos usuarios compartidos.");

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        params.put("metadatos", metadatos.toString());

        Log.d("MAIN: ", "Ingrese los siguientes datos en los parametros del PUT: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");
        Log.d("MAIN: ", "El token ingresado es: \"" + token + "\".");
        Log.d("MAIN: ", "Los metadatos en formato JSON: \"" + metadatos.toString() + "\".");

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL_METADATOS + "\".");

        client.put(QUERY_URL_METADATOS, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, JSONObject jsonObject) {
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");
                Log.i("MAIN: ", "Se pudieron actualizar los usuarios compartidos exitosamente.");

                Toast.makeText(getApplicationContext(), "Archivo actualizado", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");

                if (error == null) {
                    Log.e("MAIN: ", "No se pudo comunicar con el servidor.");

                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        Log.e("MAIN: ", "Hubo un problema al mandar los metadatos con los usuarios compartidos actualizados.");
                        Log.e("MAIN: ", error.getString("error"));

                        Toast.makeText(getApplicationContext(), error.getString("error"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        Log.e("MAIN: ", e.getMessage());
                        Log.e("MAIN: ", "No se pudo obtener el mensaje de error del JSON.");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void avisarColision(){
        Log.i("MAIN: ", "Voy a crear un Alert Dialog avisando que hay una colision entre archivos");

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Advertencia!");
        alert.setMessage("Usted descargo la versión X y va a pisar a la ultima versión Y.\n" +
                "¿Esta seguro que desea sobreescribir el archivo?");

        // El boton Sobreescribir sube el archivo igual
        alert.setPositiveButton("Sobreescribir", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("MAIN: ", "Hice click en sobreescribir.");

            }
        });

        // Un boton para cancelar, que no hace nada (se cierra la ventana emergente)
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("MAIN: ", "Hice click en Cancelar.");
            }
        });

        alert.show();

        Log.i("MAIN: ", "Mostre un dialogo de advertencia.");
    }
}
