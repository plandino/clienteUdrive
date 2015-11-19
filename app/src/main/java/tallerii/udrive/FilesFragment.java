package tallerii.udrive;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.software.shell.fab.ActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class FilesFragment extends Fragment implements AbsListView.OnItemClickListener {

    private OnFragmentInteractionListener mListener;

    Map<String, List<String>> mapaArchivosOpciones;
    Map<String, String> mapaExtensionArchivos;
    List<String>              opcionesMapKeys;
    ExpandableListView expandableListView;
    MyCustomAdapter adapter;

    ActionButton actionButton;

    // Este int sirve para saber que grupo fue abierto anteriormente y se debe cerrar.
    private int grupoAbierto = -1;

    // CONSTRUCTOR VACIO
    public FilesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.i("FILES_FRAGMENT: ", "Creo un nuevo fragmento.");
        // Expando la vista
        View view = inflater.inflate(R.layout.fragment_files, container, false);

        // Obtengo el TextView para mostrar el mensaje en caso de que no haya archivos o carpetas para mostrar
        TextView vistaVaciaTextView = (TextView) view.findViewById(R.id.vistaVacia);

        // Obtengo el floating action button que se usa para subir archivos
        actionButton = (ActionButton) view.findViewById(R.id.action_button);
        actionButton.setButtonColor(getResources().getColor(R.color.violeta));
        actionButton.setImageResource(R.drawable.fab_plus_icon);

        // Le agrego el listener al boton
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.subirFAB();
            }
        });

        Log.i("FILES_FRAGMENT: ", "Agregue el floating action button.");

        expandableListView = (ExpandableListView) view.findViewById(R.id.expandableList);
        JSONObject estructuraCarpetasJSON = new JSONObject();
        String username = "";
        try{
            String estructuraCarpetas   = getArguments().getString("estructura");
            Log.d("FILES_FRAGMENT: ", "Saco de los argumentos la estructura: \"" + estructuraCarpetas + "\".");

            String tipo                 = getArguments().getString("tipo");
            Log.d("FILES_FRAGMENT: ", "Saco de los argumentos el tipo: \"" + tipo + "\".");

            username             = getArguments().getString("username");
            Log.d("FILES_FRAGMENT: ", "Saco de los argumentos el username: \"" + username + "\".");

            if(tipo == null){
                Log.i("FILES_FRAGMENT: ", "No obtuve ningun tipo, le seteo tipo == CARPETA");
                tipo = MyDataArrays.CARPETA;
            }
            if( estructuraCarpetas != null ){

                if (estructuraCarpetas.equals("{}")) {
                    Log.i("FILES_FRAGMENT: ", "La carpeta esta vacia.");
                    String texto;
                    switch (tipo){
                        case MyDataArrays.BUSQUEDA:
                            texto = "La busqueda no devolvio ningun resultado.";
                            Log.i("FILES_FRAGMENT: ", "Seteo el texto de BUSQUEDA vacia.");
                            break;
                        case MyDataArrays.PAPELERA:
                            actionButton.hide();
                            texto = "La papelera se encuentra vacía.";
                            Log.i("FILES_FRAGMENT: ", "Seteo el texto de PAPELERA vacia y oculto el action button.");
                            break;
                        case MyDataArrays.COMPARTIDOS:
                            actionButton.hide();
                            texto = "Nadie te ha compartido archivos.";
                            Log.i("FILES_FRAGMENT: ", "Seteo el texto de COMPARTIDOS vacia y oculto el action button.");
                            break;
                        case MyDataArrays.CARPETA:
                            texto = "Carpeta vacía. Sube algún archivo!";
                            Log.i("FILES_FRAGMENT: ", "Seteo el texto de CARPETA vacia y oculto el action button.");
                            break;
                        default:
                            texto = "Error: \"" + tipo + "\".";
                            Log.e("FILES_FRAGMENT: ", "Me pasaron un tipo incorrecto: \"" + tipo + "\".");
                            break;
                    }
                    vistaVaciaTextView.setText(texto);
                    vistaVaciaTextView.setVisibility(View.VISIBLE);
                    Log.i("FILES_FRAGMENT: ", "Muestro el cartel indicando que la carpeta esta vacia.");

                } else {
                    Log.i("FILES_FRAGMENT: ", "La carpeta contiene cosas, oculto el cartel que dice que esta vacia.");
                    vistaVaciaTextView.setVisibility(View.INVISIBLE);
                }
                estructuraCarpetasJSON = new JSONObject(estructuraCarpetas);
                Log.i("FILES_FRAGMENT: ", "Cree un nuevo JSON con la estructura de la carpeta.");
            }
        }catch (JSONException e ){
            Log.e("FILES_FRAGMENT: ", e.getMessage());
            Log.e("FILES_FRAGMENT: ", "No se pudo obtener crear el JSON con la estructura de las carpetas.");
            e.printStackTrace();
        }
        mapaArchivosOpciones = MyDataProvider.getDataMap(estructuraCarpetasJSON, username);
        Log.i("FILES_FRAGMENT: ", "Obtuve el mapa de archivos y opciones.");

        mapaExtensionArchivos = MyDataProvider.getTypeMap(estructuraCarpetasJSON);
        Log.i("FILES_FRAGMENT: ", "Obtuve el mapa de archivos y extensiones.");

        opcionesMapKeys = new ArrayList<>(mapaArchivosOpciones.keySet());
        Log.i("FILES_FRAGMENT: ", "Cree un ArrayList con las opciones.");

        adapter = new MyCustomAdapter(getActivity(), mapaArchivosOpciones, opcionesMapKeys, mapaExtensionArchivos);
        expandableListView.setAdapter(adapter);
        Log.i("FILES_FRAGMENT: ", "Cree y le agregue el Adapter a la ExpandableListView.");

        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                // Primero colapso los fragmentos ya abiertos y luego expando el seleccionado.
                colapsarFragmentos();
                Log.d("FILES_FRAGMENT: ", "Expandi el grupo : \"" + groupPosition + "\".");
                grupoAbierto = groupPosition;
            }
        });

        expandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                Log.d("FILES_FRAGMENT: ", "Colapse el grupo : \"" + groupPosition + "\".");
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View clickedView, int groupPosition, int childPosition, long id) {

                String opcion = mapaArchivosOpciones.get(opcionesMapKeys.get(groupPosition)).get(childPosition);
                String idGrupo = opcionesMapKeys.get(groupPosition);
                Log.d("FILES_FRAGMENT: ", "Hice click sobre la opcion : \"" + opcion + "\" del padre: \"" + idGrupo + "\".");
                mListener.onOptionClick(idGrupo, opcion);
                return false;
            }
        });

        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View vista, int groupPosition, long id) {
                String idGrupo = opcionesMapKeys.get(groupPosition).replace(MyDataArrays.folderExtension, "");
                Log.d("FILES_FRAGMENT: ", "Hice click sobre el padre: \"" + idGrupo + "\".");
                mListener.onGroupClick(idGrupo);
                return true;
            }
        });


        expandableListView.setOnTouchListener(new OnSwipeTouchListener(getActivity().getApplicationContext()) {
            public void onSwipeTop() {
            }
            public void onSwipeRight() {
            }
            public void onSwipeLeft() {
            }
            public void onSwipeBottom() {
                Log.i("FILES_FRAGMENT: ", "El usuario deslizo el dedo hacia abajo.");
                mListener.onDownScroll();
            }

            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
//            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
        }
    }

    /* cualquier Activity que use este fragmento, debe implementar esta Interfaz,
       para lograr una buena comunicacion desde el fragmento hacia la Activity.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
//        public void onFragmentInteraction(String id);
        public void onGroupClick(String idGroup);
        public void onOptionClick(String idCarpeta, String opcion);
        public void onDownScroll();
        public void subirFAB();
    }

    public void ocultarFloatingButton(){
        Log.i("FILES_FRAGMENT: ", "Oculto el Floating Action Button.");
        actionButton.hide();
    }

    public void mostrarFloatingButton(){
        Log.i("FILES_FRAGMENT: ", "Muestro el Floating Action Button.");
        actionButton.show();
    }

    public void colapsarFragmentos(){
        if(grupoAbierto > 0 ){
            Log.d("FILES_FRAGMENT: ", "Colapse el grupo : \"" + grupoAbierto + "\".");
            expandableListView.collapseGroup(grupoAbierto);
        }
    }

}
