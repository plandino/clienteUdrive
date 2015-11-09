package tallerii.udrive;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by panchoubuntu on 29/10/15.
 */
public class SearchableActivity extends AppCompatActivity implements FilesFragment.OnFragmentInteractionListener{

    FragmentManager fragmentManager;

    StringBuilder constructor;


    private String QUERY_URL_METADATOS;
    private String username;
    private String token;

    private boolean ultimo;
    private String estructuraCarpetas;
    private JSONObject estructuraCarpetasJSON;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchable);

        constructor = new StringBuilder();
        ultimo = false;





        fragmentManager = getFragmentManager();

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String busqueda = intent.getStringExtra(SearchManager.QUERY);
            username = intent.getStringExtra("user");
            token = intent.getStringExtra("token");
            actualizarMetadatos(busqueda, 0);
        }

        Toast.makeText(getApplicationContext(), "Inicie la search", Toast.LENGTH_LONG).show();

    }



    public void actualizarMetadatos(final String busqueda, final int cant){

        QUERY_URL_METADATOS = MyDataArrays.direccion + "/metadata/" ;

        AsyncHttpClient client = new AsyncHttpClient();

        final RequestParams params = new RequestParams();
        params.put("token", token);
        params.put("user", username);
        params.put("propietario", busqueda);


        client.get(QUERY_URL_METADATOS, params, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int status, Header[] headers, JSONObject jsonObject) {
                        if (jsonObject != null) {
                            try {
                                estructuraCarpetasJSON = jsonObject.getJSONObject("busqueda");
                            } catch (JSONException e) {

                            }
                            crearNuevoFragmento(estructuraCarpetasJSON.toString());



                        }
                    }

                    @Override
                    public void onFailure ( int statusCode, Header[] headers, Throwable throwable, JSONObject error){
                        Toast.makeText(getApplicationContext(), "Error al conectar con el servidor en recibir", Toast.LENGTH_LONG).show();
                    }
                }

        );
    }

    @Override
    public void onGroupClick(String id) {

    }

    @Override
    public void onOptionClick(String idCarpeta, String opcion) {

    }

    @Override
    public void onDownScroll() {

    }

    @Override
    public void eliminar() {

    }

    @Override
    public void subirFAB() {

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
}
