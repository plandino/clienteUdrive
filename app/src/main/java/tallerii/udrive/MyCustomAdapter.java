package tallerii.udrive;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

/**
 * Adapter para cargar la informacion en la expandable list.
 */
public class MyCustomAdapter extends BaseExpandableListAdapter {

    private Context context;
    private HashMap<String, List<String>> hashMap;
    private List<String> opciones;
    private HashMap<String, String> hashTipoArchivos;

    public MyCustomAdapter(Context context, HashMap<String, List<String>> hashMap, List<String> list, HashMap<String, String> hashTipoArchivos) {
        this.context = context;
        this.hashMap = hashMap;
        this.opciones = list;
        this.hashTipoArchivos = hashTipoArchivos;
    }

    @Override
    public int getGroupCount() {
        return hashMap.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return hashMap.get(opciones.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return opciones.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return hashMap.get(opciones.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, final ViewGroup parent) {

        String groupTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.parent_layout, parent, false);
        }

        TextView parentTextView = (TextView) convertView.findViewById(R.id.textViewParent);
        ImageView listHeaderArrow = (ImageView) convertView.findViewById(R.id.arrow);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams( Gravity.TOP | Gravity.END);

        listHeaderArrow.setLayoutParams(layoutParams);

        parentTextView.setText(groupTitle);

        ImageView iconImage;
        iconImage = (ImageView) convertView.findViewById(R.id.icono);
        String tipo = hashTipoArchivos.get(groupTitle);

        if(tipo.equals(MyDataArrays.caracterReservado + "folder")){
            iconImage.setImageResource(R.mipmap.folder);
        } else if((tipo.equals("png")) || (tipo.equals("jpg")) || (tipo.equals("jpeg")) || (tipo.equals("gif"))){
            iconImage.setImageResource(R.mipmap.image);
        } else if((tipo.equals("pdf")) || (tipo.equals("doc")) || (tipo.equals("docx")) || (tipo.equals("txt")) || (tipo.equals("odt"))){
            iconImage.setImageResource(R.mipmap.file);
        } else {
            iconImage.setImageResource(R.mipmap.binary);
        }

        //Set the arrow programatically, so we can control it
        int imageResourceId = isExpanded ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float;
        listHeaderArrow.setImageResource(imageResourceId);
        listHeaderArrow.setScaleX(2.0f);
        listHeaderArrow.setScaleY(2.0f);

        listHeaderArrow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (isExpanded) ((ExpandableListView) parent).collapseGroup(groupPosition);
                else ((ExpandableListView) parent).expandGroup(groupPosition, true);

            }
        });

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        String childTitle = (String) getChild(groupPosition, childPosition);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.child_layout, parent, false);
        }
        TextView childTextView = (TextView) convertView.findViewById(R.id.textViewChild);
        childTextView.setText(childTitle);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
