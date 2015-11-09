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
import android.support.v4.view.MenuItemCompat;
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

public class MainActivity extends AppCompatActivity implements FilesFragment.OnFragmentInteractionListener {

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

    private JSONArray usuariosCompartidos;

    private boolean busque = false;
    private boolean menuActualizado = false;

    private MenuItem buscarNombre;
    private MenuItem buscarExtension;
    private MenuItem buscarEtiquetas;
    private MenuItem buscarPropietario;

    private Menu menuMain;

    private boolean buscarPorNombre;
    private boolean buscarPorExtension;
    private boolean buscarPorEtiquetas;
    private boolean buscarPorPropietario;


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

        usuariosCompartidos = new JSONArray();

        fragmentManager = getFragmentManager();

        busque = false;

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
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);


        menuMain = menu;
        agregarBusqueda(menu);


        menuActualizado = true;
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
    public boolean onPrepareOptionsMenu(Menu menu){

        if( !menuActualizado ){
            menuActualizado = true;
            if(busque){

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

            } else {
                if(buscarNombre != null){

                }


            }
        }

        return true;
    }

    public void agregarBusqueda(final Menu menu){
// Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.buscar_archivo).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Le agrego un Listener para detectar cuando expando la barra de busqueda y cuando la cierro
        MenuItem menuItem = menu.findItem(R.id.buscar_archivo);

        MenuItemCompat.setOnActionExpandListener(menuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {

                menu.removeItem(buscarNombre.getItemId());
                menu.removeItem(buscarExtension.getItemId());
                menu.removeItem(buscarEtiquetas.getItemId());
                menu.removeItem(buscarPropietario.getItemId());

                menu.removeItem(R.id.buscar_archivo);

                getMenuInflater().inflate(R.menu.menu_main, menu);

                agregarBusqueda(menu);

                busque = false;
                menuActualizado = false;
//                getMenuInflater().inflate(R.menu.menu_main, menu);
                get(null);
                return true;  // Siempre devuelvo true
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
//                invalidateOptionsMenu();
//                menu.clear();
                menuActualizado = false;
                busque = true;
//                getMenuInflater().inflate(R.menu.menu_busqueda, menu);

//                buscarNombre = menu.findItem(R.id.buscar_nombre);
//                buscarExtension = menu.findItem(R.id.buscar_extension);
//                buscarEtiquetas = menu.findItem(R.id.buscar_etiqueta);
//                buscarPropietario = menu.findItem(R.id.buscar_propietario);
                return true;  // Siempre devuelvo true
            }
        });
    }

    @Override
    public void startActivity(Intent intent) {


        // check if search intent
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String busqueda = intent.getStringExtra(SearchManager.QUERY);
            buscarPorMetadatos(busqueda);
            Toast.makeText(getApplicationContext(), "TATA2", Toast.LENGTH_LONG).show();
//            intent.putExtra("user", username);
//            intent.putExtra("token", token);
        }
////
//        super.startActivity(intent);

    }

    public void buscarPorMetadatos(final String busqueda){

//        busque = true;

        String myQuery = MyDataArrays.direccion + "/metadata/" ;

        AsyncHttpClient client = new AsyncHttpClient();

        final RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        if(buscarPorNombre){
            params.put("nombre", busqueda);
        }
        if(buscarPorExtension){
            params.put("extension", busqueda);

        }
        if(buscarPorEtiquetas){
            params.put("etiqueta", busqueda);

        }
        if(buscarPorPropietario){
            params.put("propietario", busqueda);

        }
        params.put("propietario", busqueda);


        client.get(myQuery, params, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int status, JSONObject jsonObject) {
                        if (jsonObject != null) {
                            try {
                                estructuraCarpetasJSON = jsonObject.getJSONObject("busqueda");
                            } catch (JSONException e) {

                            }
                            crearNuevoFragmento(estructuraCarpetasJSON.toString());



                        }
                    }

                    @Override
                    public void onFailure ( int statusCode, Throwable throwable, JSONObject error){
                        Toast.makeText(getApplicationContext(), "Error al conectar con el servidor en recibir", Toast.LENGTH_LONG).show();
                    }
                }

        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.buscar_archivo:
                Log.i("MAIN: ", "Hice click en el boton de buscar archivo.");
//                onSearchRequested();
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

            case R.id.buscar_nombre:
                buscarNombre.setChecked(!buscarNombre.isChecked());
                Log.i("MAIN: ", "Cambie el estado de la busqueda por nombres de archivos.");
                buscarPorNombre = buscarNombre.isChecked();
                return true;
            case R.id.buscar_extension:
                buscarExtension.setChecked(!buscarExtension.isChecked());
                Log.i("MAIN: ", "Cambie el estado de la busqueda por extensiones de archivos.");
                buscarPorExtension = buscarExtension.isChecked();
                return true;
            case R.id.buscar_etiqueta:
                buscarEtiquetas.setChecked(!buscarEtiquetas.isChecked());
                Log.i("MAIN: ", "Cambie el estado de la busqueda por etiquetas de archivos.");
                buscarPorEtiquetas = buscarEtiquetas.isChecked();
                return true;
            case R.id.buscar_propietario:
                buscarPropietario.setChecked(!buscarPropietario.isChecked());
                Log.i("MAIN: ", "Cambie el estado de la busqueda por propietarios de archivos.");
                buscarPorPropietario = buscarPropietario.isChecked();
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
                                arAdapter.add(user);
                                listaCompartidos.setItemChecked(j, false);


                                Log.d("MAIN: ", "usuarios compartidos: \"" + usuariosCompartidos.toString() + "\".");

                                int cant = usuariosCompartidos.length();
                                Log.d("MAIN: ", "cant: \"" + cant + "\".");

                                for (int i = 0; i < cant; i++) {
                                    try{

                                        String usuario = usuariosCompartidos.getString(i);
                                        Log.d("MAIN: ", "El usuario revisado es: \"" + usuario + "\".");
                                        Log.d("MAIN: ", "El usuario agregado es: \"" + usuario + "\".");


                                        if (usuario.equals(user)) {
                                            listaCompartidos.setItemChecked(j, true);
                                            Log.d("MAIN: ", "Puse en true el tilde.");

                                        }
                                    } catch (JSONException e){
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

        borrarDatosUsuario();

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

    private void borrarDatosUsuario(){

        Log.i("MAIN: ", "Voy a borrar los datos de usuario.");

        SharedPreferences mSharedPreferences;

        // Accedo a los datos guardados
        mSharedPreferences = getSharedPreferences(MyDataArrays.SESION_DATA, MODE_PRIVATE);

        // Guardo en la memoria, el username y el token
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.remove(MyDataArrays.USERNAME);
        e.remove(MyDataArrays.TOKEN);
        e.apply();

        Log.i("MAIN: ", "Pude borrar los datos de usuario.");

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


        if(volver) get(null);

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
        arAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_multichoice);

        listaCompartidos = new ListView(this);
        listaCompartidos.setAdapter(arAdapter);
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
                } else { // En cambio si el usuario fue destildado, quiero sacarlo
                    Log.i("MAIN: ", "El usuario clickeado SI estaba en la lista.");
                    int cant = usuariosCompartidos.length();
                    for (int i = 0; i < cant; i++) {
                        try{
                            String usuario = usuariosCompartidos.getString(i);
                            if (usuario.equals(usuarioClickeado)) {
                                Log.d("MAIN: ", "Saco al usuario de la lista de usuarios compartidos: \"" + usuarioClickeado + "\".");
                                usuariosCompartidos.remove(i);
                                arAdapter.remove(usuarioClickeado);
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
        });

        // Luego creo un EditText donde el usuario puede ingresar la busqueda que desea
        final EditText usuario = new EditText(this);

        usuario.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                arAdapter.clear();
                if (s.length() > 0) {
                    getUsuarios(s.toString());
                } else {
                    recibirOActualizarMetadatos(nombreArchivo, false);
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
        // mostrarlos bien en el AlertDialog
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
                recibirOActualizarMetadatos(nombreArchivo, MyDataArrays.MANDAR);
            }
        });

        // Un boton de cancelar, que no hace nada (se cierra la ventana emergente)
        alertCompartidos.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("MAIN: ", "Hice click en cancelar.");
            }
        });

        // Agrego el Layout al AlertDialog
        alertCompartidos.setView(layout);

        // Muestro el AlertDialog
        alertCompartidos.show();

    }

    public void recibirOActualizarMetadatos(String idArchivo, final boolean actualizar){

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

                            if(actualizar){
                                // Le pongo los nuevos usuarios compartidos a los metadatos y los mando
                                metadatos.put("usuarios", usuariosCompartidos);
                                mandarMetadatos(metadatos);

                                // Si solo quiero recibir, los voy a agregar a la lista
                                // Si la lista esta vacia, es porque es la carga inicial de la lista
                            } else if(usuariosCompartidos.length() == 0){

                                int cant = usrCompartidos.length();
                                for(int i = 0; i < cant ; i++){

                                    Log.d("MAIN: ", "Agrego al JSONArray de compartidos a: \"" + usrCompartidos.getString(i) + "\".");
                                    usuariosCompartidos.put(i, usrCompartidos.getString(i));

                                    // Si el usuario compartido, es distinto de el usuario actual que esta usando la aplicacion
                                    // lo agrego a la lista de usuarios compartidos que se muestra para tildar y destildar
                                    if( !username.equals(usuariosCompartidos.getString(i)) ) {
                                        Log.d("MAIN: ", "Agrego al adapter de la lista de compartidos a: \"" + usrCompartidos.getString(i) + "\".");
                                        arAdapter.add(usrCompartidos.getString(i));
                                    }

                                    // Tildeo al usuario compartido en la lista que se muestra
                                    listaCompartidos.setItemChecked(i, true);
                                }
                                Log.d("MAIN: ", "La lista inicial de usuarios compartidos es: \"" + usuariosCompartidos.toString() + "\".");

                            } else {
                                for(int i = 0; i < usuariosCompartidos.length(); i++){

                                    // Si el usuario compartido, es distinto de el usuario actual que esta usando la aplicacion
                                    // lo agrego a la lista de usuarios compartidos que se muestra para tildar y destildar
                                    if( !username.equals(usuariosCompartidos.getString(i)) ){
                                        arAdapter.add(usuariosCompartidos.getString(i));
                                    }

                                    // Tildeo al usuario compartido en la lista que se muestra
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

    private void avisarColision(){
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

    private void guardarVersionDescargada(String version){

    }
}
