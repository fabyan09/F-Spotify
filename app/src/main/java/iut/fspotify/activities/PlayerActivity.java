package iut.fspotify.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import iut.fspotify.R;
import iut.fspotify.model.Song;
import iut.fspotify.utils.CSVParser;

public class PlayerActivity extends AppCompatActivity {
    private static MediaPlayer mediaPlayer;
    private static List<Song> songList;
    private static int currentIndex = 0;

    private boolean showingLyrics = false;
    private SharedPreferences prefs;

    private ImageView cover;
    private ScrollView lyricsScroll;
    private TextView lyrics, title, artistAlbumText, total_duration, current_time;
    private ImageButton play, next, prev, forward, rewind, likeButton;
    private ImageButton navPlayerButton, navQueueButton, navLibraryButton;
    private SeekBar seekBar;
    private Handler handler = new Handler();
    private Runnable updateSeekBar;

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
        
        // Initialisation des boutons de navigation
        navPlayerButton = findViewById(R.id.nav_player_button);
        navQueueButton = findViewById(R.id.nav_queue_button);
        navLibraryButton = findViewById(R.id.nav_library_button);

        prefs = getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);

        // Récupération de la chanson depuis l'intent si disponible
        if (getIntent().hasExtra("SONG")) {
            Song song = getIntent().getParcelableExtra("SONG");
            if (song != null) {
                // Correction: Toujours arrêter et libérer le lecteur précédent
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                songList = List.of(song);
                currentIndex = 0;
                loadSong(currentIndex);
            }
        } else {
            // Sinon, charger la liste complète
            if (songList == null) {
                songList = CSVParser.parseCSV();
            }

            if (!songList.isEmpty()) {
                loadSong(currentIndex);
            }
        }

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
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    play.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    mediaPlayer.start();
                    play.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        });

        next.setOnClickListener(v -> {
            if (currentIndex < songList.size() - 1) {
                currentIndex++;
                loadSong(currentIndex);
            }
        });

        prev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                loadSong(currentIndex);
            }
        });

        forward.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                int newPos = mediaPlayer.getCurrentPosition() + 10000;
                mediaPlayer.seekTo(Math.min(newPos, mediaPlayer.getDuration()));
            }
        });

        rewind.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                int newPos = mediaPlayer.getCurrentPosition() - 10000;
                mediaPlayer.seekTo(Math.max(newPos, 0));
            }
        });

        likeButton.setOnClickListener(v -> {
            if (songList != null && !songList.isEmpty()) {
                Song song = songList.get(currentIndex);
                String key = song.title.trim().toLowerCase();

                boolean alreadyLiked = prefs.getBoolean(key, false);
                prefs.edit().putBoolean(key, !alreadyLiked).apply();
                updateLikeIcon(key);

                Toast.makeText(this, alreadyLiked ? "Retiré des likés" : "Ajouté aux likés", Toast.LENGTH_SHORT).show();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Configuration de la navigation
        navPlayerButton.setOnClickListener(v -> {
            // Déjà sur cette activité, ne rien faire
        });

        navQueueButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, QueueActivity.class);
            startActivity(intent);
            finish(); // Fermer l'activité actuelle
        });

        navLibraryButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LibraryActivity.class);
            startActivity(intent);
            finish(); // Fermer l'activité actuelle
        });
    }

    private void loadSong(int index) {
        boolean wasPlaying = mediaPlayer != null && mediaPlayer.isPlaying();

        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBar);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        Song song = songList.get(index);
        title.setText(song.title);
        artistAlbumText.setText(song.getArtist() + " - " + song.getAlbum());
        
        // Correction: Centrer les paroles et assurer qu'elles sont scrollables
        String formattedLyrics = song.lyrics.replace(";", "\n\n");
        lyrics.setText(formattedLyrics);
        lyrics.setGravity(android.view.Gravity.CENTER);
        
        total_duration.setText(formatTime((int) song.duration * 1000));
        current_time.setText("0:00");

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

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource("http://edu.info06.net/lyrics/mp3/" + song.mp3);
            mediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mediaPlayer.getDuration());
                play.setImageResource(android.R.drawable.ic_media_play); // Icône de lecture par défaut

                // Afficher la durée totale
                int duration = mediaPlayer.getDuration();
                total_duration.setText(formatTime(duration));

                if (wasPlaying) {
                    mediaPlayer.start(); // Reprendre la lecture si elle était en cours
                    play.setImageResource(android.R.drawable.ic_media_pause); // Icône de pause
                }
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                if (currentIndex < songList.size() - 1) {
                    currentIndex++;
                    loadSong(currentIndex);

                    // Démarrer automatiquement la lecture après préparation
                    mediaPlayer.setOnPreparedListener(mpPrepared -> {
                        mediaPlayer.start();
                        play.setImageResource(android.R.drawable.ic_media_pause); // Mettre à jour l'icône
                    });
                }
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                return true;
            });
            mediaPlayer.prepareAsync(); // Préparation asynchrone
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "URL invalide, lecture impossible", Toast.LENGTH_SHORT).show();
            play.setEnabled(false); // Désactiver le bouton play
        }

        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    // Mettre à jour le temps actuel
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    current_time.setText(formatTime(currentPosition));
                    seekBar.setProgress(currentPosition);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(updateSeekBar);

        updateLikeIcon(song.title.trim().toLowerCase());
        cover.setVisibility(View.VISIBLE);
        lyricsScroll.setVisibility(View.GONE);
        showingLyrics = false;
    }

    // Méthode utilitaire pour formater le temps en mm:ss
    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void updateLikeIcon(String key) {
        boolean liked = prefs.getBoolean(key, false);
        Log.d("PLAYER", "updateLikeIcon: " + key + " = " + liked);
        likeButton.setImageResource(liked ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBar);
            // Correction: Libérer le MediaPlayer pour éviter les problèmes de lecture
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // Méthode statique pour jouer une chanson depuis une autre activité
    public static void playSong(Context context, Song song) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra("SONG", song);
        context.startActivity(intent);
    }
}
