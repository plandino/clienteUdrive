package tallerii.udrive;

/*created using Android Studio (Beta) 0.8.14
* www.101apps.co.za
* */
public class MyDataArrays {
    static char     caracterReservado = '!';
    static final String folderExtension = "." + caracterReservado + "folder";
    static String   caracterReemplazaEspacios = "~";
    static char     divisor = ';';
    static String[] opcionesArchivos = {"Descargar", "Versiones anteriores", "Compartir", "Eliminar"};
    static String[] opcionesPapelera = {"Restaurar", "Eliminar"};
    static String[] opcionesCarpetas = {"Eliminar"};

    static int BAD_REQUEST  = 400;
    static int UNAUTHORIZED = 401;
    static int NOT_FOUND    = 404;
    static int UNSOPORTED_METHOD = 405;
    static int CONFLICT     = 409;


    static int SUCCESS          = 200;
    static int RESOURCE_CREATED = 201;

    static boolean RESTAURAR    = true;
    static boolean ELIMINAR     = false;

    static boolean RECIBIR      = false;
    static boolean MANDAR       = true;

    static boolean FORZAR       = true;
    static boolean SIN_FORZAR   = false;

    static final String SESION_DATA = "prefs";
    static final String USERNAME    = "name";
    static final String TOKEN       = "token";
    static final String ARCHIVOS_DESCARGADOS = "descargados";
    static final String IP          = "IP";

    static final String BUSQUEDA     = "BUSQUEDA";
    static final String CARPETA      = "CARPETA";
    static final String PAPELERA     = "PAPELERA";
    static final String COMPARTIDOS  = "COMPARTIDOS";


    // MI CASA
    public static String direccion = "http://192.168.0.27:8080";

    // CASA CAMI
//    public static String direccion = "http://192.168.1.106:8080";


    public static void setIP(String ip){
        direccion = "http://" + ip + ":8080";
    }

    // MI CASA COMPU SANTI
//    public static String direccion = "http://192.168.0.39:8080";


    // CASA SANTI
//     public static String direccion = "http://192.168.0.31:8080";
    // CELU TOBI
//    public static String direccion = "http://192.168.43.224:8080";
    // CASA TOBI
//    public static String direccion = "http://192.168.1.113:8080";
    // FACU LH
//    public static String direccion = "http://10.30.1.91:8080";

//    public static String direccion = "http://192.168.43.140:8080";

}
