package tallerii.udrive;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MyDataProvider {

    public static HashMap<String, List<String>> getDataHashMap(JSONObject json) {

        HashMap<String, List<String>> miHashMap = new HashMap<String, List<String>>();

        List<String> opcionesArchivosList = new ArrayList<String>();
        for (int i = 0; i < MyDataArrays.opcionesArchivos.length; i++) {
            opcionesArchivosList.add(MyDataArrays.opcionesArchivos[i]);
        }

        List<String> opcionesCarpetasList = new ArrayList<String>();
        for (int i = 0; i < MyDataArrays.opcionesCarpetas.length; i++) {
            opcionesCarpetasList.add(MyDataArrays.opcionesCarpetas[i]);
        }


        Iterator<?> keys = json.keys();

        while( keys.hasNext() ) {
            String key = (String)keys.next();
            String value = "";
            try{
                value = json.getString(key);
            } catch (JSONException e){

            }
            if( ( ! key.equals("#trash") ) && ( ! key.equals("#compartidos")) ){
                if((value.equals("#folder"))   ){
                    miHashMap.put(key, opcionesCarpetasList);
                } else {
                    miHashMap.put(key, opcionesArchivosList);
                }
            }

            Log.v("MyDataProvider","FILES: "+ key);
        }
        return miHashMap;

    }

    public static HashMap<String, String> getTypeHashMap(JSONObject json) {

        HashMap<String, String> hashTipoArchivos = new HashMap<String, String>();

        Iterator<?> keys = json.keys();

        while( keys.hasNext() ) {
            String key = (String)keys.next();
            String value = "";
            try{
                value = json.getString(key);
            } catch (JSONException e){

            }
            hashTipoArchivos.put(key, value);

            Log.v("MyDataProvider","FILES: "+ key);
        }


        return hashTipoArchivos;
    }
}
