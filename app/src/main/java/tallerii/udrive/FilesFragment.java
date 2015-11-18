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

    Map<String, List<String>> miMap;
    Map<String, String>       hashTipoArchivos;
    List<String> hasMapKeys;
    ExpandableListView expandableListView;
    MyCustomAdapter adapter;

    ActionButton actionButton;

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
        View view = inflater.inflate(R.layout.fragment_files, container, false);

        TextView vistaVaciaTextView = (TextView) view.findViewById(R.id.vistaVacia);

        actionButton = (ActionButton) view.findViewById(R.id.action_button);
        actionButton.setButtonColor(getResources().getColor(R.color.violeta));
        actionButton.setImageResource(R.drawable.fab_plus_icon);

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.subirFAB();
            }
        });

        expandableListView = (ExpandableListView) view.findViewById(R.id.expandableList);
        JSONObject estructuraCarpetasJSON = new JSONObject();
        try{
            String estructuraCarpetas   = getArguments().getString("estructura");
            String tipo                 = getArguments().getString("tipo");
            if(tipo == null) tipo = MyDataArrays.CARPETA;
            if( estructuraCarpetas != null ){

                String texto;
                switch (tipo){
                    case MyDataArrays.BUSQUEDA:
                        texto = "La busqueda no devolvio ningun resultado.";
                        break;
                    case MyDataArrays.PAPELERA:
                        actionButton.hide();
                        texto = "La papelera se encuentra vacía.";
                        break;
                    case MyDataArrays.COMPARTIDOS:
                        actionButton.hide();
                        texto = "Nadie te ha compartido archivos.";
                        break;
                    case MyDataArrays.CARPETA:
                        texto = "Carpeta vacía. Sube algún archivo!";
                        break;
                    default:
                        texto = "Error: \"" + tipo + "\".";
                        Log.e("FILES_FRAGMENT: ", "Me pasaron un tipo incorrecto: \"" + tipo + "\".");
                        break;
                }

                if (estructuraCarpetas.equals("{}")) {
                    vistaVaciaTextView.setText(texto);
                    vistaVaciaTextView.setVisibility(View.VISIBLE);
                } else {
                    vistaVaciaTextView.setVisibility(View.INVISIBLE);
                }

                estructuraCarpetasJSON = new JSONObject(estructuraCarpetas);
            }
        }catch (JSONException e ){
            Log.e("FILES_FRAGMENT: ", e.getMessage());
            Log.e("FILES_FRAGMENT: ", "No se pudo obtener crear el JSON con la estructura de las carpetas.");
            e.printStackTrace();
        }
        miMap = MyDataProvider.getDataMap(estructuraCarpetasJSON);
        hashTipoArchivos = MyDataProvider.getTypeMap(estructuraCarpetasJSON);

        hasMapKeys = new ArrayList<>(miMap.keySet());

        adapter = new MyCustomAdapter(getActivity(), miMap, hasMapKeys, hashTipoArchivos);
        expandableListView.setAdapter(adapter);

        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                if(grupoAbierto > 0 ){
                    expandableListView.collapseGroup(grupoAbierto);
                }
                grupoAbierto = groupPosition;
            }
        });

        expandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View clickedView, int groupPosition, int childPosition, long id) {

                mListener.onOptionClick(hasMapKeys.get(groupPosition), miMap.get(hasMapKeys.get(groupPosition)).get(childPosition));
                return false;
            }
        });

        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View vista, int groupPosition, long id) {
                String idGrupo = hasMapKeys.get(groupPosition).replace(MyDataArrays.folderExtension, "");
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
        actionButton.hide();
    }

    public void mostrarFloatingButton(){
        actionButton.show();
    }

}
