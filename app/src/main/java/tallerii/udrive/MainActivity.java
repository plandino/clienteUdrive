package tallerii.udrive;

import android.app.DownloadManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity implements FilesFragment.OnFragmentInteractionListener{

    FragmentManager fragmentManager;

    private String token = "";
    private String username = "";

    private String QUERY_URL = MyDataArrays.direccion + "/profile";

    DownloadManager downloadManager;
    private BroadcastReceiver receiverDownloadComplete;
    private BroadcastReceiver receiverNotificationClicked;
    private long myDownloadReference;

    private String nombreArchivo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        token = intent.getStringExtra("token");
        username = intent.getStringExtra("username");

        // Inicio el download manager
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        // Creo un fragmento dinamicamente, en un futuro voy a necesitar pedir
        // la estructura de las carpetas y archivos antes de crear el fragmento

        //Paso 1: Obtener la instancia del administrador de fragmentos
        fragmentManager = getFragmentManager();

        //Paso 2: Creo un nuevo fragmento
        FilesFragment fragment = new FilesFragment();

        // Creo un Bundle y le agrego informacion del tipo <Key, Value>
        Bundle bundle = new Bundle();
        bundle.putInt("groupPosition", 1);

        // Le agrego el Bundle al fragmento
        fragment.setArguments(bundle);

        //Paso 3: Crear una nueva transacción
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Agrego el fragmento a la transaccion
        transaction.add(R.id.contenedor, fragment);

        // Agrego la transaccion al backStack, para que pueda volver a las carpetas superiores
        // cuando apreto la flecha de para atras
        transaction.addToBackStack("inicio");

        //Paso 4: Confirmar el cambio
        transaction.commit();

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
                return true;
//            case R.id.buscar_archivo:
//                return true;
            case R.id.ver_perfil:
                pasarAVerPerfil();
                return true;
            case R.id.action_settings:
                //metodoSettings()
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void get(String carpetaPadre) {

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

        // Creo un cliente asyncronico, para comunicarme con el servidor
        AsyncHttpClient client = new AsyncHttpClient();

        // Le mando un GET al server, pidiendo la estructura interna de la carpeta que seleccione
        // QUERY_URL + carpetaPadre
        client.get(QUERY_URL, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                crearNuevoFragmento(jsonObject);
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onGroupClick(String id) {
        crearNuevoFragmento(new JSONObject());
//        get(id);
    }

    public void crearNuevoFragmento(JSONObject jsonObject){
        // Aca recibo desde el fragmento, la carpeta que seleccione y tengo que pedirle al servidor
        // que me devuelva la estructura de carpetas y archivos que estan adentro de esa.
        // Luego creo un nuevo fragmento con esa estructura y reemplazo el anterior

        //Paso 3: Creo un nuevo fragmento
        FilesFragment fragment = new FilesFragment();

        // Creo un Bundle y le agrego informacion del tipo <Key, Value>
        Bundle bundle = new Bundle();
        bundle.putInt("groupPosition", 2);

        // Le agrego el Bundle al fragmento
        fragment.setArguments(bundle);

        //Paso 2: Crear una nueva transacción
        FragmentTransaction transaction2 = fragmentManager.beginTransaction().replace(R.id.contenedor, fragment);

        // Agrego la transaccion al backStack, para que pueda volver a las carpetas superiores
        // cuando apreto la flecha de para atras
        transaction2.addToBackStack("dos");

        //Paso 4: Confirmar el cambio
        transaction2.commit();
    }

    @Override
    public void onOptionClick(String idCarpeta, String opcion) {
        if(opcion.equals("Eliminar")){

            QUERY_URL = QUERY_URL + "/file/" + username + "/filename" ;

            // Muestro una ventana emergente para confirmar que quiere eliminar
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Eliminar!");
            alert.setMessage("¿Esta seguro que quiere eliminar " + idCarpeta + "?");

            // El boton "OK" confirma la eliminacion del archivo o carpeta
            alert.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {

                    // TODO: LINEAS PARA BORRAR EL ARCHIVO
                    Toast.makeText(getApplicationContext(), "La cagaste en grande!", Toast.LENGTH_LONG).show();
                }
            });

            // Un boton de cancelar, que no hace nada (se cierra la ventana emergente)
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {}
            });

            alert.show();
        }

        if(opcion.equals("Descargar")){

//            GET en /file/<username_de_propietario>/<filename>

            nombreArchivo = "texto.txt";
            QUERY_URL = MyDataArrays.direccion + "/file/" + username + "/" + nombreArchivo ;
            AsyncHttpClient client = new AsyncHttpClient();

            RequestParams params = new RequestParams();
            params.put("token", token);
            params.put("user", username);

            final String savedFilePath = "/data/data/tallerii.udrive/files/" + nombreArchivo;

            File file=new File(savedFilePath);
//            apkFile.deleteOnExit();
            new AsyncHttpClient().get(QUERY_URL, params, new FileAsyncHttpResponseHandler(file) {
//                        @Override
//                        public void onProgress(int bytesWritten, int totalSize) {
//                            Toast.makeText(getApplicationContext(), "Descargando...", Toast.LENGTH_LONG).show();
//                        }

                        @Override
                        public void onSuccess(File file) {

                            Toast.makeText(getApplicationContext(), "termine", Toast.LENGTH_LONG).show();

                            // CON ESTO PUEDO ESCRIBIR EN EL ARCHIVO //
//                            String filename = "bajo.txt";
//                            String string = "Hello world!";
//                            FileOutputStream outputStream;
//
//                            try {
//                                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
//                                outputStream.write(string.getBytes());
//                                outputStream.close();
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
                            // CON ESTO PUEDO ESCRIBIR EN EL ARCHIVO //

                            // CON ESTO LEO EL TEXTO DEL ARCHIVO //
//                            try {
//                                FileInputStream fileIn=openFileInput(nombreArchivo);
//                                InputStreamReader InputRead= new InputStreamReader(fileIn);
//
//                                char[] inputBuffer= new char[100];
//                                String s="";
//                                int charRead;
//
//                                while ((charRead=InputRead.read(inputBuffer))>0) {
//                                    // char to string conversion
//                                    String readstring=String.copyValueOf(inputBuffer,0,charRead);
//                                    s +=readstring;
//                                }
//                                InputRead.close();
//                                Toast.makeText(getBaseContext(), s,Toast.LENGTH_SHORT).show();
//
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }

                            // CON ESTO LEO EL TEXTO DEL ARCHIVO //


                            // CON ESTO PASO AL DISPLAY ACTIVITY //
                            // Cuando la descarga es exitosa, cambio a otra activity que muestra el archivo
                            Intent intentDisplay = new Intent(MainActivity.this,DisplayActivity.class);
                            if(false){
                                intentDisplay.putExtra("extensionArchivo", "foto");
                            } else{
                                intentDisplay.putExtra("extensionArchivo", "texto");
                            }
                            intentDisplay.putExtra("filePath", savedFilePath);
                            intentDisplay.putExtra("nombreArchivo", nombreArchivo);
                            startActivity(intentDisplay);
                        }

                        @Override
                        public void onFailure(Throwable e, File response) {
                            Toast.makeText(getApplicationContext(), "cague", Toast.LENGTH_LONG).show();
                        }
                    }
            );

//
//
//            nombreArchivo = "texto.txt";
//
//            QUERY_URL = QUERY_URL + "/file/" + username + "/" + nombreArchivo ;
//
//            Uri uri = Uri.parse(QUERY_URL);
//            DownloadManager.Request request = new DownloadManager.Request(uri);
//
//            // Creo una notificacion para mostrar la descarga
//            request.setDescription("My Download").setTitle(nombreArchivo);
//
//            // Establezco el path donde se va a descargar el archivo, junto con el nombre
//            // del archivo
//            request.setDestinationInExternalFilesDir(MainActivity.this,Environment.DIRECTORY_DOWNLOADS, nombreArchivo);
//           // Con esto guardo en otra carpeta
//           // request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MyWebsiteLogo.png");
//
//            // Hago que el archivo sea visible en la aplicacion de descargas del sistema
//            request.setVisibleInDownloadsUi(true);
//
//            // Le digo a la aplicacion que baje el archivo por WIFI o por el internet del celular
//            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
//                    | DownloadManager.Request.NETWORK_MOBILE);
//
//            // Encolo la descarga
//            myDownloadReference = downloadManager.enqueue(request);

        }

        if(opcion.equals("Detalles")){
            cerrarSesion();
        }

        if(opcion.equals("Compartir")){

        }
    }

    @Override
    protected void onResume() {
        super.onResume();


        // Filtro de Intents, solo recibe Intents que notifican los clicks que se hacen sobre la descarga en curso
        IntentFilter clickIntentFilter = new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED);

        receiverNotificationClicked = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String extraId = DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
                long[] references = intent.getLongArrayExtra(extraId);
                for (long reference : references) {
                    if (reference == myDownloadReference) {
//                        do something with the download file
                    }
                }
            }
        };
        registerReceiver(receiverNotificationClicked, clickIntentFilter);

        // Filtra los Intents, solo recibe los que avisan que se completo una descarga
        IntentFilter completedIntentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        receiverDownloadComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (myDownloadReference == reference) {
//                    do something with the download file
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(reference);
                    Cursor cursor = downloadManager.query(query);

                    cursor.moveToFirst();
//                        get the status of the download
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(columnIndex);

                    int fileNameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
//                    String savedFilePath = cursor.getString(fileNameIndex);

//                        get the reason - more detail on the status
                    int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reason = cursor.getInt(columnReason);

                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:

                            // Cuando la descarga es exitosa, cambio a otra activity que muestra el archivo
                            Intent intentDisplay = new Intent(MainActivity.this,DisplayActivity.class);
                            if(false){
                                intentDisplay.putExtra("extensionArchivo", "foto");
                            } else{
                                intentDisplay.putExtra("extensionArchivo", "texto");
                            }
//                            intentDisplay.putExtra("filePath", savedFilePath);
                            intentDisplay.putExtra("nombreArchivo", nombreArchivo);
                            startActivity(intentDisplay);
                            break;
                        case DownloadManager.STATUS_FAILED:
                            Toast.makeText(MainActivity.this,
                                    "FAILED: " + reason,
                                    Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            Toast.makeText(MainActivity.this,
                                    "PAUSED: " + reason,
                                    Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_PENDING:
                            Toast.makeText(MainActivity.this,
                                    "PENDING!",
                                    Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            Toast.makeText(MainActivity.this,
                                    "RUNNING!",
                                    Toast.LENGTH_LONG).show();
                            break;
                    }
                    cursor.close();
                }
            }
        };
        registerReceiver(receiverDownloadComplete, completedIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiverDownloadComplete);
        unregisterReceiver(receiverNotificationClicked);
    }

    private void pasarAElegir(){

        // Creo un Intent para pasar al main
        Intent elegirIntent = new Intent(this, ElegirSesionActivity.class);

        // Agrego la informacion que quiero al Intent
        elegirIntent.putExtra("yaInicio", true);

        // Inicio la actividad con el Intent
        startActivity(elegirIntent);
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void pasarAVerPerfil(){
        // Creo un Intent para pasar al main
        Intent perfilIntent = new Intent(this, PerfilActivity.class);

        // Agrego la informacion que quiero al Intent
        perfilIntent.putExtra("yaInicio", true);
        perfilIntent.putExtra("token", token);
        perfilIntent.putExtra("username", username);

        // Inicio la actividad con el Intent
        startActivity(perfilIntent);
    }

    void cerrarSesion(){

        QUERY_URL = MyDataArrays.direccion + "/session" + token;

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("user", "pepito");

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
                Toast.makeText(getApplicationContext(), "Sesión cerrada", Toast.LENGTH_LONG).show();
                pasarAElegir();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    void subir(String path){
        String filename = "texto.txt";
        QUERY_URL = MyDataArrays.direccion + "/file" + "/" + username + "/" + filename;

        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
//        params.put("tipoDeArchivo", "foto");
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
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 1) {
            this.finish();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    public void subirArchivo(){

            FileDialog fileOpenDialog =  new FileDialog(MainActivity.this, "FileOpen..",
                    new FileDialog.SimpleFileDialogListener() {

                        @Override
                        public void onChosenDir(String chosenDir)
                        {

                            // Aca tengo que mandar la carpeta
                            Toast.makeText(getApplicationContext(), "PATH: " + chosenDir, Toast.LENGTH_LONG).show();
                            subir(chosenDir);
                        }
                    }
            );
            fileOpenDialog.default_file_name =  "/data/data/tallerii.udrive/files";
            fileOpenDialog.chooseFile_or_Dir(fileOpenDialog.default_file_name);
    }

    public void crearCarpeta(){
        // Muestro una ventana emergente para confirmar que quiere eliminar
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Crear carpeta");
        alert.setMessage("Introduzca el nombre de la nueva carpeta");

        // El boton "Subir" confirma subir el archivo
        alert.setPositiveButton("Crear", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {

                // TODO: LINEAS PARA BORRAR EL ARCHIVO
                Toast.makeText(getApplicationContext(), "Cree la nueva carpeta", Toast.LENGTH_LONG).show();
            }
        });

        // Un boton de cancelar, que no hace nada (se cierra la ventana emergente)
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {}
        });

        alert.show();
    }
}

