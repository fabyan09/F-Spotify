package iut.fspotify.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import iut.fspotify.R;
import iut.fspotify.model.Song;
import iut.fspotify.services.MusicPlayerService;
import iut.fspotify.utils.CSVParser;

public class PlayerActivity extends AppCompatActivity implements MusicPlayerService.OnMusicPlayerListener {
    private static final String TAG = "PlayerActivity";
    
    // Service de lecture musicale
    private MusicPlayerService musicService;
    private boolean serviceBound = false;
    
    // Liste des chansons
    private static List<Song> songList;
    private static int currentIndex = 0;
    
    // UI
    private boolean showingLyrics = false;
    private SharedPreferences prefs;
    private ImageView cover;
    private ScrollView lyricsScroll;
    private TextView lyrics, title, artistAlbumText, total_duration, current_time;
    private ImageButton play, next, prev, forward, rewind, likeButton;
    private SeekBar seekBar;
    private Handler handler = new Handler();
    private Runnable updateSeekBar;
    private BottomNavigationView bottomNavigationView;
    
    // Connexion au service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicBinder binder = (MusicPlayerService.MusicBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            
            // Enregistrer cette activité comme listener
            musicService.addListener(PlayerActivity.this);
            
            // Vérifier si une chanson est déjà en cours de lecture
            Song currentSong = musicService.getCurrentSong();
            if (currentSong != null) {
                // Mettre à jour l'UI avec la chanson en cours
                updateUI(currentSong);
                updatePlayPauseButton(musicService.isPlaying());
            } else if (getIntent().hasExtra("SONG")) {
                // Charger la chanson depuis l'intent
                Song song = (Song) getIntent().getSerializableExtra("SONG");
                if (song != null) {
                    songList = List.of(song);
                    currentIndex = 0;
                    musicService.loadSong(song);
                }
            } else if (songList != null && !songList.isEmpty()) {
                // Utiliser la liste existante
                musicService.loadSong(songList.get(currentIndex));
            } else {
                // Charger la liste complète
                songList = CSVParser.parseCSV();
                if (!songList.isEmpty()) {
                    musicService.loadSong(songList.get(currentIndex));
                }
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
        setContentView(R.layout.activity_player);

        // Initialisation des vues
        cover = findViewById(R.id.cover_image);
        lyrics = findViewById(R.id.lyrics_text);
        lyricsScroll = findViewById(R.id.lyrics_scroll);
        title = findViewById(R.id.title_text);
        artistAlbumText = findViewById(R.id.artist_album_text);
        play = findViewById(R.id.play_button);
        next = findViewById(R.id.next_button);
        prev = findViewById(R.id.prev_button);
        forward = findViewById(R.id.forward_button);
        rewind = findViewById(R.id.rewind_button);
        likeButton = findViewById(R.id.like_button);
        seekBar = findViewById(R.id.seek_bar);
        current_time = findViewById(R.id.current_time);
        total_duration = findViewById(R.id.total_duration);
        FrameLayout mediaContainer = findViewById(R.id.media_container);
        
        // Initialisation du menu de navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_player);

        prefs = getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);

        // Démarrer et lier le service
        Intent intent = new Intent(this, MusicPlayerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        overridePendingTransition(0, 0); // Désactive l'animation

        // Configuration des listeners
        mediaContainer.setOnClickListener(v -> {
            showingLyrics = !showingLyrics;
            cover.setVisibility(showingLyrics ? View.GONE : View.VISIBLE);
            lyricsScroll.setVisibility(showingLyrics ? View.VISIBLE : View.GONE);
        });

        lyrics.setOnClickListener(v -> {
            showingLyrics = false;
            lyricsScroll.setVisibility(View.GONE);
            cover.setVisibility(View.VISIBLE);
        });

        play.setOnClickListener(v -> {
            if (serviceBound) {
                musicService.playPause();
            }
        });

        next.setOnClickListener(v -> {
            if (currentIndex < songList.size() - 1) {
                currentIndex++;
                if (serviceBound) {
                    musicService.loadSong(songList.get(currentIndex));
                }
            }
        });

        prev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                if (serviceBound) {
                    musicService.loadSong(songList.get(currentIndex));
                }
            }
        });

        forward.setOnClickListener(v -> {
            if (serviceBound) {
                musicService.seekForward();
            }
        });

        rewind.setOnClickListener(v -> {
            if (serviceBound) {
                musicService.seekBackward();
            }
        });

        // Correction du bug de like : utiliser la chanson actuelle du service
        likeButton.setOnClickListener(v -> {
            if (serviceBound && musicService.getCurrentSong() != null) {
                Song currentSong = musicService.getCurrentSong();
                String key = currentSong.title.trim().toLowerCase();

                boolean alreadyLiked = prefs.getBoolean(key, false);
                prefs.edit().putBoolean(key, !alreadyLiked).apply();
                updateLikeIcon(key);

                Toast.makeText(this, alreadyLiked ? "Retiré des likés" : "Ajouté aux likés", Toast.LENGTH_SHORT).show();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serviceBound) {
                    musicService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Configuration de la navigation avec if/else au lieu de switch/case
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                
                if (id == R.id.nav_player) {
                    // Déjà sur cette activité
                    return true;
                } else if (id == R.id.nav_queue) {
                    Intent queueIntent = new Intent(PlayerActivity.this, QueueActivity.class);
                    startActivity(queueIntent);
                    return true;
                } else if (id == R.id.nav_library) {
                    Intent libraryIntent = new Intent(PlayerActivity.this, LibraryActivity.class);
                    startActivity(libraryIntent);
                    return true;
                }
                return false;
            }
        });
    }

    private void updateUI(Song song) {
        title.setText(song.title);
        artistAlbumText.setText(song.getArtist() + " - " + song.getAlbum());
        
        // Formater les paroles pour l'affichage
        String formattedLyrics = song.lyrics.replace(";", "\n");
        lyrics.setText(formattedLyrics);
        
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        try {
            URL url = new URL("http://edu.info06.net/lyrics/images/" + song.cover);
            InputStream input = url.openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            cover.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            cover.setImageResource(R.drawable.placeholder); // Image par défaut
        }
        
        updateLikeIcon(song.title.trim().toLowerCase());
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        play.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    // Méthode utilitaire pour formater le temps en mm:ss
    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void updateLikeIcon(String key) {
        boolean liked = prefs.getBoolean(key, false);
        Log.d(TAG, "updateLikeIcon: " + key + " = " + liked);
        likeButton.setImageResource(liked ? R.drawable.like : R.drawable.empty_like);
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

    // Méthode statique pour jouer une chanson depuis une autre activité
    public static void playSong(Context context, Song song) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra("SONG", song);
        context.startActivity(intent);

    }

    // Implémentation des callbacks du service
    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        runOnUiThread(() -> updatePlayPauseButton(isPlaying));
    }

    @Override
    public void onSongChanged(Song song) {
        runOnUiThread(() -> updateUI(song));
    }

    @Override
    public void onProgressChanged(int position, int duration) {
        runOnUiThread(() -> {
            seekBar.setMax(duration);
            seekBar.setProgress(position);
            current_time.setText(formatTime(position));
            total_duration.setText(formatTime(duration));
        });
    }
}
