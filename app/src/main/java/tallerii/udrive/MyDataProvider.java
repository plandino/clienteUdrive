package tallerii.udrive;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Esta clase se encarga de proveer los datos necesarios para poblar la expandable list
 */
public class MyDataProvider {

    /**
     * Devuelve un hashmap con un String como clave y una lista de Stirng como valor.
     * La clave es el titulo del padre en la expandable list.
     * El valor son las opciones que aparecen cuando se despliegan los hijos de la expandable list.
     * @param json la estructura de la carpeta a mostrar
     * @return el hashmap con los pares de clave valor
     */
    public static HashMap<String, List<String>> getDataHashMap(JSONObject json) {

        Log.i("MY_DATA_PROVIDER: ", "Voy a obtener el dataHashMap con la estructura de las carpetas.");

        HashMap<String, List<String>> miHashMap = new HashMap<>();

        List<String> opcionesArchivosList = new ArrayList<>();
        opcionesArchivosList.addAll(Arrays.asList(MyDataArrays.opcionesArchivos));

        List<String> opcionesCarpetasList = new ArrayList<>();
        opcionesCarpetasList.addAll(Arrays.asList(MyDataArrays.opcionesCarpetas));

        List<String> opcionesPapelera = new ArrayList<>();
        opcionesPapelera.addAll(Arrays.asList(MyDataArrays.opcionesPapelera));

        Iterator<?> keys = json.keys();

        while( keys.hasNext() ) {
            String key = (String)keys.next();
            String value = "";
            try{
                value = json.getString(key);
            } catch (JSONException e){
                Log.e("MY_DATA_PROVIDER: ", "Hubo un error al obtener algun valor de la estructura de carpetas.");
                Log.e("MY_DATA_PROVIDER: ", e.getMessage());
                e.printStackTrace();
            }

            int estamosEnPapelera = key.lastIndexOf(MyDataArrays.caracterReservado);
            int index = value.lastIndexOf(".");
            String nombre;
            String extension = "";
            if(index >= 0){
                nombre = value.substring(0, index);
                extension = value.substring(index);
            } else {
                nombre = value;
            }

            if( ( ! nombre.equals(MyDataArrays.caracterReservado + "trash") ) && ( ! nombre.equals(MyDataArrays.caracterReservado + "compartidos")) ){
                if(estamosEnPapelera > 0){
                    miHashMap.put(nombre, opcionesPapelera);
                } else if((extension.equals("." + MyDataArrays.caracterReservado + "folder"))   ){
                    miHashMap.put(nombre, opcionesCarpetasList);
                } else {
                    miHashMap.put(nombre, opcionesArchivosList);
                }
            }
        }
        return miHashMap;

    }

    public static HashMap<String, String> getTypeHashMap(JSONObject json) {

        Log.i("MY_DATA_PROVIDER: ", "Voy a obtener el typeHashMap con la estructura de las carpetas.");

        HashMap<String, String> hashTipoArchivos = new HashMap<>();

        Iterator<?> keys = json.keys();

        while( keys.hasNext() ) {
            String key = (String)keys.next();
            String value = "";
            try{
                value = json.getString(key);
            } catch (JSONException e){
                Log.e("MY_DATA_PROVIDER: ", "Hubo un error al obtener algun valor de la estructura de carpetas.");
                Log.e("MY_DATA_PROVIDER: ", e.getMessage());
                e.printStackTrace();
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

        return hashTipoArchivos;
    }
}
