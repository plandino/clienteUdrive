package tallerii.udrive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MyDataProvider {

    public static HashMap<String, List<String>> getDataHashMap(int opcion) {

        HashMap<String, List<String>> miHashMap = new HashMap<String, List<String>>();

        List<String> opcionesList = new ArrayList<String>();
        for (int i = 0; i < MyDataArrays.opciones.length; i++) {
            opcionesList.add(MyDataArrays.opciones[i]);
        }

        switch (opcion){
            case 1:
                miHashMap.put("Facultad", opcionesList);
                miHashMap.put("Trabajo", opcionesList);
                miHashMap.put("Cursos", opcionesList);
                break;

            case 2:
                miHashMap.put("Chino Mandarin", opcionesList);
                miHashMap.put("Scala", opcionesList);
                miHashMap.put("Ruby", opcionesList);
                miHashMap.put("Como fabricar bombas", opcionesList);
                break;

            default:
                break;
        }

        return miHashMap;

    }
}
