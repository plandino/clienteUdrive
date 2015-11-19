package tallerii.udrive;

import java.util.Comparator;

/**
 * Comparador Case Insensitive y que ademas ordena primero las carpetas y luego el resto de los archivos
 */
public class CustomComparator implements Comparator<String> {

    final String folderExtension = MyDataArrays.caracterReservado + "folder";

    @Override
    public int compare(String lhs, String rhs) {
        final boolean lhsIsFolder = lhs.endsWith(folderExtension);
        final boolean rhsIsFolder = rhs.endsWith(folderExtension);

        // Si el item a comparar termina con la extension de una carpeta, se ordena primero
        if (lhsIsFolder && !rhsIsFolder)
            return -1;

        if (!lhsIsFolder && rhsIsFolder)
            return 1;

        // Si nignuno o los dos items a comparar terminan con la extension de una carpeta, se ordenan Case Insensitive
        return lhs.compareToIgnoreCase(rhs);
    }
}
