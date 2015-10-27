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

        List<String> opcionesPapelera = new ArrayList<String>();
        for (int i = 0; i < MyDataArrays.opcionesPapelera.length; i++) {
            opcionesPapelera.add(MyDataArrays.opcionesPapelera[i]);
        }


        Iterator<?> keys = json.keys();

        while( keys.hasNext() ) {
            String key = (String)keys.next();
            String value = "";
            try{
                value = json.getString(key);
            } catch (JSONException e){

            }

            int estamosEnPapelera = key.lastIndexOf(MyDataArrays.caracterReservado);
            int index = value.lastIndexOf(".");
            String nombre = "";
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
                } else if((extension.equals(MyDataArrays.caracterReservado + "folder"))   ){
                    miHashMap.put(nombre, opcionesCarpetasList);
                } else {
                    miHashMap.put(nombre, opcionesArchivosList);
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

            Log.v("MyDataProvider","FILES: "+ key);
        }


        return hashTipoArchivos;
    }
}
