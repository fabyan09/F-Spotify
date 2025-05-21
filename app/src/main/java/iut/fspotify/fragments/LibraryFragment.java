package iut.fspotify.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import iut.fspotify.R;
import iut.fspotify.adapter.LikedSongAdapter;
import iut.fspotify.model.Song;
import iut.fspotify.utils.CSVParser;

public class LibraryFragment extends Fragment {

    private List<Song> likedSongs = new ArrayList<>();
    private LikedSongAdapter adapter;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.liked_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new LikedSongAdapter(getContext(), likedSongs);
        recyclerView.setAdapter(adapter);

        loadLikedSongs();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLikedSongs();
    }

    private void loadLikedSongs() {
        SharedPreferences prefs = requireContext().getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);
        List<Song> allSongs = CSVParser.parseCSV();
        likedSongs.clear();

        for (Song song : allSongs) {
            String key = song.title.trim().toLowerCase();
            if (prefs.getBoolean(key, false)) {
                likedSongs.add(song);
            }
        }

        adapter.notifyDataSetChanged();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);
        preferenceChangeListener = (sharedPreferences, key) -> loadLikedSongs();
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = requireContext().getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
}