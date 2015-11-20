package tallerii.udrive;

/**
 * Contiene los datos(Strings, ints y booleans) que usa la aplicacion.
 */
public class MyDataArrays {
    final static char     caracterReservado = '!';
    final static String folderExtension = "." + caracterReservado + "folder";
    final static String   caracterReemplazaEspacios = "~";
    final static char     divisor = ';';
    final static String[] opcionesArchivos    = {"Descargar", "Versiones anteriores", "Compartir", "Eliminar"};
    final static String[] opcionesCompartidos = {"Descargar", "Versiones anteriores", "Actualizar", "Compartir", "Eliminar"};
    final static String[] opcionesPapelera = {"Restaurar", "Eliminar"};
    final static String[] opcionesCarpetas = {"Eliminar"};

    final static int BAD_REQUEST  = 400;
    final static int UNAUTHORIZED = 401;
    final static int NOT_FOUND    = 404;
    final static int UNSOPORTED_METHOD = 405;
    final static int CONFLICT     = 409;


    final static int SUCCESS          = 200;
    final static int RESOURCE_CREATED = 201;

    final static boolean RESTAURAR    = true;
    final static boolean ELIMINAR     = false;

    final static boolean RECIBIR      = false;
    final static boolean RECIBIR_Y_ACTUALIZAR = true;

    final static boolean FORZAR       = true;
    final static boolean SIN_FORZAR   = false;

    final static String SESION_DATA = "prefs";
    final static String USERNAME    = "name";
    final static String TOKEN       = "token";
    final static String ARCHIVOS_DESCARGADOS = "descargados";
    final static String IP          = "IP";

    final static String BUSQUEDA     = "BUSQUEDA";
    final static String CARPETA      = "CARPETA";
    final static String PAPELERA     = "PAPELERA";
    final static String COMPARTIDOS  = "COMPARTIDOS";

    public static String direccion = "http://192.168.0.27:8080";

    /**
     * Setea la nueva IP de la aplicacion.
     * @param ip ip a usar.
     */
    public static void setIP(String ip){
        direccion = "http://" + ip + ":8080";
    }
}
