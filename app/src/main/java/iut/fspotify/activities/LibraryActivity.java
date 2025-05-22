package iut.fspotify.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
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

public class LibraryActivity extends AppCompatActivity {

    private List<Song> likedSongs = new ArrayList<>();
    private List<Song> filteredSongs = new ArrayList<>();
    private LikedSongAdapter adapter;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    private Button likedSongsButton;
    private Button artistsButton;
    private Button albumsButton;
    private SearchView searchView;
    private androidx.appcompat.widget.Toolbar artistToolbar;
    private ImageButton navPlayerButton, navQueueButton, navLibraryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        likedSongsButton = findViewById(R.id.liked_songs_button);
        artistsButton = findViewById(R.id.artists_button);
        albumsButton = findViewById(R.id.albums_button);
        searchView = findViewById(R.id.search_view);
        artistToolbar = findViewById(R.id.artist_toolbar);
        
        // Initialisation des boutons de navigation
        navPlayerButton = findViewById(R.id.nav_player_button);
        navQueueButton = findViewById(R.id.nav_queue_button);
        navLibraryButton = findViewById(R.id.nav_library_button);

        RecyclerView recyclerView = findViewById(R.id.liked_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LikedSongAdapter(this, filteredSongs);
        recyclerView.setAdapter(adapter);

        // Configurer la flèche "back" dans la Toolbar
        artistToolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
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

        // Configuration de la navigation
        navPlayerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
            finish(); // Fermer l'activité actuelle
        });

        navQueueButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, QueueActivity.class);
            startActivity(intent);
            finish(); // Fermer l'activité actuelle
        });

        navLibraryButton.setOnClickListener(v -> {
            // Déjà sur cette activité, ne rien faire
        });

        loadLikedSongs();

        // Enregistrer le listener pour les changements de préférences
        SharedPreferences prefs = getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);
        preferenceChangeListener = (sharedPreferences, key) -> loadLikedSongs();
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLikedSongs();
    }

    private void loadLikedSongs() {
        SharedPreferences prefs = getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);
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

        // Configurer le clic pour jouer les titres likés
        adapter.setOnItemClickListener(song -> {
            PlayerActivity.playSong(this, song); // Joue la chanson
        });
    }

    private void updateButtonStates(Button activeButton, Button... allButtons) {
        for (Button button : allButtons) {
            button.setBackgroundTintList(getColorStateList(R.color.button_selector));
        }
        activeButton.setBackgroundTintList(getColorStateList(R.color.button_selector_active));
    }

    private void showLikedSongs() {
        loadLikedSongs();

        // Configurer le clic pour jouer les titres likés
        adapter.setOnItemClickListener(song -> {
            PlayerActivity.playSong(this, song); // Joue la chanson
        });
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
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new Song(entry.getKey(), "", "", "", "", "", "", entry.getValue()))
                .collect(Collectors.toList()));

        filteredSongs.clear();
        filteredSongs.addAll(likedSongs);
        adapter.notifyDataSetChanged();

        // Configurer le clic pour afficher les chansons d'un artiste
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

        // Configurer le clic pour jouer les chansons de l'artiste
        adapter.setOnItemClickListener(song -> {
            PlayerActivity.playSong(this, song); // Joue la chanson
        });
    }

    private void showAlbums() {
        Map<String, Song> albumToFirstSong = new HashMap<>();
        List<Song> allSongs = CSVParser.parseCSV();

        // Associer chaque album à la première chanson trouvée
        for (Song song : allSongs) {
            if (!albumToFirstSong.containsKey(song.getAlbum())) {
                albumToFirstSong.put(song.getAlbum(), song);
            }
        }

        likedSongs.clear();
        likedSongs.addAll(albumToFirstSong.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Song firstSong = entry.getValue();
                    return new Song(
                            entry.getKey(), // Nom de l'album comme titre
                            entry.getKey(), // Nom de l'album
                            firstSong.getArtist(), // Artiste
                            firstSong.date, // Date
                            firstSong.getCover(), // Cover de la première chanson
                            firstSong.getLyrics(), // Lyrics
                            firstSong.getMp3(), // MP3
                            firstSong.duration // Durée
                    );
                })
                .collect(Collectors.toList()));

        filteredSongs.clear();
        filteredSongs.addAll(likedSongs);
        adapter.notifyDataSetChanged();

        // Configurer le clic pour afficher les chansons d'un album
        adapter.setOnItemClickListener(song -> showSongsByAlbum(song.getAlbum()));
    }

    private void showSongsByAlbum(String album) {
        List<Song> allSongs = CSVParser.parseCSV();
        likedSongs.clear();
        for (Song song : allSongs) {
            if (song.getAlbum().equals(album)) {
                likedSongs.add(song);
            }
        }

        filteredSongs.clear();
        filteredSongs.addAll(likedSongs);
        adapter.notifyDataSetChanged();

        searchView.setVisibility(View.GONE);
        artistToolbar.setVisibility(View.VISIBLE);
        artistToolbar.setTitle(album);

        // Configurer le clic pour jouer les chansons de l'album
        adapter.setOnItemClickListener(song -> {
            PlayerActivity.playSong(this, song); // Joue la chanson
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
}
