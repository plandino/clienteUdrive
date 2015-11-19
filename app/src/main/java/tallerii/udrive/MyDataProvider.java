package tallerii.udrive;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Esta clase se encarga de proveer los datos necesarios para poblar la expandable list.
 */
public class MyDataProvider {

    /**
     * Devuelve un Map con un String como clave y una lista de Stirng como valor.
     * La clave es el titulo del padre en la expandable list.
     * El valor son las opciones que aparecen cuando se despliegan los hijos de la expandable list.
     * @param json la estructura de la carpeta a mostrar.
     * @return el Map con los pares de clave valor.
     */
    public static Map<String, List<String>> getDataMap(JSONObject json, String username) {

        Log.i("MY_DATA_PROVIDER: ", "Voy a obtener el dataMap con la estructura de las carpetas.");

        Map<String, List<String>> miMap = new TreeMap<>(new CustomComparator());

        List<String> opcionesArchivosList = new ArrayList<>();
        opcionesArchivosList.addAll(Arrays.asList(MyDataArrays.opcionesArchivos));

        List<String> opcionesCarpetasList = new ArrayList<>();
        opcionesCarpetasList.addAll(Arrays.asList(MyDataArrays.opcionesCarpetas));

        List<String> opcionesPapelera = new ArrayList<>();
        opcionesPapelera.addAll(Arrays.asList(MyDataArrays.opcionesPapelera));

        List<String> opcionesCompartidos = new ArrayList<>();
        opcionesCompartidos.addAll(Arrays.asList(MyDataArrays.opcionesCompartidos));

        Iterator<?> keys = json.keys();

        while( keys.hasNext() ) {

            int indexPunto;
            int indexCaracterReservado;
            int indexPrimerBarra;

            String URL = (String)keys.next();
            String nombreArchivo    = "";
            String extension        = "";
            String value            = "";

            String propietario      = "";
            indexPrimerBarra = URL.indexOf("/");
            propietario = URL.substring(0, indexPrimerBarra);


            boolean estamosEnPapelera = estamosEnPapelera(URL);

            Log.d("MY_DATA_PROVIDER: ", "Saque el path de la estructura: \"" + URL + "\".");

            try {

                value = json.getString(URL);

                Log.d("MY_DATA_PROVIDER: ", "Saque el nombre del archivo de la estructura: \"" + value + "\".");

                indexPunto = value.lastIndexOf(".");
                indexCaracterReservado = value.lastIndexOf(MyDataArrays.caracterReservado);


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

                if(value.contains(MyDataArrays.caracterReservado + "folder")){
                    indexCaracterReservado = value.length();
                } else {
                    indexCaracterReservado = value.lastIndexOf(MyDataArrays.caracterReservado);
                }
                nombreArchivo = value.substring(0, indexCaracterReservado);

//                if( extension.equals(MyDataArrays.caracterReservado + "folder")  ){
//                    nombreArchivo = value.substring(0, indexCaracterReservado - 1);
//                } else {
//                    nombreArchivo = value.substring(0, indexCaracterReservado);
//                }

                Log.d("MY_DATA_PROVIDER: ", "Obtuve el nombre del archivo: \"" + nombreArchivo + "\".");
                Log.d("MY_DATA_PROVIDER: ", "Con la extension: \"" + extension + "\".");

            } catch (JSONException e){
                Log.e("MAIN: ", e.getMessage());
                Log.e("MAIN: ", "Error al sacar al usuario del JSONArray de los usuarios compartidos.");
                e.printStackTrace();
            }


            if( ! nombreArchivo.contains(MyDataArrays.caracterReservado + "trash")){
                if( ! username.equals(propietario) ){
                    Log.i("MY_DATA_PROVIDER: ", "Le pongo las opciones de los archivos compartidos.");
                    miMap.put(nombreArchivo, opcionesCompartidos);
                } else if(estamosEnPapelera){
                    Log.i("MY_DATA_PROVIDER: ", "Le pongo las opciones de la papelera.");
                    miMap.put(nombreArchivo, opcionesPapelera);
                } else if((extension.equals(MyDataArrays.caracterReservado + "folder"))   ){
                    Log.i("MY_DATA_PROVIDER: ", "Le pongo las opciones de las carpetas.");
                    miMap.put(nombreArchivo, opcionesCarpetasList);
                } else {
                    Log.i("MY_DATA_PROVIDER: ", "Le pongo las opciones de los archivos.");
                    miMap.put(nombreArchivo, opcionesArchivosList);
                }
            }
        }

        return miMap;

    }


    /**
     * Permite saber si la estructura pasada de la carpeta a mostrar, es de la papelera o no.
     * @param URL URL de un archivo.
     * @return true si se encuentra en la papelera, false si no se encuentra en la papelera.
     */
    public static boolean estamosEnPapelera(String URL){
        String[] partes = URL.split("/");
        boolean estamosEnPapelera = false;
        for (String carpeta: partes ) {
            if(carpeta.equals( MyDataArrays.caracterReservado + "trash")) estamosEnPapelera = true;
        }
        return estamosEnPapelera;
    }


    /**
     * Devuelve un Map con un String como clave y otro de Stirng como valor.
     * La clave es el nombre del archivo o carpeta.
     * El valor es la extension del archivo o carpeta.
     * @param json la estructura de la carpeta a mostrar.
     * @return el Map con los pares de clave valor.
     */
    public static Map<String, String> getTypeMap(JSONObject json) {

        Log.i("MY_DATA_PROVIDER: ", "Voy a obtener el typeMap con la estructura de las carpetas.");

        Map<String, String> mapTipoArchivos = new TreeMap<>(new CustomComparator());

        Iterator<?> keys = json.keys();

        while( keys.hasNext() ) {
            String key = (String)keys.next();

            int indexPunto;
            int indexCaracterReservado;

            String nombreArchivo    = "";
            String extension        = "";
            String value            = "";

            try{



                value = json.getString(key);

                indexPunto = value.lastIndexOf(".");
                indexCaracterReservado = value.lastIndexOf(MyDataArrays.caracterReservado);

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

//                if(value.contains(MyDataArrays.caracterReservado + "folder")){
//                    indexCaracterReservado = value.length();
//                } else {
//                    indexCaracterReservado = value.lastIndexOf(MyDataArrays.caracterReservado);
//                }
//                nombreArchivo = value.substring(0, indexCaracterReservado);

                if( extension.equals(MyDataArrays.caracterReservado + "folder")  ){
                    nombreArchivo = value.substring(0, indexCaracterReservado - 1);
                } else {
                    nombreArchivo = value.substring(0, indexCaracterReservado);
                }

                Log.d("MY_DATA_PROVIDER: ", "Obtuve el nombre del archivo: \"" + nombreArchivo + "\".");
                Log.d("MY_DATA_PROVIDER: ", "Con la extension: \"" + extension + "\".");
            } catch (JSONException e){
                Log.e("MY_DATA_PROVIDER: ", "Hubo un error al obtener algun valor de la estructura de carpetas.");
                Log.e("MY_DATA_PROVIDER: ", e.getMessage());
                e.printStackTrace();
            }

            mapTipoArchivos.put(nombreArchivo, extension);
        }

        return mapTipoArchivos;
    }
}
