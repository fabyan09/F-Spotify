package iut.fspotify.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import iut.fspotify.R;
import iut.fspotify.adapter.LikedSongAdapter;
import iut.fspotify.model.Song;
import iut.fspotify.services.MusicPlayerService;
import iut.fspotify.utils.CSVParser;

public class LibraryActivity extends AppCompatActivity implements MusicPlayerService.OnMusicPlayerListener {

    private List<Song> likedSongs = new ArrayList<>();
    private List<Song> filteredSongs = new ArrayList<>();
    private LikedSongAdapter adapter;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    private androidx.appcompat.widget.AppCompatButton likedSongsButton;
    private androidx.appcompat.widget.AppCompatButton artistsButton;
    private androidx.appcompat.widget.AppCompatButton albumsButton;
    private SearchView searchView;
    private androidx.appcompat.widget.Toolbar artistToolbar;
    private BottomNavigationView bottomNavigationView;
    
    // Service de lecture musicale
    private MusicPlayerService musicService;
    private boolean serviceBound = false;
    
    // Connexion au service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicBinder binder = (MusicPlayerService.MusicBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            
            // Enregistrer cette activité comme listener
            musicService.addListener(LibraryActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        likedSongsButton = findViewById(R.id.liked_songs_button);
        artistsButton = findViewById(R.id.artists_button);
        albumsButton = findViewById(R.id.albums_button);
        searchView = findViewById(R.id.search_view);
        artistToolbar = findViewById(R.id.artist_toolbar);


        // Sélectionner "Titres Likés" par défaut
        updateButtonStates(likedSongsButton, likedSongsButton, artistsButton, albumsButton);
        
        // Initialisation du menu de navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_library);

        // Démarrer et lier le service
        Intent intent = new Intent(this, MusicPlayerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        overridePendingTransition(0, 0); // Désactive l'animation

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

        // Configuration de la navigation avec if/else au lieu de switch/case
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                
                if (id == R.id.nav_player) {
                    Intent playerIntent = new Intent(LibraryActivity.this, PlayerActivity.class);
                    startActivity(playerIntent);
                    overridePendingTransition(0, 0); // Désactive l'animation
                    return true;
                } else if (id == R.id.nav_queue) {
                    Intent queueIntent = new Intent(LibraryActivity.this, QueueActivity.class);
                    startActivity(queueIntent);
                    overridePendingTransition(0, 0); // Désactive l'animation
                    return true;
                } else if (id == R.id.nav_library) {
                    // Déjà sur cette activité
                    return true;
                }
                return false;
            }
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
            if (serviceBound) {
                // Initialiser la queue avec toutes les chansons si elle est vide
                if (musicService.getQueue().isEmpty()) {
                    musicService.setQueue(allSongs);
                }
                
                // Trouver l'index de la chanson dans la queue
                List<Song> queue = musicService.getQueue();
                for (int i = 0; i < queue.size(); i++) {
                    if (queue.get(i).getTitle().equals(song.getTitle())) {
                        musicService.playQueueItem(i);
                        break;
                    }
                }
                
                // Naviguer vers le player
                Intent playerIntent = new Intent(this, PlayerActivity.class);
                startActivity(playerIntent);
                overridePendingTransition(0, 0); // Désactive l'animation
            }
        });
    }

    private void showLikedSongs() {
        loadLikedSongs();

        // Configurer le clic pour jouer les titres likés
        adapter.setOnItemClickListener(song -> {
            if (serviceBound) {
                // Initialiser la queue avec toutes les chansons si elle est vide
                List<Song> allSongs = CSVParser.parseCSV();
                if (musicService.getQueue().isEmpty()) {
                    musicService.setQueue(allSongs);
                }
                
                // Trouver l'index de la chanson dans la queue
                List<Song> queue = musicService.getQueue();
                for (int i = 0; i < queue.size(); i++) {
                    if (queue.get(i).getTitle().equals(song.getTitle())) {
                        musicService.playQueueItem(i);
                        break;
                    }
                }
                
                // Naviguer vers le player
                Intent playerIntent = new Intent(this, PlayerActivity.class);
                startActivity(playerIntent);
                overridePendingTransition(0, 0); // Désactive l'animation
            }
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
            if (serviceBound) {
                // Initialiser la queue avec les chansons de l'artiste
                if (musicService.getQueue().isEmpty()) {
                    musicService.setQueue(allSongs);
                }
                
                // Trouver l'index de la chanson dans la queue
                List<Song> queue = musicService.getQueue();
                for (int i = 0; i < queue.size(); i++) {
                    if (queue.get(i).getTitle().equals(song.getTitle())) {
                        musicService.playQueueItem(i);
                        break;
                    }
                }
                
                // Naviguer vers le player
                Intent playerIntent = new Intent(this, PlayerActivity.class);
                startActivity(playerIntent);
                overridePendingTransition(0, 0); // Désactive l'animation
            }
        });
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
            if (serviceBound) {
                // Initialiser la queue avec toutes les chansons si elle est vide
                if (musicService.getQueue().isEmpty()) {
                    musicService.setQueue(allSongs);
                }
                
                // Trouver l'index de la chanson dans la queue
                List<Song> queue = musicService.getQueue();
                for (int i = 0; i < queue.size(); i++) {
                    if (queue.get(i).getTitle().equals(song.getTitle())) {
                        musicService.playQueueItem(i);
                        break;
                    }
                }
                
                // Naviguer vers le player
                Intent playerIntent = new Intent(this, PlayerActivity.class);
                startActivity(playerIntent);
                overridePendingTransition(0, 0); // Désactive l'animation
            }
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

    // Implémentation des callbacks du service
    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        // Non utilisé dans cette activité
    }

    @Override
    public void onSongChanged(Song song) {
        // Non utilisé dans cette activité
    }

    @Override
    public void onProgressChanged(int position, int duration) {
        // Non utilisé dans cette activité
    }
    
    @Override
    public void onQueueChanged(List<Song> queue, int currentIndex) {
        // Non utilisé dans cette activité
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (serviceBound) {
            musicService.removeListener(this);
            unbindService(serviceConnection);
            serviceBound = false;
        }

        SharedPreferences prefs = getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
    private void updateButtonStates(Button activeButton, Button... allButtons) {
        for (Button button : allButtons) {
            button.setBackgroundColor(getResources().getColor(R.color.green)); // Couleur par défaut
            button.setTextColor(getResources().getColor(R.color.white)); // Texte par défaut
            button.setTypeface(null, android.graphics.Typeface.NORMAL); // Texte normal
        }
        activeButton.setTextColor(getResources().getColor(R.color.gray)); // Texte du bouton sélectionné
        activeButton.setBackgroundColor(getResources().getColor(R.color.dark_green)); // Couleur du bouton sélectionné
        activeButton.setTypeface(null, android.graphics.Typeface.BOLD); // Texte en gras
    }
}
