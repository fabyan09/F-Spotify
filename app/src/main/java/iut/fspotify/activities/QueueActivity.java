package iut.fspotify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import iut.fspotify.R;
import iut.fspotify.adapter.QueueAdapter;
import iut.fspotify.model.Song;
import iut.fspotify.utils.CSVParser;

public class QueueActivity extends AppCompatActivity {

    private QueueAdapter adapter;
    private List<Song> songList = new ArrayList<>();
    private List<Song> filteredList = new ArrayList<>();
    private ImageButton navPlayerButton, navQueueButton, navLibraryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        RecyclerView recyclerView = findViewById(R.id.queue_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialisation des boutons de navigation
        navPlayerButton = findViewById(R.id.nav_player_button);
        navQueueButton = findViewById(R.id.nav_queue_button);
        navLibraryButton = findViewById(R.id.nav_library_button);

        songList = CSVParser.parseCSV(); // Charger les titres depuis le CSV
        filteredList.addAll(songList); // Initialiser la liste filtrée
        adapter = new QueueAdapter(filteredList, song -> {
            PlayerActivity.playSong(this, song);
        });
        recyclerView.setAdapter(adapter);

        SearchView searchView = findViewById(R.id.search_view);
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

        // Configuration de la navigation
        navPlayerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
            finish(); // Fermer l'activité actuelle
        });

        navQueueButton.setOnClickListener(v -> {
            // Déjà sur cette activité, ne rien faire
        });

        navLibraryButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LibraryActivity.class);
            startActivity(intent);
            finish(); // Fermer l'activité actuelle
        });
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
