package iut.fspotify.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iut.fspotify.R;
import iut.fspotify.adapter.QueueAdapter;
import iut.fspotify.model.Song;
import iut.fspotify.services.MusicPlayerService;
import iut.fspotify.utils.CSVParser;

public class QueueActivity extends AppCompatActivity implements MusicPlayerService.OnMusicPlayerListener {

    private QueueAdapter adapter;
    private List<Song> songList = new ArrayList<>();
    private List<Song> filteredList = new ArrayList<>();
    private Map<Integer, Integer> filteredToOriginalIndexMap = new HashMap<>();
    private BottomNavigationView bottomNavigationView;
    private RecyclerView recyclerView;
    private boolean isSearchActive = false;
    
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
            musicService.addListener(QueueActivity.this);
            
            // Initialiser la queue si elle est vide
            if (musicService.getQueue().isEmpty()) {
                musicService.setQueue(songList);
            } else {
                // Sinon, utiliser la queue existante
                updateQueueFromService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        recyclerView = findViewById(R.id.queue_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialisation du menu de navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_queue);

        // Démarrer et lier le service
        Intent intent = new Intent(this, MusicPlayerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        overridePendingTransition(0, 0); // Désactive l'animation

        // Charger les titres depuis le CSV si nécessaire
        if (songList.isEmpty()) {
            songList = CSVParser.parseCSV();
            filteredList.addAll(songList);
        }

        // Configuration du ItemTouchHelper pour le drag & drop
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                 @NonNull RecyclerView.ViewHolder viewHolder, 
                                 @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                // Ne pas permettre le drag & drop pendant la recherche
                if (isSearchActive) {
                    return false;
                }
                
                // Mettre à jour l'adapter
                adapter.moveItem(fromPosition, toPosition);
                
                // Mettre à jour la queue dans le service
                if (serviceBound) {
                    musicService.setQueue(adapter.getSongList());
                    
                    // Si le morceau en cours a été déplacé, mettre à jour l'index
                    if (adapter.getCurrentPlayingPosition() == toPosition) {
                        musicService.playQueueItem(toPosition);
                    }
                }
                
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Non utilisé
            }
            
            // Permettre le déplacement sur plusieurs positions
            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        };
        
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        // Initialisation de l'adapter
        adapter = new QueueAdapter(filteredList, (song, position) -> {
            if (serviceBound) {
                // Si la recherche est active, trouver l'index réel dans la queue
                if (isSearchActive) {
                    // Trouver la chanson dans la queue complète
                    List<Song> fullQueue = musicService.getQueue();
                    for (int i = 0; i < fullQueue.size(); i++) {
                        if (fullQueue.get(i).getTitle().equals(song.getTitle()) && 
                            fullQueue.get(i).getArtist().equals(song.getArtist())) {
                            // Jouer la chanson à son index réel
                            musicService.playQueueItem(i);
                            break;
                        }
                    }
                } else {
                    // Jouer la chanson sélectionnée à sa position dans la liste
                    musicService.playQueueItem(position);
                }
                
                // Naviguer vers le player
                Intent playerIntent = new Intent(this, PlayerActivity.class);
                startActivity(playerIntent);
                overridePendingTransition(0, 0); // Désactive l'animation
            }
        }, touchHelper);
        recyclerView.setAdapter(adapter);

        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                isSearchActive = !TextUtils.isEmpty(newText);
                filterList(newText);
                return true;
            }
        });

        // Configuration de la navigation avec if/else au lieu de switch/case
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                
                if (id == R.id.nav_player) {
                    Intent playerIntent = new Intent(QueueActivity.this, PlayerActivity.class);
                    startActivity(playerIntent);
                    overridePendingTransition(0, 0); // Désactive l'animation
                    return true;
                } else if (id == R.id.nav_queue) {
                    // Déjà sur cette activité
                    return true;
                } else if (id == R.id.nav_library) {
                    Intent libraryIntent = new Intent(QueueActivity.this, LibraryActivity.class);
                    startActivity(libraryIntent);
                    overridePendingTransition(0, 0); // Désactive l'animation
                    return true;
                }
                return false;
            }
        });
    }

    private void filterList(String query) {
        filteredList.clear();
        filteredToOriginalIndexMap.clear();
        
        if (TextUtils.isEmpty(query)) {
            isSearchActive = false;
            // Si la recherche est vide, afficher toute la queue
            List<Song> serviceQueue = musicService.getQueue();
            filteredList.addAll(serviceQueue);
            
            // Mettre à jour l'index du morceau en cours
            adapter.setCurrentPlayingPosition(musicService.getCurrentQueueIndex());
        } else {
            isSearchActive = true;
            // Filtrer la liste selon la requête
            List<Song> serviceQueue = musicService.getQueue();
            for (int i = 0; i < serviceQueue.size(); i++) {
                Song song = serviceQueue.get(i);
                if (song.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        song.getArtist().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(song);
                    filteredToOriginalIndexMap.put(filteredList.size() - 1, i);
                    
                    // Si c'est le morceau en cours, mettre à jour l'index dans la liste filtrée
                    if (i == musicService.getCurrentQueueIndex()) {
                        adapter.setCurrentPlayingPosition(filteredList.size() - 1);
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
    
    private void updateQueueFromService() {
        if (serviceBound) {
            // Récupérer la queue actuelle du service
            List<Song> serviceQueue = musicService.getQueue();
            int currentIndex = musicService.getCurrentQueueIndex();
            
            // Mettre à jour la liste locale
            filteredList.clear();
            filteredList.addAll(serviceQueue);
            
            // Mettre à jour l'adapter
            adapter.setCurrentPlayingPosition(currentIndex);
            adapter.notifyDataSetChanged();
        }
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
        runOnUiThread(() -> {
            // Si la recherche est active, ne pas mettre à jour la liste filtrée
            if (!isSearchActive) {
                filteredList.clear();
                filteredList.addAll(queue);
                adapter.setCurrentPlayingPosition(currentIndex);
                adapter.notifyDataSetChanged();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (serviceBound && !isSearchActive) {
            updateQueueFromService();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            musicService.removeListener(this);
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
