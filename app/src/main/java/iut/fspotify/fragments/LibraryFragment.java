package iut.fspotify.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import iut.fspotify.R;
import iut.fspotify.adapter.LikedSongAdapter;
import iut.fspotify.model.Song;
import iut.fspotify.utils.CSVParser;

public class LibraryFragment extends Fragment {

    private List<Song> likedSongs = new ArrayList<>();
    private List<Song> filteredSongs = new ArrayList<>();
    private LikedSongAdapter adapter;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    private Button likedSongsButton;
    private Button artistsButton;
    private Button albumsButton;
    private SearchView searchView;
    private Toolbar artistToolbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        likedSongsButton = view.findViewById(R.id.liked_songs_button);
        artistsButton = view.findViewById(R.id.artists_button);
        albumsButton = view.findViewById(R.id.albums_button);
        searchView = view.findViewById(R.id.search_view);
        artistToolbar = view.findViewById(R.id.artist_toolbar);

        RecyclerView recyclerView = view.findViewById(R.id.liked_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new LikedSongAdapter(getContext(), filteredSongs);
        recyclerView.setAdapter(adapter);

        // Configurer la flèche "back" dans la Toolbar
        artistToolbar.setNavigationIcon(R.drawable.back_arrow); // Assurez-vous d'avoir une icône `ic_back_arrow` dans `res/drawable`
        artistToolbar.setNavigationOnClickListener(v -> {
            artistToolbar.setVisibility(View.GONE);
            searchView.setVisibility(View.VISIBLE);
            showArtists();
        });

        likedSongsButton.setOnClickListener(v -> {
            updateButtonStates(likedSongsButton, likedSongsButton, artistsButton, albumsButton);
            searchView.setVisibility(View.GONE);
            artistToolbar.setVisibility(View.GONE);
            showLikedSongs();
        });

        artistsButton.setOnClickListener(v -> {
            updateButtonStates(artistsButton, likedSongsButton, artistsButton, albumsButton);
            searchView.setVisibility(View.VISIBLE);
            artistToolbar.setVisibility(View.GONE);
            showArtists();
        });

        albumsButton.setOnClickListener(v -> {
            updateButtonStates(albumsButton, likedSongsButton, artistsButton, albumsButton);
            searchView.setVisibility(View.GONE);
            artistToolbar.setVisibility(View.GONE);
            showAlbums();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterArtists(newText);
                return true;
            }
        });

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

        filteredSongs.clear();
        filteredSongs.addAll(likedSongs);
        adapter.notifyDataSetChanged();
    }

    private void updateButtonStates(Button activeButton, Button... allButtons) {
        for (Button button : allButtons) {
            button.setBackgroundTintList(requireContext().getColorStateList(R.color.button_selector));
        }
        activeButton.setBackgroundTintList(requireContext().getColorStateList(R.color.button_selector_active));
    }

    private void showLikedSongs() {
        loadLikedSongs();
    }

    private void showArtists() {
        Map<String, Float> artistDurations = new HashMap<>();
        List<Song> allSongs = CSVParser.parseCSV();

        for (Song song : allSongs) {
            artistDurations.put(song.getArtist(),
                    artistDurations.getOrDefault(song.getArtist(), 0f) + song.duration);
        }

        likedSongs.clear();
        likedSongs.addAll(artistDurations.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // Tri alphabétique
                .map(entry -> new Song(entry.getKey(), "", "", "", "", "", "", entry.getValue()))
                .collect(Collectors.toList()));

        filteredSongs.clear();
        filteredSongs.addAll(likedSongs);
        adapter.notifyDataSetChanged();

        adapter.setOnItemClickListener(song -> showSongsByArtist(song.getTitle()));
    }

    private void filterArtists(String query) {
        filteredSongs.clear();
        if (TextUtils.isEmpty(query)) {
            filteredSongs.addAll(likedSongs);
        } else {
            for (Song song : likedSongs) {
                if (song.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filteredSongs.add(song);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showSongsByArtist(String artist) {
        List<Song> allSongs = CSVParser.parseCSV();
        likedSongs.clear();
        for (Song song : allSongs) {
            if (song.getArtist().equals(artist)) {
                likedSongs.add(song);
            }
        }

        filteredSongs.clear();
        filteredSongs.addAll(likedSongs);
        adapter.notifyDataSetChanged();

        searchView.setVisibility(View.GONE);
        artistToolbar.setVisibility(View.VISIBLE);
        artistToolbar.setTitle(artist);

        adapter.setOnItemClickListener(song -> {
            PlayerFragment.playSelectedSong(requireContext(), song);
        });
    }

    private void showAlbums() {
        likedSongs.clear();
        filteredSongs.clear();
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