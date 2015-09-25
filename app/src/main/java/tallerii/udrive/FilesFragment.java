package tallerii.udrive;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
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

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    HashMap<String, List<String>> miHashMap;
    List<String> hasMapKeys;
    ExpandableListView expandableListView;
    MyCustomAdapter adapter;

    // TODO: Rename and change types of parameters. LO DEJO POR LAS DUDAS PARA MAS ADELANTE
    public static FilesFragment newInstance(String param1, String param2) {
        FilesFragment fragment = new FilesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    // CONSTRUCTOR VACIO
    public FilesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: LO DEJO PARA MAS ADELANTE POR LAS DUDAS
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);

        expandableListView = (ExpandableListView) view.findViewById(R.id.expandableList);
        int groupPosition = getArguments().getInt("groupPosition");
        miHashMap = MyDataProvider.getDataHashMap(groupPosition);

        hasMapKeys = new ArrayList<String>(miHashMap.keySet());

        adapter = new MyCustomAdapter(getActivity(), miHashMap, hasMapKeys);
        expandableListView.setAdapter(adapter);

        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
//                Toast.makeText(getActivity(), hasMapKeys.get(groupPosition) + " expanded", Toast.LENGTH_SHORT).show();
            }
        });

        expandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
//                Toast.makeText(getActivity(), hasMapKeys.get(groupPosition) + " collapsed", Toast.LENGTH_SHORT).show();
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View clickedView, int groupPosition, int childPosition, long id) {
                Toast.makeText(getActivity(), "Selected " + miHashMap.get(hasMapKeys.get(groupPosition)).get(childPosition)
                        + " from " + hasMapKeys.get(groupPosition), Toast.LENGTH_SHORT).show();
                mListener.onOptionClick(hasMapKeys.get(groupPosition), miHashMap.get(hasMapKeys.get(groupPosition)).get(childPosition));
                return false;
            }
        });

        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View vista, int groupPosition, long id) {

                mListener.onGroupClick(hasMapKeys.get(groupPosition));
                return true;
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
    }

}
