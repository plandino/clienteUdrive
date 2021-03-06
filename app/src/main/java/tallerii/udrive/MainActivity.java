package tallerii.udrive;

import android.app.FragmentManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

/**
 * Maneja la actividad principal de la aplicacion. Muestra las carpetas y archivos que tiene el usuario.
 * Se comunica con el servidor para subir y bajar archivos o carpetas.
 * Se usa para compartir archivos con otros usuarios.
 */
public class MainActivity extends AppCompatActivity implements FilesFragment.OnFragmentInteractionListener {

    FragmentManager fragmentManager;

    private String token = "";
    private String username = "";

    private String QUERY_CARPETAS = MyDataArrays.direccion + "/folder/";

    private String QUERY_URL_METADATOS;

    private String estructuraCarpetas;
    private JSONObject estructuraCarpetasJSON;
    private JSONObject usuariosParaCompartir;

    HashMap<String, String> URLArchivos;
    HashMap<String, String> extensionesArchivos;
    HashMap<String, String> propietariosArchivos;
    HashMap<String, Integer> versionesServidorArchivos;

    private TextView textoFallido;


    private static final int PICKFILE_RESULT_CODE = 1;
    private static final int METADATOS = 2;
    private static final int ACTUALIZAR_RESULT_CODE = 3;


    MyLocationListener locationListener;

    private ArrayAdapter<String> usuariosAdapter;

    AlertDialog.Builder alertCompartidos;

    SharedPreferences mSharedPreferences;

    ListView listaCompartidos;

    private JSONArray usuariosCompartidos;

    private boolean menuActualizado = false;
    private boolean yaObtuveLosUsuariosCompartidos = true;

    private MenuItem buscarNombre;
    private MenuItem buscarExtension;
    private MenuItem buscarEtiquetas;
    private MenuItem buscarPropietario;

    private SearchView searchView;

    private Menu menu;

    private String PATH_CARPETAS = "/";

    private String archivoAActualizar = "";

    private int idDescargado = 1;
    private int idSubido = 1;

    private TextView ubicacionActualTextView;


    /**
     * {@inheritDoc}
     */
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

        QUERY_CARPETAS += username + "/";

        Log.d("MAIN: ", "Forme el URL_CARPETAS: \"" + QUERY_CARPETAS + "\".");

        // Accedo a los datos guardados
        mSharedPreferences = getSharedPreferences(MyDataArrays.SESION_DATA, MODE_PRIVATE);


        // Con esto seteo la IP, con la que guarde al setearla, por si se rompe la aplicacion y se reinicia
        String ip = mSharedPreferences.getString(MyDataArrays.IP, MyDataArrays.direccion);
        MyDataArrays.setIP(ip);

        ubicacionActualTextView = (TextView) findViewById(R.id.vistaVacia);

        usuariosCompartidos = new JSONArray();

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

        Log.i("MAIN: ", "Se creo la MainActivity.");

        get(null);
    }

    @Override
    public void subirFAB(){
        seleccionarArchivo(false);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        // Este booleano se usa para avisar que modifique el menu cuando entro en la interfaz de busqueda de archivos.
        // Al inicio se carga el menu por defecto, asi que se encuentra actualizado.
        menuActualizado = true;

        agregarMenu(menu);

        return true;

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.menu = menu;
        return true;
    }

    /**
     * Agrega el menu principal a la aplicacion.
     * @param menu Menu principal de la aplicacion el cual es configurado.
     */
    public void agregarMenu(final Menu menu){

        getMenuInflater().inflate(R.menu.menu_main, menu);

        this.menu = menu;

        // Agrega al menu la barra de busqueda
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.buscar_archivo).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Le agrego un Listener para detectar cuando expando la barra de busqueda y cuando la cierro
        MenuItem menuItem = menu.findItem(R.id.buscar_archivo);

        MenuItemCompat.setOnActionExpandListener(menuItem, new MenuItemCompat.OnActionExpandListener() {

            /**
             * Se encarga de modificar el menu cuando detecta que se colapso la barra de busqueda
             * @param item item colapsado
             * @return siempre tiene que devolver un booleano igual a true
             */
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {

                FilesFragment filesFragment = (FilesFragment) getFragmentManager().findFragmentById(R.id.contenedor);
                filesFragment.mostrarFloatingButton();

                menu.removeItem(R.id.buscar_nombre);
                menu.removeItem(R.id.buscar_extension);
                menu.removeItem(R.id.buscar_etiqueta);
                menu.removeItem(R.id.buscar_propietario);

                menu.removeItem(R.id.buscar_archivo);

                // Se menuActualizado == false, es porque antes busque un archivo y se cambio el menu
                // Entonces el menu esta desactualizado y hay que cambiarlo.
                if (!menuActualizado) {
                    menuActualizado = true;
                    agregarMenu(menu);
                }

                get(null);
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {

                FilesFragment filesFragment = (FilesFragment) getFragmentManager().findFragmentById(R.id.contenedor);
                filesFragment.ocultarFloatingButton();

                menu.removeItem(R.id.subir_archivo);
                menu.removeItem(R.id.crear_carpeta);
                menu.removeItem(R.id.papelera);
                menu.removeItem(R.id.carpetas_compartidas);
                menu.removeItem(R.id.ver_perfil);
                menu.removeItem(R.id.cerrar_sesion);
                menu.removeItem(R.id.actualizar);

                getMenuInflater().inflate(R.menu.menu_busqueda, menu);

                buscarNombre = menu.findItem(R.id.buscar_nombre);
                buscarExtension = menu.findItem(R.id.buscar_extension);
                buscarEtiquetas = menu.findItem(R.id.buscar_etiqueta);
                buscarPropietario = menu.findItem(R.id.buscar_propietario);

                // Cambie al menu de busquedas, por lo que el menu esta desactualizado y cuando cierre
                // la interfaz de busquedas, tengo que volver a agregar el menu por defecto.
                menuActualizado = false;

                return true;  // Siempre devuelvo true
            }
        });
    }

    @Override
    public void startActivity(Intent intent) {

        Log.i("MAIN: ", "Voy a iniciar una nueva activity.");

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String busqueda = intent.getStringExtra(SearchManager.QUERY);
            buscarPorMetadatos(busqueda);
            Log.i("MAIN: ", "Me quedo en MainActivity porque es una busqueda.");
        } else {
            Log.i("MAIN: ", "Es una Activity diferente.");
            super.startActivity(intent);
        }
    }

    /**
     * Busca los archivos que concuerdan con la busqueda, en alguno de sus metadatos.
     * @param busqueda Busqueda a realizar.
     */
    public void buscarPorMetadatos(final String busqueda){

        Log.i("MAIN: ", "Voy a realizar una busqueda.");

        String QUERY_URL = MyDataArrays.direccion + "/metadata/" ;

        AsyncHttpClient client = new AsyncHttpClient();

        final RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        if(buscarNombre.isChecked()){
            Log.d("MAIN: ", "Busco por nombre: \"" + busqueda + "\".");
            params.put("nombre", busqueda);
        }
        if(buscarExtension.isChecked()){
            Log.d("MAIN: ", "Busco por extension: \"" + busqueda + "\".");
            params.put("extension", busqueda);

        }
        if(buscarEtiquetas.isChecked()){
            Log.d("MAIN: ", "Busco por etiquetas: \"" + busqueda + "\".");
            params.put("etiqueta", busqueda);

        }
        if(buscarPropietario.isChecked()){
            Log.d("MAIN: ", "Busco por propietario: \"" + busqueda + "\".");
            params.put("propietario", busqueda);
        }

        Log.d("MAIN: ", "Ingrese los siguientes datos en los parametros del GET: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");
        Log.d("MAIN: ", "El token ingresado es: \"" + token + "\".");

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL + "\".");

        client.get(QUERY_URL, params, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int status, JSONObject jsonObject) {
                        Log.i("MAIN: ", "Pude hacer la busqueda correctamente.");

                        if (jsonObject != null) {
                            try {
                                estructuraCarpetasJSON = jsonObject.getJSONObject("busqueda");
                                Log.d("MAIN: ", "La estructura de la busqueda es: \"" + estructuraCarpetasJSON.toString() + "\".");

                                guardarDatosEstructuraArchivos(estructuraCarpetasJSON);

                            } catch (JSONException e) {
                                Log.e("MAIN: ", e.getMessage());
                                Log.e("MAIN: ", "No pude obtener los datos del JSON de la busqueda de archivos.");
                                e.printStackTrace();
                            }
                            crearNuevoFragmento(estructuraCarpetasJSON.toString(), MyDataArrays.BUSQUEDA);
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
                                Log.e("MAIN: ", "Hubo un problema al obtener la estructura de la busqueda.");
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // Menu para el movimiento normal por carpetas y archivos
            case R.id.buscar_archivo:
                Log.i("MAIN: ", "Hice click en el boton de buscar archivo.");
                return true;
            case R.id.subir_archivo:
                Log.i("MAIN: ", "Hice click en el boton de subir archivo.");
                seleccionarArchivo(false);
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
            case R.id.atras:
                Log.i("MAIN: ", "Hice click en atras del menu hamburguesa.");
                onBackPressed();
                return true;
            case R.id.ver_perfil:
                Log.i("MAIN: ", "Hice click en el boton para ver el perfil.");
                pasarAVerPerfil();
                return true;
            case R.id.actualizar:
                if( menuActualizado){
                    get(null);
                }
                Toast.makeText(getApplicationContext(), "Carpeta actualizada", Toast.LENGTH_LONG).show();
                return true;
            case R.id.cerrar_sesion:
                Log.i("MAIN: ", "Hice click en el boton para cerrar sesión.");
                cerrarSesion();
                return true;

            case android.R.id.home:
                this.onBackPressed();
                return true;

            // Menu para las busquedas de archivos
            case R.id.buscar_nombre:
                buscarNombre.setChecked(true);
                buscarExtension.setChecked(false);
                buscarEtiquetas.setChecked(false);
                buscarPropietario.setChecked(false);
                Log.i("MAIN: ", "Cambie el estado de la busqueda por nombres de archivos.");
                return true;
            case R.id.buscar_extension:
                buscarNombre.setChecked(false);
                buscarExtension.setChecked(true);
                buscarEtiquetas.setChecked(false);
                buscarPropietario.setChecked(false);
                Log.i("MAIN: ", "Cambie el estado de la busqueda por extensiones de archivos.");
                return true;
            case R.id.buscar_etiqueta:
                buscarNombre.setChecked(false);
                buscarExtension.setChecked(false);
                buscarEtiquetas.setChecked(true);
                buscarPropietario.setChecked(false);
                Log.i("MAIN: ", "Cambie el estado de la busqueda por etiquetas de archivos.");
                return true;
            case R.id.buscar_propietario:
                buscarNombre.setChecked(false);
                buscarExtension.setChecked(false);
                buscarEtiquetas.setChecked(false);
                buscarPropietario.setChecked(true);
                Log.i("MAIN: ", "Cambie el estado de la busqueda por propietarios de archivos.");
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

                    String selectedFilePath;
                    Uri selectedImageUri = data.getData();

                    // Tengo que obtener el path del archivo. Uso el FilePathGetter para traducir
                    // de una Uri a un file path absoluto
                    selectedFilePath = FilePathGetter.getPath(getApplicationContext(), selectedImageUri);
                    Log.d("MAIN: ", "El path del archivo seleccionado es: " + selectedFilePath);

                    subir(selectedFilePath, MyDataArrays.SIN_FORZAR);
                }
                break;
            case ACTUALIZAR_RESULT_CODE:
                Log.i("MAIN: ", "El request fue buscar un archivo para actualizar.");

                if(resultCode==RESULT_OK){

                    Log.i("MAIN: ", "El resultado de la activity es OK.");

                    if (data == null){
                        Log.i("MAIN: ", "No hay data en el resultado de la activity.");
                        return;
                    }

                    String selectedFilePath;
                    Uri selectedImageUri = data.getData();

                    // Tengo que obtener el path del archivo. Uso el FilePathGetter para traducir
                    // de una Uri a un file path absoluto
                    selectedFilePath = FilePathGetter.getPath(getApplicationContext(), selectedImageUri);
                    Log.d("MAIN: ", "El path del archivo seleccionado es: " + selectedFilePath);

                    int index = selectedFilePath.lastIndexOf(".");
                    String extensionNuevoArchivo = selectedFilePath.substring(index + 1);
                    String archivoViejo = archivoAActualizar.replace(MyDataArrays.caracterReemplazaEspacios, " ");
                    String extensionViejoArchivo = obtenerExtensionArchivo(archivoViejo);

                    Log.d("MAIN: ", "Extension vieja: " + extensionViejoArchivo);
                    Log.d("MAIN: ", "Extension nuevo: " + extensionNuevoArchivo);


                    if(extensionNuevoArchivo.equals(extensionViejoArchivo)){
                        Log.d("MAIN: ", "subo normal");

                        subir(selectedFilePath, MyDataArrays.SIN_FORZAR);
                    } else {
                        Log.d("MAIN: ", "subo avisando");

                        avisarExtensionesDiferentes(selectedFilePath);
                    }

                }
                break;
            case METADATOS:
                Log.i("MAIN: ", "El request fue acceder a los metadatos.");

                if((resultCode==RESULT_OK) && (menuActualizado)){
                    FilesFragment filesFragment = (FilesFragment) getFragmentManager().findFragmentById(R.id.contenedor);
                    filesFragment.colapsarFragmentos();
                    Log.i("MAIN: ", "El resultado de la activity es OK.");
                    get(null);
                } else if( !menuActualizado){
                    searchView.clearFocus();
                }

        }
    }

    /**
     * Muestra un AlertDialog avisando al usuario, que el archivo nuevo que quiere subir tiene una extension
     * diferente a la original.
     */
    private void avisarExtensionesDiferentes(final String path){
        Log.i("MAIN: ", "Voy a crear un Alert Dialog avisando que hay una colision entre extensiones de archivos");

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Advertencia!");
        alert.setMessage("La extensión del archivo que quiere subir es diferente a la original, " +
                "esto puede corromper el archivo. \n ¿Está seguro que quiere reemplazar el archivo?");

        // El boton Sobreescribir sube el archivo igual
        alert.setPositiveButton("Sobreescribir", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("MAIN: ", "Hice click en sobreescribir.");
                subir(path, MyDataArrays.SIN_FORZAR);
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

    /**
     * Obtiene la estructura interna de la carpeta pasada por parametro, o de la cual se encuentra actualmente
     * parado en caso de que se le pase un null.
     * @param id nombre de la carpeta, de la cual se quiere obtener la estructura interna.
     *           Si es un null, se obtiene la estructura interna de la carpeta en la cual se encuentra
     *           actualmente parado el usuario.
     */
    private void get(String id) {
        Log.d("MAIN: ", "Voy a obtener la estrcutra interna de la carpeta: \"" + id + "\".");

        if(id != null){
            if(id.equals(MyDataArrays.caracterReservado + "permisos")){
                PATH_CARPETAS = "/" + id + "/";
                QUERY_CARPETAS =  MyDataArrays.direccion + "/folder"  + PATH_CARPETAS +  username ;
            } else if(id.equals(MyDataArrays.caracterReservado + "trash")){
                PATH_CARPETAS = "/" + id + "/";
                QUERY_CARPETAS =  MyDataArrays.direccion + "/folder/" + username + PATH_CARPETAS;
            } else {
                PATH_CARPETAS += id + "/";
                QUERY_CARPETAS += id + "/";
            }
        } else {
            // Al hacer un get(null) actualizo el path actual
            // Depende de si vuelvo de permisos o no, es el orden del PATH y username
            if(PATH_CARPETAS.contains("permisos")){
                QUERY_CARPETAS = MyDataArrays.direccion + "/folder/" + PATH_CARPETAS + username;
            } else {
                QUERY_CARPETAS = MyDataArrays.direccion + "/folder/" + username + PATH_CARPETAS;
            }
        }

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);

        Log.d("MAIN: ", "El URL al que le pego es: \"" + QUERY_CARPETAS + "\".");


        client.get(QUERY_CARPETAS, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                Log.i("MAIN: ", "Exito al obtener la estructura interna de la carpeta.");

                try {
                    estructuraCarpetas = jsonObject.getString("estructura");
                    estructuraCarpetas = estructuraCarpetas.replace(MyDataArrays.caracterReemplazaEspacios, " ");
                    estructuraCarpetasJSON = new JSONObject(estructuraCarpetas);
                    Log.d("MAIN: ", "La estructura de la carpeta en formato String: \"" + estructuraCarpetas + "\".");

                    guardarDatosEstructuraArchivos(estructuraCarpetasJSON);

                } catch (JSONException e) {
                    Log.e("MAIN: ", e.getMessage());
                    Log.e("MAIN: ", "No pude obtener los datos del JSON.");
                    e.printStackTrace();
                }
                crearNuevoFragmento(estructuraCarpetasJSON.toString(), MyDataArrays.CARPETA);
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

                yaObtuveLosUsuariosCompartidos = true;

                try {
                    usuariosParaCompartir = jsonObject.getJSONObject("busqueda");
                    Log.d("MAIN: ", "Los usuarios que concuerdan con la busqueda en formato JSON son: \"" + usuariosParaCompartir.toString() + "\".");

                    String usuarios = usuariosParaCompartir.getString("usuarios");
                    Log.d("MAIN: ", "Los usuarios que concuerdan con la busqueda en formato STRING son: \"" + usuarios + "\".");

                    if (usuarios.length() > 0) {
                        Log.d("MAIN: ", "Agrego los usuarios a la lista.");

                        textoFallido.setVisibility(View.INVISIBLE);
                        String[] partes = usuarios.split(Character.toString(MyDataArrays.caracterReservado));
                        String nuevo = usuarios.replaceAll(String.valueOf(MyDataArrays.caracterReservado), "\",\"");
                        Log.d("MAIN: ", "El String para mostrar los usuarios para compartir es: \"" + nuevo + "\".");

                        // Todos los usuarios que devuelve la busqueda, los agrego al adapter de la lista,
                        // menos a mi mismo, porque no tiene sentido mostrarme
                        int j = 0;
                        for (String user : partes) {

                            if (!user.equals(username)) {
                                usuariosAdapter.add(user);
                                listaCompartidos.setItemChecked(j, false);


                                Log.d("MAIN: ", "usuarios compartidos: \"" + usuariosCompartidos.toString() + "\".");

                                int cant = usuariosCompartidos.length();
                                Log.d("MAIN: ", "cant: \"" + cant + "\".");

                                for (int i = 0; i < cant; i++) {
                                    try {

                                        String usuario = usuariosCompartidos.getString(i);
                                        Log.d("MAIN: ", "El usuario revisado es: \"" + usuario + "\".");
                                        Log.d("MAIN: ", "El usuario agregado es: \"" + usuario + "\".");


                                        if (usuario.equals(user)) {
                                            listaCompartidos.setItemChecked(j, true);
                                            Log.d("MAIN: ", "Puse en true el tilde.");

                                        }
                                    } catch (JSONException e) {
                                        Log.e("MAIN: ", e.getMessage());
                                        Log.e("MAIN: ", "Error al sacar al marcar los ticks en los usuarios ya compartidos.");
                                        e.printStackTrace();
                                    }
                                }
                            }
                            j++;
                        }
                        Log.i("MAIN: ", "Agregue todos los usuarios a la lista.");

                    } else {
                        Log.i("MAIN: ", "Ningun usuario concuerda con la busqueda.");
                        textoFallido.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    Log.e("MAIN: ", e.getMessage());
                    Log.e("MAIN: ", "No se pudo obtener los datos del JSON.");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {

                yaObtuveLosUsuariosCompartidos = true;

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

    @Override
    public void onGroupClick(String idClick) {

        idClick = idClick.replace(MyDataArrays.caracterReemplazaEspacios, " ");
        Log.d("MAIN: ", "Hice click sobre: \"" + idClick + "\".");

        String tipoDeArchivo = obtenerExtensionArchivo(idClick);

        Log.d("MAIN: ", "El tipo de archivo es: \"" + tipoDeArchivo + "\".");

        if(tipoDeArchivo.equals(MyDataArrays.caracterReservado + "folder")) {
            Log.i("MAIN: ", "Accedo y obtengo la estructura interna de la carpeta.");
            idClick = idClick.replace(" ", MyDataArrays.caracterReemplazaEspacios);
            get(idClick);
        } else {
            Log.i("MAIN: ", "Obtengo los metadatos del archivo.");
            // Solo se puede acceder a los metadatos si el archivo no esta en la papelera
            if( ! PATH_CARPETAS.contains("trash")){
                obtenerMetadatos(idClick);
            } else {
                Toast.makeText(getApplicationContext(), "No puede acceder a los datos del archivo desde la papelera", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * {@inheritDoc}
     * Actualiza la vista de la carpeta cuando detecta que el usuario deslizo el dedo hacia abajo.
     */
    @Override
    public void onDownScroll(){
        if( menuActualizado){
            get(null);
        }
        Toast.makeText(getApplicationContext(), "Carpeta actualizada", Toast.LENGTH_LONG).show();
    }

    /**
     * Crea un nuevo fragmento para mostrar la estructura de la carpeta seleccionada.
     * @param estructuraCarpetas estructura interna de la carpeta que se desea mostrar.
     */
    public void crearNuevoFragmento(String estructuraCarpetas, String tipo){
        Log.i("MAIN: ", "Voy a crear un nuevo fragmento.");
        Log.d("MAIN: ", "La estructura a usar es: \"" + estructuraCarpetas + "\".");

        // Si el path actual es == "/" no muestro la flecha para atras en el action bar
        // Si el path es mas grande, si la muestro
        if(PATH_CARPETAS.length() > 1){
            mostrarFlechaParaAtras(true);
        } else {
            mostrarFlechaParaAtras(false);
        }


        // Aca recibo desde el fragmento, la carpeta que seleccione y tengo que pedirle al servidor
        // que me devuelva la estructura de carpetas y archivos que estan adentro de esa.
        // Luego creo un nuevo fragmento con esa estructura y reemplazo el anterior

        // Si estoy en la vista de navegar por las carpetas, pongo un textview diciendo donde estoy ubicado
        if(tipo.equals(MyDataArrays.CARPETA)){
            ubicacionActualTextView.setVisibility(View.VISIBLE);
            String textoUbicacion;

            // Si estoy en la papelera
            if(PATH_CARPETAS.contains(MyDataArrays.caracterReservado + "trash")){
                textoUbicacion = "Ubicación: /papelera/";
                tipo = MyDataArrays.PAPELERA;

                this.menu.findItem(R.id.crear_carpeta).setVisible(false);
                this.menu.findItem(R.id.subir_archivo).setVisible(false);
                this.menu.findItem(R.id.papelera).setVisible(false);
                this.menu.findItem(R.id.carpetas_compartidas).setVisible(true);
                this.menu.findItem(R.id.atras).setVisible(true);

                // Si estoy en los archivos compartidos
            } else if(PATH_CARPETAS.contains(MyDataArrays.caracterReservado + "permisos")){
                textoUbicacion = "Ubicación: /compartidos/";
                tipo = MyDataArrays.COMPARTIDOS;

                this.menu.findItem(R.id.carpetas_compartidas).setVisible(false);
                this.menu.findItem(R.id.crear_carpeta).setVisible(false);
                this.menu.findItem(R.id.subir_archivo).setVisible(false);
                this.menu.findItem(R.id.papelera).setVisible(true);
                this.menu.findItem(R.id.atras).setVisible(true);

            } else {
                String path = PATH_CARPETAS.replace(MyDataArrays.caracterReemplazaEspacios, " ");
                textoUbicacion = "Ubicación: " + path;
            }
            ubicacionActualTextView.setText(textoUbicacion);
        } else {
            // Si estoy en busquedas, no muestro la ubicacion
            ubicacionActualTextView.setVisibility(View.INVISIBLE);
        }

        //Paso 3: Creo un nuevo fragmento
        FilesFragment fragment = new FilesFragment();

        // Creo un Bundle y le agrego informacion del tipo <Key, Value>
        Bundle bundle = new Bundle();
        bundle.putString("estructura", estructuraCarpetas);
        bundle.putString("tipo", tipo);
        bundle.putString("username", username);

        // Le agrego el Bundle al fragmento
        fragment.setArguments(bundle);

        //Paso 2: Crear una nueva transacción
        fragmentManager.beginTransaction().replace(R.id.contenedor, fragment).commit();
    }

    @Override
    public void onOptionClick(String idPadre, String opcion) {

        idPadre = idPadre.replace(MyDataArrays.folderExtension, "");

        if(opcion.equals("Descargar")){
            Log.i("MAIN: ", "Hice click en la opcion DESCARGAR.");
            descargarArchivo(idPadre, obtenerNumeroVersionServidorArchivo(idPadre));
        }

        if(opcion.equals("Versiones anteriores")){
            Log.i("MAIN: ", "Hice click en la opcion VERSIONES ANTERIORES.");
            mostrarVersionesAnteriores(idPadre);
        }

        if(opcion.equals("Actualizar")){
            Log.i("MAIN: ", "Hice click en la opcion Actualizar.");
            archivoAActualizar = idPadre.replace(" ", MyDataArrays.caracterReemplazaEspacios);
            seleccionarArchivo(true);
        }

        if(opcion.equals("Compartir")){
            Log.i("MAIN: ", "Hice click en la opcion COMPARTIR.");
            compartirArchivo(idPadre);
        }

        if(opcion.equals("Eliminar")){
            Log.i("MAIN: ", "Hice click en la opcion ELIMINAR.");
            confirmarEliminarORestaurar(idPadre, MyDataArrays.ELIMINAR);
        }

        if(opcion.equals("Restaurar")){
            Log.i("MAIN: ", "Hice click en la opcion RESTAURAR.");
            confirmarEliminarORestaurar(idPadre, MyDataArrays.RESTAURAR);
        }
    }

    /**
     * Sube un archivo al servidor.
     * @param path path interno del telefono, al archivo a subir.
     */
    private void subir(final String path, boolean forzar){

        String pathSinEspacios = path.replace(" ", MyDataArrays.caracterReemplazaEspacios);

        Log.i("MAIN: ", "Voy a subir un archivo.");

        int index = pathSinEspacios.lastIndexOf("/");
        final String filename;

        if(PATH_CARPETAS.contains(MyDataArrays.caracterReservado + "permisos")){
            filename = archivoAActualizar;
        } else {
            filename = pathSinEspacios.substring(index + 1);
        }


        final int numeroVersionDescargado;
        if(forzar){
            numeroVersionDescargado = obtenerNumeroVersionServidorArchivo(filename) + 1;
            Log.d("MAIN: ", "Mi numero de version a subir, forzando: \"" + numeroVersionDescargado + "\".");

        } else {
            numeroVersionDescargado = obtenerUltimoNumeroDeVersionDescargado(PATH_CARPETAS + filename) + 1;
            Log.d("MAIN: ", "Mi numero de version a subir, sin forzar: \"" + numeroVersionDescargado + "\".");

        }

        String QUERY_URL = MyDataArrays.direccion + "/file/" + username +  PATH_CARPETAS + filename + MyDataArrays.caracterReservado + numeroVersionDescargado;

        if(PATH_CARPETAS.contains(MyDataArrays.caracterReservado + "permisos") || ( !archivoAActualizar.equals(""))){
            String archivoViejo = archivoAActualizar.replace(MyDataArrays.caracterReemplazaEspacios, " ");
            QUERY_URL = MyDataArrays.direccion + "/file/" + obtenerURLArchivo(archivoViejo) + MyDataArrays.caracterReservado + numeroVersionDescargado;
        }


        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        double latitud = locationListener.getLatitud();
        double longitud = locationListener.getLongitud();
        params.put("latitud", String.valueOf(latitud));
        params.put("longitud", String.valueOf(longitud));
        if(forzar) params.put("force", "true");

        Log.d("MAIN: ", "Voy a ingresar los siguientes datos en los parametros del PUT: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");
        Log.d("MAIN: ", "El token ingresado es: \"" + token + "\".");
        Log.d("MAIN: ", "Latitud: \"" + latitud + "\" || Longitud: \"" + longitud + "\".");
        Log.d("MAIN: ", "Force: \"" + forzar + "\".");

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


        idSubido += 1;

        final int myIdSubido = idSubido;

        final NotificationCompat.Builder mBuilderSubidaExitosa = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle("Subida completada")
                .setContentText("Se subio correctamente el archivo: " + filename);

        final NotificationCompat.Builder mBuilderSubidaFallida = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.ic_delete)
                .setContentTitle("Subida fallida")
                .setContentText("Error al subir el archivo: " + filename);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_sys_upload)
                        .setContentTitle("Subiendo...")
                        .setContentText("Subiendo archivo: " + filename)
                        .setProgress(100, 0, true)
                        .setOngoing(true);


        final NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(myIdSubido, mBuilder.build());

        Toast.makeText(getApplicationContext(), "Subiendo archivo", Toast.LENGTH_LONG).show();

        client.put(QUERY_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, JSONObject jsonObject) {

                mNotificationManager.cancel(myIdSubido);

                mNotificationManager.notify(myIdSubido, mBuilderSubidaExitosa.build());

                archivoAActualizar = "";

                guardarVersionArchivo(PATH_CARPETAS + filename, numeroVersionDescargado);

                Log.i("MAIN: ", "Exito al subir un archivo.");
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");

                Toast.makeText(getApplicationContext(), "Archivo subido", Toast.LENGTH_LONG).show();
                // Luego de subir un archivo actualizo la estructura de la carpeta, si no estoy en busquedas.
                if(menuActualizado)  get(null);
                else searchView.clearFocus();
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {

                mNotificationManager.cancel(myIdSubido);

                searchView.clearFocus();

                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");

                if (error == null) {

                    mNotificationManager.notify(myIdSubido, mBuilderSubidaFallida.build());

                    Log.e("MAIN: ", "No se pudo comunicar con el servidor.");

                    Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                } else {
                    try {

                        String ultimaVersion = error.getString("ultima version");
                        Log.e("MAIN: ", "Ultima version: \"" + ultimaVersion + "\".");

                        avisarColision(path);

                        Log.e("MAIN: ", "Hubo un problema al subir un archivo.");
                        Log.e("MAIN: ", error.getString("error"));

                        // Si el error es por conflicto de versiones no tengo que mostrar este mensaje de error
                        if(statusCode != MyDataArrays.CONFLICT)
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
     * Crea un menu para seleccionar que archivo subir.
     */
    private void seleccionarArchivo(boolean actualizar){

        Log.i("MAIN: ", "Voy a seleccionar un archivo para subir.");

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        String archivoViejo = archivoAActualizar.replace(MyDataArrays.caracterReemplazaEspacios, " ");
        String extension = obtenerExtensionArchivo(archivoViejo);
        if(extension == null){
            extension = "";
        }
        String filtroArchivo = "*";
        if (extension.equals("gif") || extension.equals("jpg") || extension.equals("png") || extension.equals("jpeg")) {
            filtroArchivo =  "image";
        } else if(extension.equals("mp4") || extension.equals("mpeg") || extension.equals("aac") || extension.equals("wav") || extension.equals("ogg") || extension.equals("midi") || extension.equals("mp3") || extension.equals("mp5")){
            filtroArchivo = "audio";
        } else if(extension.equals("xml") || extension.equals("txt") || extension.equals("cfg") || extension.equals("csv") || extension.equals("conf") || extension.equals("rc") || extension.equals("html") || extension.equals("htm")){
            filtroArchivo = "text";
        } else if(extension.equals("pdf") || extension.equals("apk") ){
            filtroArchivo = "application";
        }

        Log.i("MAIN: ", "Filtro el archivo por: \"" + filtroArchivo + "\".");


        if(actualizar){
            intent.setType(filtroArchivo + "/*");
            startActivityForResult(intent, ACTUALIZAR_RESULT_CODE);
        } else {
            intent.setType("*/*");
            startActivityForResult(intent, PICKFILE_RESULT_CODE);
        }

    }

    /**
     *
     * @param nombreArchivo
     */
    private void descargarArchivo(final String nombreArchivo, final int numeroVersion){

        Log.i("MAIN: ", "Voy a descargar un archivo.");
        Log.d("MAIN: ", "Descargo el archivo: \"" + nombreArchivo + "\" en su version: \"" + numeroVersion + "\".");

        String QUERY_URL = MyDataArrays.direccion + "/file/" +  obtenerURLArchivo(nombreArchivo) + MyDataArrays.caracterReservado + numeroVersion;

        QUERY_URL = QUERY_URL.replace(" ", MyDataArrays.caracterReemplazaEspacios);

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);

        Log.d("MAIN: ", "Voy a ingresar los siguientes datos en los parametros del GET: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");
        Log.d("MAIN: ", "El token ingresado es: \"" + token + "\".");

        final String savedFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + nombreArchivo;

        Log.d("MAIN: ", "Guardo el archivo en el path: \"" + savedFilePath + "\".");

        File file = new File(savedFilePath);

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL + "\".");

        idDescargado += 1;

        final int myIdDescargado = idDescargado;

        final NotificationCompat.Builder mBuilderDescargaExitosa = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Descarga completada")
                .setContentText("Se descargo correctamente el archivo: " + nombreArchivo);

        final NotificationCompat.Builder mBuilderDescargaFallida = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.ic_delete)
                .setContentTitle("Descarga fallida")
                .setContentText("Error al subir el archivo: " + nombreArchivo);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Descargando")
                .setContentText("Descargando archivo: " + nombreArchivo)
                .setProgress(100, 0, true)
                .setOngoing(true);

        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(myIdDescargado, mBuilder.build());

        Toast.makeText(getApplicationContext(), "Descargando archivo", Toast.LENGTH_LONG).show();

        client.get(QUERY_URL, params, new FileAsyncHttpResponseHandler(file) {

                    @Override
                    public void onSuccess(File file) {

                        mNotificationManager.cancel(myIdDescargado);

                        mNotificationManager.notify(myIdDescargado, mBuilderDescargaExitosa.build());

                        Toast.makeText(getApplicationContext(), "Se ha descargado el archivo exitosamente!", Toast.LENGTH_LONG).show();

                        guardarVersionArchivo(PATH_CARPETAS + nombreArchivo, numeroVersion);

                        Log.i("MAIN: ", "Exito al descarga el archivo.");

                        String extension = obtenerExtensionArchivo(nombreArchivo);
                        Log.d("MAIN: ", "La extension del archivo es: \"" + extension + "\".");

                        if (extension.equals("txt") || extension.equals("jpg") || extension.equals("png") || extension.equals("jpeg")) {

                            Log.i("MAIN: ", "Voy a visualizar el archivo.");

                            // Cuando la descarga es una foto o un archivo de texto, cambio a otra
                            // activity para ver el archivo
                            Intent intentDisplay = new Intent(MainActivity.this, DisplayActivity.class);

                            intentDisplay.putExtra("extensionArchivo", extension);
                            intentDisplay.putExtra("filePath", savedFilePath);
                            intentDisplay.putExtra("nombreArchivo", nombreArchivo);

                            Log.d("MAIN: ", "Le agrego al Intent la extension: \"" + extension + "\".");
                            Log.d("MAIN: ", "Le agrego al Intent el path al archivo guardado: \"" + savedFilePath + "\".");
                            Log.d("MAIN: ", "Le agrego al Intent el nombre: \"" + nombreArchivo + "\".");

                            startActivity(intentDisplay);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Throwable e, File response) {

                        mNotificationManager.cancel(myIdDescargado);

                        mNotificationManager.notify(myIdDescargado, mBuilderDescargaFallida.build());


                        Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");
                        Log.e("MAIN: ", "Hubo un problema al descargar un archivo.");

                        Toast.makeText(getApplicationContext(), "No se pudo descargar el archivo.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /**
     * Muestra un AlertDialog para que el usuario ingrese el nombre de la nueva carpeta a crear.
     * Al hacer click en Cancelar, se cierra la ventana y no se hace nada.
     * Al hacer click en Crear, se manda a crear la nueva carpeta al servidor.
     */
    private void crearCarpeta(){

        Log.i("MAIN: ", "Voy a crear una carpeta.");

        // Muestro una ventana emergente para que introduzca el nombre de la carpeta a crear
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Crear carpeta");
        alert.setMessage("Introduzca el nombre de la nueva carpeta");

        final EditText nombreCarpeta = new EditText(this);

        // Luego creo un LinearLayout y los configuro para ordenar todos los items anteriores y
        // mostrarlos bien en el AlertDialog TODO: unificar esto
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        float pxTodp = 300 / getResources().getDisplayMetrics().density;
        layout.setPadding((int) pxTodp, 0, (int) pxTodp, 0);

        layout.addView(nombreCarpeta);
        alert.setView(layout);

        // El boton "Crear" crea la carpeta
        alert.setPositiveButton("Crear", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("MAIN: ", "Hice click en Crear. El nombre introducido es: \"" + nombreCarpeta.getText().toString() + "\".");
                String nombreCarpetaString = nombreCarpeta.getText().toString();
                nombreCarpetaString = nombreCarpetaString.replace(" ", MyDataArrays.caracterReemplazaEspacios);
                agregarCarpeta(nombreCarpetaString);
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

    /**
     * Manda a crear al servidor, la nueva carpeta, con el nombre: nombreCarpeta.
     * @param nombreCarpeta nombre usado para crear la nueva carpeta en el servidor.
     */
    private void agregarCarpeta(String nombreCarpeta){

        Log.i("MAIN: ", "Voy a mandar la carpeta creada al servidor.");

        String QUERY_URL = MyDataArrays.direccion + "/folder/" + username + PATH_CARPETAS + nombreCarpeta;

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

    /**
     *
     * @param id
     * @param restaurar
     */
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
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("MAIN: ", "Hice click en cancelar.");
            }
        });

        alert.show();

        Log.i("MAIN: ", "Mostre un Alert Dialog para confirmar que quiero eliminar o restaurar un archivo.");
    }

    /**
     * Manda un DELETE al servidor, para eliminar (permanentemente o no) o restaurar (desde la papelera) un archivo.
     * @param nombreArchivo el nombre del archivo el cual se quiere eliminar o restaurar
     * @param restore es un booleano que en caso de ser true, indica que quiere restaurar un archivo desde la papelera.
     *                Si es un false, indica que se quiere borrar un archivo.
     */
    private void eliminarORestaurar(final String nombreArchivo, boolean restore){

        if(restore){
            Log.i("MAIN: ", "Voy a mandar restaurar un archivo de la papelera en el servidor.");
        } else {
            Log.i("MAIN: ", "Voy a eliminar un archivo.");
        }

        AsyncHttpClient client = new AsyncHttpClient();

        // El delete pide que le pase Headers, le pongo basura
        Header[] header = {
                new BasicHeader("Content-type", "application/x-www-form-urlencoded")
                ,new BasicHeader("Content-type", "application/x-www-form-urlencoded")
                ,new BasicHeader("Accep", "text/html,text/xml,application/xml")
                ,new BasicHeader("Connection", "keep-alive")
                ,new BasicHeader("keep-alive", "115")
                ,new BasicHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2")
        };

        // Agrego los parametros
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

        Log.d("MAIN: ", "Ingrese los siguientes datos en los parametros del DELETE: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");
        Log.d("MAIN: ", "El token ingresado es: \"" + token + "\".");
        Log.d("MAIN: ", "Quiero restaurar: \"" + restaurar + "\".");

        String QUERY_URL;
        String extension = obtenerExtensionArchivo(nombreArchivo);

        if(extension.equals(MyDataArrays.caracterReservado + "folder")){
            QUERY_URL = MyDataArrays.direccion + "/folder/";
        } else {
            QUERY_URL = MyDataArrays.direccion + "/file/";
        }

        QUERY_URL += obtenerURLArchivo(nombreArchivo);

        QUERY_URL = QUERY_URL.replace(" ", MyDataArrays.caracterReemplazaEspacios);

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL + "\".");

        client.delete(getApplicationContext(), QUERY_URL, header, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (restaurar.equals("true")) {
                    Log.i("MAIN: ", "Se ha restaurado: \"" + nombreArchivo + "\" exitosamente.");
                    Toast.makeText(getApplicationContext(), "Se ha restaurado " + nombreArchivo, Toast.LENGTH_LONG).show();
                } else {
                    Log.i("MAIN: ", "Se ha eliminado: \"" + nombreArchivo + "\" exitosamente.");
                    Toast.makeText(getApplicationContext(), "Se ha eliminado " + nombreArchivo, Toast.LENGTH_LONG).show();
                }
                // Luego actualizo la vista
                if( menuActualizado) get(null);
                else searchView.clearFocus();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");
                if (restaurar.equals("true")) {
                    Log.e("MAIN: ", "Hubo un problema al restaurar: \"" + nombreArchivo + "\".");
                    Toast.makeText(getApplicationContext(), "Hubo un problema al restaurar: \"" + nombreArchivo + "\".", Toast.LENGTH_LONG).show();
                } else {
                    Log.e("MAIN: ", "Hubo un problema al eliminar: \"" + nombreArchivo + "\".");
                    Toast.makeText(getApplicationContext(), "Hubo un problema al eliminar: \"" + nombreArchivo + "\".", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Borra de los datos privados de la aplicacion, los datos de la sesion guardada.
     * Luego se comunica con el servidor para indicarle que se cierra esta sesion. Al recibir confirmacion
     * del servidor, se pasa a la ElegirSesionActivity para poder conectarse con otro usuario.
     */
    private void cerrarSesion(){

        Log.i("MAIN: ", "Voy a cerrar sesion.");

        // Borro de los datos privados de la aplicacion, los datos de sesion del usuario
        borrarDatosUsuario();

        AsyncHttpClient client = new AsyncHttpClient();

        // El delete pide que le pase Headers, le pongo basura
        Header[] header = {
                new BasicHeader("Content-type", "application/x-www-form-urlencoded")
                ,new BasicHeader("Content-type", "application/x-www-form-urlencoded")
                ,new BasicHeader("Accep", "text/html,text/xml,application/xml")
                ,new BasicHeader("Connection", "keep-alive")
                ,new BasicHeader("keep-alive", "115")
                , new BasicHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2")
        };

        RequestParams params = new RequestParams();
        params.put("user", username);

        Log.d("MAIN: ", "Ingrese los siguientes datos en los parametros del DELETE: ");
        Log.d("MAIN: ", "El username ingresado es: \"" + username + "\".");

        String QUERY_URL = MyDataArrays.direccion + "/session/" + token;

        Log.d("MAIN: ", "Le pego a la URL: \"" + QUERY_URL + "\".");

        Toast.makeText(getApplicationContext(), "Cerrando sesión...", Toast.LENGTH_LONG).show();

        client.delete(getApplicationContext(), QUERY_URL, header, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");
                Log.i("MAIN: ", "Sesion cerrada correctamente.");

                Toast.makeText(getApplicationContext(), "Sesión cerrada", Toast.LENGTH_LONG).show();

                pasarAElegir();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("MAIN: ", "StatusCode: \"" + statusCode + "\".");
                Log.e("MAIN: ", "Hubo un problema al cerrar sesion.");
                Toast.makeText(getApplicationContext(), "Hubo un problema al cerrar sesion", Toast.LENGTH_LONG).show();

                pasarAElegir();
            }
        });
    }

    /**
     *
     */
    private void borrarDatosUsuario(){

        Log.i("MAIN: ", "Voy a borrar los datos de usuario.");

        // Guardo en la memoria, el username y el token
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.remove(MyDataArrays.USERNAME);
        e.remove(MyDataArrays.TOKEN);
        e.apply();

        Log.i("MAIN: ", "Pude borrar los datos de usuario.");

    }

    /**
     *
     * @param nombreArchivo
     */
    private void obtenerMetadatos(String nombreArchivo){

        String extension = obtenerExtensionArchivo(nombreArchivo);
        if( ! extension.equals("") )
            extension = "." + extension;

        // Creo un Intent para pasar a la activity de mostrar metadatos
        Intent metadatosIntent = new Intent(this, MetadatosActivity.class);

        // Agrego la informacion que quiero al Intent
        metadatosIntent.putExtra("token", token);
        metadatosIntent.putExtra("username", username);
        String url = obtenerURLArchivo(nombreArchivo);
        url = url.replace(" ", MyDataArrays.caracterReemplazaEspacios);
        metadatosIntent.putExtra("pathArchivo", url);

        // Le tengo que mandar el propietario ademas, si estoy en la carpeta de archivos compartidos
        // el propietario no soy yo
        if(PATH_CARPETAS.contains("permisos")){
            metadatosIntent.putExtra("propietario", obtenerPropietarioArchivo(nombreArchivo));
        } else {
            metadatosIntent.putExtra("propietario", username);
        }

        // Inicio la actividad con el Intent
        startActivityForResult(metadatosIntent, METADATOS);
    }

    // Este metodo es para volver para atras en los fragmentos
    // y cerrar la aplicacion cuando volvi al inicio de todo
    @Override
    public void onBackPressed() {

        Log.i("MAIN: ", "Hice click en el boton para atras.");
        Log.d("MAIN: ", "Mi PATH_CARPETAS actual es: \"" + PATH_CARPETAS + "\".");

        if(PATH_CARPETAS.contains("trash")){
            Log.i("MAIN: ", "Saco el boton de atras y pongo el de papelera de vuelta.");

            this.menu.findItem(R.id.atras).setVisible(false);

            this.menu.findItem(R.id.papelera).setVisible(true);
            this.menu.findItem(R.id.crear_carpeta).setVisible(true);
            this.menu.findItem(R.id.subir_archivo).setVisible(true);

        } else if( PATH_CARPETAS.contains("permisos")){
            Log.i("MAIN: ", "Saco el boton de atras y pongo el de archivos compartidos de vuelta.");

            this.menu.findItem(R.id.atras).setVisible(false);

            this.menu.findItem(R.id.carpetas_compartidas).setVisible(true);
            this.menu.findItem(R.id.papelera).setVisible(true);
            this.menu.findItem(R.id.crear_carpeta).setVisible(true);
            this.menu.findItem(R.id.subir_archivo).setVisible(true);
        }

        if(PATH_CARPETAS.equals("/")){
            Log.i("MAIN: ", "No queda volver mas para atras asi que cierro la aplicacion.");
            getFragmentManager().popBackStack();
            this.finish();
        } else {
            /**
             * Como el path de las carpetas es de la siguiente forma:
             * Ej: /carpetaUno/carpetaDos/carpetaTres/
             *
             * Yo tengo que borrar dos veces para borrar la ultima barra y lo que queda despues de la anteultima.
             * Al volver para atras quiero que el path quede asi:
             * Ej: /carpetaUno/carpetaDos/
             *
             * Por eso en el segundo substring, se agarra hasta index + 1, para agarrar la ultima barra "/".
             * */
            int index = PATH_CARPETAS.lastIndexOf("/");
            PATH_CARPETAS = PATH_CARPETAS.substring(0, index);

            index = PATH_CARPETAS.lastIndexOf("/");
            PATH_CARPETAS = PATH_CARPETAS.substring(0, index + 1);
            Log.d("MAIN: ", "Mi PATH_CARPETAS actual es: \"" + PATH_CARPETAS + "\".");

            // Luego de actualizar el path, actualizo
            get(null);
        }
    }

    /**
     * Muestra u oculta la flecha para ir para atras en el ActionBar
     * @param mostrar Si es true, muestra la flecha, si es false, la oculta
     */
    private void mostrarFlechaParaAtras(boolean mostrar){
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(mostrar);
        }
    }

    /**
     * Este metodo sirve para ir a la activity de iniciar sesion o registrarse
     */
    private void pasarAElegir(){

        // Creo un Intent para pasar a la activity de elegir para iniciar sesion o registrarse
        Intent elegirIntent = new Intent(this, ElegirSesionActivity.class);

        // Inicio la actividad con el Intent
        startActivity(elegirIntent);
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    /**
     * Cambia a la PerfilActivity.
     */
    private void pasarAVerPerfil(){

        Log.i("MAIN: ", "Creo un Intent para cambiar a la PerfilActivity.");

        // Creo un Intent para pasar a la activity de ver y actualizar perfil
        Intent perfilIntent = new Intent(this, PerfilActivity.class);

        // Agrego la informacion que quiero al Intent
        perfilIntent.putExtra("token", token);
        perfilIntent.putExtra("username", username);

        Log.d("MAIN: ", "Mando a la PerfilActivity el user: \"" + username + "\".");
        Log.d("MAIN: ", "Mando a la PerfilActivity el token: \"" + token + "\".");

        // Inicio la actividad con el Intent
        startActivity(perfilIntent);

        Log.i("MAIN: ", "Inicie la PerfilActivity.");
    }


    /**
     * Guarda los datos necesarios para manejar los archivos, obtenidos a partir de la estructura de la carpeta.
     * @param estructuraCarpetasJSON la estructura interna de la carpeta actual en la que se encuentra
     *                               parado el usuario.
     */
    private void guardarDatosEstructuraArchivos(JSONObject estructuraCarpetasJSON){

        Log.i("MAIN: ", "Voy a guardar un mapa con las extensiones de los archivos de la carpeta en la que estoy parado.");

        extensionesArchivos         = new HashMap<>();
        URLArchivos                 = new HashMap<>();
        versionesServidorArchivos   = new HashMap<>();
        propietariosArchivos        = new HashMap<>();

        Iterator<?> keys = estructuraCarpetasJSON.keys();

        while( keys.hasNext() ) {

            int indexPunto;
            int indexCaracterReservado;
            int indexPrimeraBarra;
            int numeroVersionInt = 50;

            String URL = (String)keys.next();
            String nombreArchivo    = "";
            String extension        = "";
            String numeroVersionString;
            String value            = "";
            String propietario      = "";

            Log.d("MAIN: ", "Saque el path de la estructura: \"" + URL + "\".");

            try{
                value = estructuraCarpetasJSON.getString(URL);
            } catch (JSONException e){
                Log.e("MAIN: ", e.getMessage());
                Log.e("MAIN: ", "Error al obtener los datos JSON de estructura carpeta.");
                e.printStackTrace();
            }

            Log.d("MAIN: ", "Saque el nombre del archivo de la estructura: \"" + value + "\".");

            indexPunto = value.lastIndexOf(".");
            indexCaracterReservado = value.lastIndexOf(MyDataArrays.caracterReservado);
            indexPrimeraBarra = URL.indexOf("/");

            Log.d("MAIN: ", "La ubicacion del punto es: \"" + indexPunto + "\".");
            Log.d("MAIN: ", "La ubicacion del caracter reservado es: \"" + indexCaracterReservado + "\".");
            Log.d("MAIN: ", "La ubicacion de la primera contra barra es: \"" + indexPrimeraBarra + "\".");

            /*
             * Si es el archivo tiene un punto y un caracter reservado juntos, es porque tiene un punto
             * al final del nombre del archivo, sin tener una extension, menos en el caso de que sea una carpeta.
             */
            if( indexCaracterReservado == indexPunto + 1){
                nombreArchivo   = value.substring(0, indexPunto);
                extension = value.substring(indexPunto + 1);
                if( ! extension.equals(MyDataArrays.caracterReservado + "folder")){
                    extension = "";
                }
            } else if( (indexPunto > 0) && (indexCaracterReservado > 0) ){
                nombreArchivo   = value.substring(0, indexPunto);
                extension       = value.substring(indexPunto + 1, indexCaracterReservado);
            } else if(indexCaracterReservado > 0) {
                nombreArchivo   = value.substring(0, indexCaracterReservado);
            }

            numeroVersionString       = value.substring(indexCaracterReservado + 1);
            if( !numeroVersionString.equals("folder")){
                numeroVersionInt          = Integer.parseInt(numeroVersionString);
            }

            int indice = value.lastIndexOf(MyDataArrays.folderExtension);
            if(indice > 0 ) {
                nombreArchivo = value.substring(0, indice);
            } else {
                nombreArchivo = value.substring(0, indexCaracterReservado);
            }

            propietario = URL.substring(0, indexPrimeraBarra);

            Log.d("MAIN: ", "Gaurdo el archivo: \"" + nombreArchivo + "\" con la extension: \"" + extension + "\".");
            extensionesArchivos.put(nombreArchivo, extension);

            Log.d("MAIN: ", "Gaurdo el archivo: \"" + nombreArchivo + "\" con la URL: \"" + URL + "\".");
            URLArchivos.put(nombreArchivo, URL);

            Log.d("MAIN: ", "Gaurdo el archivo: \"" + nombreArchivo + "\" con el numero de version: \"" + numeroVersionInt + "\".");
            versionesServidorArchivos.put(nombreArchivo, numeroVersionInt);

            Log.d("MAIN: ", "Gaurdo el archivo: \"" + nombreArchivo + "\" con el propietario: \"" + propietario + "\".");
            propietariosArchivos.put(nombreArchivo, propietario);
        }
    }

    /**
     * Devuelve la extensión del archivo.
     * @param nombre nombre del archivo de la cual se quiere conocer la extensión.
     * @return La extensión del archivo.
     */
    private String obtenerExtensionArchivo(String nombre){
        String extension = extensionesArchivos.get(nombre);
        if (extension == null){
            nombre = nombre.replace(" ", MyDataArrays.caracterReemplazaEspacios);
            extension = extensionesArchivos.get(nombre);
        }
        Log.d("MAIN: ", "El archivo: \"" + nombre + "\" tiene la extension: \"" + extension + "\".");
        return extension;
    }

    /**
     * Devuelve el propietario del archivo.
     * @param nombre nombre del archivo de la cual se quiere conocer la extensión.
     * @return El propietario del archivo.
     */
    private String obtenerPropietarioArchivo(String nombre){
        String propietario = propietariosArchivos.get(nombre);
        Log.d("MAIN: ", "El archivo: \"" + nombre + "\" tiene el propietario: \"" + propietario + "\".");
        return propietario;
    }

    /**
     * Devuelve la URL que sirve de direccion, para interactuar con el archivo o carpeta.
     * @param nombre nombre del archivo o carpeta de la cual se quiere conocer la URL.
     * @return La URL que sirve de direccion del archivo o carpeta.
     */
    private String obtenerURLArchivo(String nombre){
        String URL = URLArchivos.get(nombre);
        if (URL == null){
            nombre = nombre.replace(" ", MyDataArrays.caracterReemplazaEspacios);
            URL = URLArchivos.get(nombre);
        }
        Log.d("MAIN: ", "El archivo: \"" + nombre + "\" tiene la URL: \"" + URL + "\".");
        return URL.replace(" ", MyDataArrays.caracterReemplazaEspacios);
    }

    /**
     * Devuelve el numero de version del archivo que se encuentra en el servidor.
     * @param nombre nombre del archivo o carpeta de la cual se quiere conocer el numero de version en el servidor.
     * @return El ultimo numero de version del archivo que se encuentra en el servidor.
     */
    private int obtenerNumeroVersionServidorArchivo(String nombre){
        int numeroVersionInt = 0;
        String nombeConEspacios = nombre.replace(MyDataArrays.caracterReemplazaEspacios, " ");
        Integer numeroVersion = versionesServidorArchivos.get(nombeConEspacios);
        if(numeroVersion != null){
            numeroVersionInt =  numeroVersion;
        }
        Log.d("MAIN: ", "El archivo: \"" + nombre + "\" tiene el numero de version: \"" + numeroVersionInt + "\".");
        return numeroVersionInt;
    }

    /**
     *
     * @param nombreArchivo
     */
    private void compartirArchivo(final String nombreArchivo){

        Log.i("MAIN: ", "Voy a crear un Alert Dialog para seleccionar los usuarios con los que " +
                "quiero compartir el archivo.");

        // Primero creo los items internos del AlertDialog
        // Empezamos con el mensaje de busqueda fallida
        textoFallido = new TextView(this);
        String mensajeFallido = "Ningun usuario concuerda con la busqueda";
        textoFallido.setText(mensajeFallido);
        textoFallido.setVisibility(View.INVISIBLE);

        // Luego creo la lista que va a mostrar los resultados de la busqueda y los usuarios ya compartidos
        // Para la lista necesito crear un adapter
        usuariosAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_multichoice);

        listaCompartidos = new ListView(this);
        listaCompartidos.setAdapter(usuariosAdapter);
        listaCompartidos.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Creo un ClickListener que cuando tildeo un usuario, lo agrega a una JSONArray para luego compartirlo
        // Si el usuario ya estaba tildado y lo destildeo, lo saca del JSONArray
        listaCompartidos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {

                String usuarioClickeado = listaCompartidos.getAdapter().getItem(pos).toString();
                Log.d("MAIN: ", "Clickie sobre el usuario: \"" + usuarioClickeado + "\".");

                // Si el item se tildo, quiero agregarlo a la lista de usuarios compartidos
                if(listaCompartidos.isItemChecked(pos)){
                    Log.i("MAIN: ", "El usuario clickeado NO estaba en la lista.");
                    try{
                        usuariosCompartidos.put(usuariosCompartidos.length(), usuarioClickeado);
                        Log.i("MAIN: ", "Agregue al usuario clickeado a la lista de usuarios compartidos.");
                        Log.d("MAIN: ", "La lista quedo asi: \"" + usuariosCompartidos.toString() + "\".");
                    } catch (JSONException e){
                        Log.e("MAIN: ", e.getMessage());
                        Log.e("MAIN: ", "Error al agregar un usuario al JSONArray de usuarios compartidos.");
                        e.printStackTrace();
                    }
                } else {
                    /* En cambio si el usuario fue destildado, quiero sacarlo
                     * No puedo destildar al propietario del archivo
                     */
                    if(usuarioClickeado.equals(obtenerPropietarioArchivo(nombreArchivo))){
                        listaCompartidos.setItemChecked(pos, true);
                        Toast.makeText(getApplicationContext(), "No puede descompartir un archivo con el propietario", Toast.LENGTH_LONG).show();
                    } else {
                        Log.i("MAIN: ", "El usuario clickeado SI estaba en la lista.");
                        int cant = usuariosCompartidos.length();
                        for (int i = 0; i < cant; i++) {
                            try{
                                String usuario = usuariosCompartidos.getString(i);
                                if (usuario.equals(usuarioClickeado)) {
                                    Log.d("MAIN: ", "Saco al usuario de la lista de usuarios compartidos: \"" + usuarioClickeado + "\".");
                                    usuariosCompartidos.remove(i);
                                    usuariosAdapter.remove(usuarioClickeado);
                                }
                            } catch (JSONException e){
                                Log.e("MAIN: ", e.getMessage());
                                Log.e("MAIN: ", "Error al sacar al usuario del JSONArray de los usuarios compartidos.");
                                e.printStackTrace();
                            }
                        }
                        Log.i("MAIN: ", "Saque al usuario clickeado de la lista de usuarios compartidos.");
                        Log.d("MAIN: ", "La lista quedo asi: \"" + usuariosCompartidos.toString() + "\".");
                    }
                }
            }
        });

        // Luego creo un EditText donde el usuario puede ingresar la busqueda que desea
        final EditText usuario = new EditText(this);

        usuario.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                usuariosAdapter.clear();
                if( (s.length() > 0) && (yaObtuveLosUsuariosCompartidos) ) {
                    yaObtuveLosUsuariosCompartidos = false;
                    getUsuarios(s.toString());
                } else if(yaObtuveLosUsuariosCompartidos){
                    recibirOActualizarMetadatos(nombreArchivo, MyDataArrays.RECIBIR);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Luego creo un LinearLayout y los configuro para ordenar todos los items anteriores y
        // mostrarlos bien en el AlertDialog TODO: UNIDICAR ESTO
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        float pxTodp = 300 / getResources().getDisplayMetrics().density;
        layout.setPadding((int) pxTodp, 0, (int) pxTodp, 0);

        // Agrego todos los items anteriores al Layout
        layout.addView(textoFallido);
        layout.addView(listaCompartidos);
        layout.addView(usuario);

        // Antes de mostrar el AlertDialog, cargo a la lista los usuarios ya compartidos
        // Para solo recibir y no mandar, pongo un FALSE
        recibirOActualizarMetadatos(nombreArchivo, MyDataArrays.RECIBIR);

        // Finalmente creo el AlertDialog y configuro como deseo
        alertCompartidos = new AlertDialog.Builder(this);
        alertCompartidos.setTitle("Compartir");

        // El boton "Compartir" actualizo los metadatos con todos los usuarios tildados
        alertCompartidos.setPositiveButton("Compartir", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("MAIN: ", "Hice click en Compartir.");

                // Mando a compartir los nuevos usuarios poniendo un TRUE
                recibirOActualizarMetadatos(nombreArchivo, MyDataArrays.RECIBIR_Y_ACTUALIZAR);

            }
        });

        // Un boton de cancelar, que no hace nada (se cierra la ventana emergente)
        alertCompartidos.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("MAIN: ", "Hice click en cancelar.");
            }
        });

        // Agrego el Layout al AlertDialog
        alertCompartidos.setView(layout);

        // Muestro el AlertDialog
        alertCompartidos.show();

    }

    /**
     * Recibe los metadatos del archivo, si ademas se quiere volver a mandarlos, con los nuevos datos actualizados,
     * se tiene que poner un true en el parametro actualizar.
     * @param nombreArchivo nombre del archivo del cual se mantienen los metadatos
     * @param actualizar true si se quiere actualizar los datos, false si solo se quiere recibir.
     */
    public void recibirOActualizarMetadatos(String nombreArchivo, final boolean actualizar){

        Log.i("MAIN: ", "Voy a los metadatos del archivo: \"" + nombreArchivo +"\".");
        QUERY_URL_METADATOS = MyDataArrays.direccion + "/metadata/" + obtenerURLArchivo(nombreArchivo);

        QUERY_URL_METADATOS = QUERY_URL_METADATOS.replace(" ", MyDataArrays.caracterReemplazaEspacios);

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

                            if (actualizar) {
                                // Le pongo los nuevos usuarios compartidos a los metadatos y los mando
                                metadatos.put("usuarios", usuariosCompartidos);
                                mandarMetadatos(metadatos);
                                usuariosCompartidos = new JSONArray();

                                // Si solo quiero recibir, los voy a agregar a la lista
                                // Si la lista esta vacia, es porque es la carga inicial de la lista
                            } else if (usuariosCompartidos.length() == 0) {

                                int cant = usrCompartidos.length();
                                for (int i = 0; i < cant; i++) {

                                    Log.d("MAIN: ", "Agrego al JSONArray de compartidos a: \"" + usrCompartidos.getString(i) + "\".");
                                    usuariosCompartidos.put(i, usrCompartidos.getString(i));

                                    // Si el usuario compartido, es distinto de el usuario actual que esta usando la aplicacion
                                    // lo agrego a la lista de usuarios compartidos que se muestra para tildar y destildar
                                    if (!username.equals(usuariosCompartidos.getString(i))) {
                                        Log.d("MAIN: ", "Agrego al adapter de la lista de compartidos a: \"" + usrCompartidos.getString(i) + "\".");
                                        usuariosAdapter.add(usrCompartidos.getString(i));
                                    }

                                    // Tildeo al usuario compartido en la lista que se muestra
                                    listaCompartidos.setItemChecked(i, true);
                                }
                                Log.d("MAIN: ", "La lista inicial de usuarios compartidos es: \"" + usuariosCompartidos.toString() + "\".");

                            } else {
                                for (int i = 0; i < usuariosCompartidos.length(); i++) {

                                    // Si el usuario compartido, es distinto de el usuario actual que esta usando la aplicacion
                                    // lo agrego a la lista de usuarios compartidos que se muestra para tildar y destildar
                                    if (!username.equals(usuariosCompartidos.getString(i))) {
                                        usuariosAdapter.add(usuariosCompartidos.getString(i));
                                    }

                                    // Tildeo al usuario compartido en la lista que se muestra
                                    listaCompartidos.setItemChecked(i, true);
                                }
                            }

                        } catch (JSONException e) {
                            Log.e("MAIN: ", e.getMessage());
                            Log.e("MAIN: ", "No se pudo obtener o reempazar los metadatos-usuariosCompartidos del JSON.");
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

    /**
     * Manda los metadatos actualizados de un archivo.
     * @param metadatos nuevos metadatos a mandar.
     */
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

    /**
     * Muestra un AlertDialog avisando al usuario, sobre una colisión entre
     * distintas versiones de un mismo archivo.
     */
    private void avisarColision(final String path){
        Log.i("MAIN: ", "Voy a crear un Alert Dialog avisando que hay una colision entre archivos");

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Advertencia!");
        alert.setMessage("Usted descargo una version anterior y va a pisar a la ultima versión.\n" +
                "¿Esta seguro que desea sobreescribir el archivo?");

        // El boton Sobreescribir sube el archivo igual
        alert.setPositiveButton("Sobreescribir", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("MAIN: ", "Hice click en sobreescribir.");
                subir(path, MyDataArrays.FORZAR);
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

    /**
     * Guarda en los datos privados de la aplicacion, el numero de la version de la ultima vez que
     * descargue o subi el archivo.
     * @param pathArchivo el path completo en el servidor, del archivo
     * @param numeroVersion el numero de la version descargada o subida
     */
    private void guardarVersionArchivo(String pathArchivo, int numeroVersion){

        pathArchivo = pathArchivo.replace(MyDataArrays.caracterReemplazaEspacios, " ");

        Log.d("MAIN: ", "Voy a guardar(en los datos guardados de la aplicacion) el numero" +
                " de la ultima version que descargue del archivo: \"" + pathArchivo + "\".");

        SharedPreferences.Editor e = mSharedPreferences.edit();
        String archivosDescargados = mSharedPreferences.getString(MyDataArrays.ARCHIVOS_DESCARGADOS, "{}");

        try{
            JSONObject archivosDescargadosJSON = new JSONObject(archivosDescargados);
            if(archivosDescargadosJSON.has(pathArchivo)){
                Log.i("MAIN: ", "Ya habia descargado un archivo con ese path, por lo que lo saco.");
                archivosDescargadosJSON.remove(pathArchivo);
            }
            Log.d("MAIN: ", "Guardo el nuevo numero de version: \"" + numeroVersion + "\".");
            archivosDescargadosJSON.put(pathArchivo, numeroVersion);

            // Guardo en la memoria, el username y el token
            e.putString(MyDataArrays.ARCHIVOS_DESCARGADOS, archivosDescargadosJSON.toString());
            e.commit();

            Log.i("MAIN: ", "Agregue los cambios de numero de version descargada a los datos guardados de la aplicacion.");

        } catch (JSONException error ){
            Log.e("MAIN: ", error.getMessage());
            Log.e("MAIN: ", "AL GUARDAR UN NUEVO NRO DE VERSION: Error al usar un JSON para modificar los datos guardados que contienen los numeros de version descargada de los archivos descargados.");
            error.printStackTrace();
        }
    }

    /**
     * Obtiene, desde los datos privados de la aplicacion, el numero de la ultima version descargada o subida del archivo.
     * @param pathArchivo ubicacion en el servidor, del archivo sobre el cual estoy obteniendo el numero de version
     * @return un int, con el ultimo numero de version de la ultima vez que descargue o subi el archivo
     */
    private int obtenerUltimoNumeroDeVersionDescargado(String pathArchivo){

        pathArchivo = pathArchivo.replace(MyDataArrays.caracterReemplazaEspacios, " ");

        Log.d("MAIN: ", "Voy a obtener(desde los datos guardados de la aplicacion) el numero " +
                "de la ultima version que descargue del archivo: \"" + pathArchivo + "\".");

        int numeroVersion = 0;
        String archivosDescargados = mSharedPreferences.getString(MyDataArrays.ARCHIVOS_DESCARGADOS, "{}");

        try{
            JSONObject archivosDescargadosJSON = new JSONObject(archivosDescargados);
            if(archivosDescargadosJSON.has(pathArchivo)){
                Log.i("MAIN: ", "Ya habia descargado un archivo con ese path.");
                numeroVersion = archivosDescargadosJSON.getInt(pathArchivo);
            } else {
                Log.i("MAIN: ", "Nunca descargue un archivo con ese path.");
            }
        } catch (JSONException error ){
            Log.e("MAIN: ", error.getMessage());
            Log.e("MAIN: ", "AL OBTENER UN NUEVO NRO DE VERSION: Error al obtener un numero de version de los archivos descargados.");
            error.printStackTrace();
        }

        Log.d("MAIN: ", "El numero de version obtenido es: \"" + numeroVersion + "\".");
        return numeroVersion;
    }

    private void mostrarVersionesAnteriores(final String nombreArchivo){

        Log.i("MAIN: ", "Voy a crear un Alert Dialog para seleccionar los usuarios con los que " +
                "quiero compartir el archivo.");

        // Primero creo los items internos del AlertDialog
        // Empezamos con el mensaje de busqueda fallida

        // Luego creo la lista que va a mostrar los resultados de la busqueda y los usuarios ya compartidos
        // Para la lista necesito crear un adapter
        ArrayAdapter<Integer> versionesAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice);

        int cantidadVersiones = obtenerNumeroVersionServidorArchivo(nombreArchivo);
        for(int i = 1; i <= cantidadVersiones; i ++){
            versionesAdapter.add(i);
        }


        final ListView listaVersiones = new ListView(this);
        listaVersiones.setAdapter(versionesAdapter);
        listaVersiones.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


        // Luego creo un LinearLayout y los configuro para ordenar todos los items anteriores y
        // mostrarlos bien en el AlertDialog TODO: UNIDICAR ESTO
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        float pxTodp = 300 / getResources().getDisplayMetrics().density;
        layout.setPadding((int) pxTodp, 0, (int) pxTodp, 0);

        // Agrego todos los items anteriores al Layout
        layout.addView(listaVersiones);


        // Finalmente creo el AlertDialog y configuro como deseo
        AlertDialog.Builder alertVersiones = new AlertDialog.Builder(this);
        alertVersiones.setTitle("Descargar versiones anteriores");
        alertVersiones.setMessage("Por favor elija la version que desea descargar.");

        // El boton "Compartir" actualizo los metadatos con todos los usuarios tildados
        alertVersiones.setPositiveButton("Descargar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("MAIN: ", "Hice click en Descargar en versiones anteriores.");

                // Mando a compartir los nuevos usuarios poniendo un TRUE
                descargarArchivo(nombreArchivo, listaVersiones.getCheckedItemPosition() + 1);
            }
        });

        // Un boton de cancelar, que no hace nada (se cierra la ventana emergente)
        alertVersiones.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("MAIN: ", "Hice click en cancelar y no descargue versiones anteriores.");
            }
        });

        // Agrego el Layout al AlertDialog
        alertVersiones.setView(layout);

        AlertDialog alerta = alertVersiones.create();
//        double width =
//        double height =
//        alerta.getWindow().setLayout( (this.getWindow().getDecorView().getWidth() * 4 ) / 5 , (this.getWindow().getDecorView().getHeight() * 2) / 3);


        // Muestro el AlertDialog
        alerta.show();

    }
}
