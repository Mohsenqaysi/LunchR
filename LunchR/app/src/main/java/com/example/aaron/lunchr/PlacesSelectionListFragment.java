package com.example.aaron.lunchr;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.HashMap;


/**
 * A simple {@link Fragment} subclass.
 */
public class PlacesSelectionListFragment extends Fragment {


    public PlacesSelectionListFragment() {
        // Required empty public constructor

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_places_selection_list, container, false);

        Selection selection = (Selection) getActivity();

        final HashMap<String, String> placesIdMapFromSelection = selection.getNameIdMap();
        if (placesIdMapFromSelection.containsKey("deleted")) {
            placesIdMapFromSelection.remove("deleted");
        }

        final String[] values = placesIdMapFromSelection.keySet().toArray(new String[placesIdMapFromSelection.size()]);

        for (String str : values){
            Log.e("After deleted place: ", str);
        }

        ListView listView = (ListView) view.findViewById(R.id.places_list_view);

        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                values
        );

        listView.setAdapter(listViewAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), Info.class);
                String place_id = placesIdMapFromSelection.get(values[position]);
                intent.putExtra("id", place_id);
                startActivity(intent);
            }
        });

        return view;
    }

}
