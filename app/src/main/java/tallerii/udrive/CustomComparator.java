package tallerii.udrive;

import java.util.Comparator;

/**
 * Created by panchoubuntu on 17/11/15.
 */
public class CustomComparator implements Comparator<String> {

    final String folderExtension = MyDataArrays.caracterReservado + "folder";

    @Override
    public int compare(String lhs, String rhs) {
        final boolean lhsIsFolder = lhs.endsWith(folderExtension);
        final boolean rhsIsFolder = rhs.endsWith(folderExtension);
        if (lhsIsFolder && !rhsIsFolder)
            return -1;

        if (!lhsIsFolder && rhsIsFolder)
            return 1;

        return lhs.compareToIgnoreCase(rhs);
    }
}
