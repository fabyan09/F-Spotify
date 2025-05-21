package iut.fspotify.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import iut.fspotify.R;
import iut.fspotify.adapter.QueueAdapter;
import iut.fspotify.model.Song;
import iut.fspotify.utils.CSVParser;
import android.widget.SearchView;

public class QueueFragment extends Fragment {

    private QueueAdapter adapter;
    private List<Song> songList = new ArrayList<>();
    private List<Song> filteredList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_queue, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.queue_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        songList = CSVParser.parseCSV(); // Charger les titres depuis le CSV
        filteredList.addAll(songList); // Initialiser la liste filtrée
        adapter = new QueueAdapter(filteredList, song -> {
            PlayerFragment.playSelectedSong(requireContext(), song);
        });
        recyclerView.setAdapter(adapter);

        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });

        return view;
    }

    private void filterList(String query) {
        filteredList.clear();
        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(songList);
        } else {
            for (Song song : songList) {
                if (song.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        song.getArtist().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(song);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}