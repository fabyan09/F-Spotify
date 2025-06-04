package iut.fspotify.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
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

    // UI
    private boolean showingLyrics = false;
    private SharedPreferences prefs;
    private SharedPreferences ratingPrefs; // Pour stocker les notes des chansons
    private ImageView cover, aura;
    private ScrollView lyricsScroll;
    private TextView lyrics, title, artistAlbumText, total_duration, current_time;
    private ImageButton play, next, prev, forward, rewind, likeButton, infoButton;
    private SeekBar seekBar;
    private Handler handler = new Handler();
    private Runnable updateSeekBar;
    private BottomNavigationView bottomNavigationView;
    
    // Variables pour les animations
    private boolean isAnimatingCover = false;
    private ImageView nextCoverImage;
    private FrameLayout mediaContainer;
    private int lastDirection = 0; // 1 pour next, -1 pour previous
    private Song nextSong = null; // Pour stocker la chanson suivante pendant l'animation

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

                // Charger la note de la chanson si elle existe
                loadSongRating(currentSong);
            } else if (getIntent().hasExtra("SONG")) {
                // Charger la chanson depuis l'intent
                Song song = (Song) getIntent().getSerializableExtra("SONG");
                if (song != null) {
                    // Initialiser la queue avec cette chanson si elle est vide
                    if (musicService.getQueue().isEmpty()) {
                        List<Song> allSongs = CSVParser.parseCSV();
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
                }
            } else if (musicService.getQueue().isEmpty()) {
                // Initialiser la queue avec toutes les chansons
                List<Song> allSongs = CSVParser.parseCSV();
                musicService.setQueue(allSongs);
                if (!allSongs.isEmpty()) {
                    musicService.playQueueItem(0);
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
        aura = findViewById(R.id.aura_image); // Image de l'aura
        title = findViewById(R.id.title_text);
        artistAlbumText = findViewById(R.id.artist_album_text);
        play = findViewById(R.id.play_button);
        next = findViewById(R.id.next_button);
        prev = findViewById(R.id.prev_button);
        forward = findViewById(R.id.forward_button);
        rewind = findViewById(R.id.rewind_button);
        likeButton = findViewById(R.id.like_button);
        infoButton = findViewById(R.id.info_button); // Nouveau bouton d'information
        seekBar = findViewById(R.id.seek_bar);
        current_time = findViewById(R.id.current_time);
        total_duration = findViewById(R.id.total_duration);
        mediaContainer = findViewById(R.id.media_container);
        
        // Création de l'ImageView pour l'animation de transition
        nextCoverImage = new ImageView(this);
        nextCoverImage.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        nextCoverImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        nextCoverImage.setVisibility(View.GONE);
        mediaContainer.addView(nextCoverImage);

        // Initialisation du menu de navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_player);

        prefs = getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);
        ratingPrefs = getSharedPreferences("SONG_RATINGS", Context.MODE_PRIVATE);

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
            aura.setVisibility(showingLyrics ? View.GONE : View.VISIBLE);
        });

        lyrics.setOnClickListener(v -> {
            showingLyrics = false;
            lyricsScroll.setVisibility(View.GONE);
            cover.setVisibility(View.VISIBLE);
            aura.setVisibility(View.VISIBLE);
        });

        play.setOnClickListener(v -> {
            if (serviceBound) {
                // Animation du bouton play/pause
                animatePlayPauseButton();
                musicService.playPause();
            }
        });

        next.setOnClickListener(v -> {
            if (serviceBound) {
                lastDirection = 1; // Direction vers la droite (next)
                musicService.playNextInQueue();
            }
        });

        prev.setOnClickListener(v -> {
            if (serviceBound) {
                lastDirection = -1; // Direction vers la gauche (previous)
                musicService.playPreviousInQueue();
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

        // Configuration du bouton d'information
        infoButton.setOnClickListener(v -> {
            if (serviceBound && musicService.getCurrentSong() != null) {
                showSongInfoDialog(musicService.getCurrentSong());
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
                    overridePendingTransition(0, 0); // Désactive l'animation
                    return true;
                } else if (id == R.id.nav_library) {
                    Intent libraryIntent = new Intent(PlayerActivity.this, LibraryActivity.class);
                    startActivity(libraryIntent);
                    overridePendingTransition(0, 0); // Désactive l'animation
                    return true;
                }
                return false;
            }
        });
    }
    
    // Animation du bouton play/pause
    private void animatePlayPauseButton() {
        // Créer une animation de rotation et de mise à l'échelle
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(play, "scaleX", 1f, 0.8f, 1f);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(play, "scaleY", 1f, 0.8f, 1f);
        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(play, "rotation", 0f, 360f);
        
        // Configurer les animations
        scaleXAnimator.setDuration(300);
        scaleYAnimator.setDuration(300);
        rotateAnimator.setDuration(300);
        
        // Créer un ensemble d'animations
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator, rotateAnimator);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // Démarrer l'animation
        animatorSet.start();
    }
    
    // Animation de transition de la cover
    private void animateCoverTransition(Song newSong, int direction) {
        if (isAnimatingCover || showingLyrics) {
            // Si une animation est déjà en cours ou si on affiche les paroles, on ne fait rien
            updateUIWithoutAnimation(newSong);
            return;
        }
        
        isAnimatingCover = true;
        nextSong = newSong;
        
        // Mettre à jour immédiatement le titre et les informations de la chanson
        updateSongInfo(newSong);
        
        // Préparer la nouvelle image
        nextCoverImage.setVisibility(View.VISIBLE);
        nextCoverImage.setAlpha(1f);
        
        // Charger l'image de la nouvelle chanson dans nextCoverImage
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        try {
            URL url = new URL("http://edu.info06.net/lyrics/images/" + newSong.cover);
            InputStream input = url.openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            nextCoverImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            nextCoverImage.setImageResource(R.drawable.placeholder);
        }
        
        // Positionner l'image hors écran selon la direction
        float screenWidth = getResources().getDisplayMetrics().widthPixels;
        nextCoverImage.setTranslationX(direction * screenWidth);
        
        // S'assurer que les deux images ont exactement la même taille
        nextCoverImage.setScaleType(cover.getScaleType());
        nextCoverImage.setPadding(cover.getPaddingLeft()+5, cover.getPaddingTop()+26,
                                 cover.getPaddingRight()+2, cover.getPaddingBottom()+26);
        
        // Animer la sortie de l'image actuelle
        ObjectAnimator exitAnimator = ObjectAnimator.ofFloat(
                cover, "translationX", 0f, -direction * screenWidth);
        exitAnimator.setDuration(500);
        exitAnimator.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator exitAuraAnimator = ObjectAnimator.ofFloat(
                aura, "translationX", 0f, -direction * screenWidth);
        exitAuraAnimator.setDuration(500);
        exitAuraAnimator.setInterpolator(new DecelerateInterpolator());
        
        // Animer l'entrée de la nouvelle image
        ObjectAnimator enterAnimator = ObjectAnimator.ofFloat(
                nextCoverImage, "translationX", direction * screenWidth, 0f);
        enterAnimator.setDuration(500);
        enterAnimator.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator enterAuraAnimator = ObjectAnimator.ofFloat(
                aura, "translationX", direction * screenWidth, 0f);
        enterAuraAnimator.setDuration(500);
        enterAuraAnimator.setInterpolator(new DecelerateInterpolator());
        
        // Créer un ensemble d'animations
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(exitAnimator, enterAnimator, exitAuraAnimator, enterAuraAnimator);
        
        // Définir un listener pour la fin de l'animation
        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Échanger les images
                cover.setImageDrawable(nextCoverImage.getDrawable());
                cover.setTranslationX(0f);
                nextCoverImage.setVisibility(View.GONE);
                
                // Mettre à jour les paroles
                if (nextSong != null) {
                    String formattedLyrics = nextSong.lyrics.replace(";", "\n");
                    lyrics.setText(formattedLyrics);
                }
                
                isAnimatingCover = false;
                nextSong = null;
            }
        });
        
        // Démarrer l'animation
        animatorSet.start();
    }
    
    // Mise à jour des informations de la chanson sans animer la cover
    private void updateUIWithoutAnimation(Song song) {
        updateSongInfo(song);
        
        // Mettre à jour les paroles
        String formattedLyrics = song.lyrics.replace(";", "\n");
        lyrics.setText(formattedLyrics);
        
        // Mettre à jour la cover
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
        loadSongRating(song);
    }
    
    // Mise à jour uniquement des informations textuelles de la chanson
    private void updateSongInfo(Song song) {
        title.setText(song.title);
        artistAlbumText.setText(song.getArtist() + " - " + song.getAlbum());
        updateLikeIcon(song.title.trim().toLowerCase());
        loadSongRating(song);
    }

    // Méthode pour afficher le dialogue d'information sur la chanson
    private void showSongInfoDialog(Song song) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.song_info_dialog);

        // Récupérer les vues du dialogue
        TextView infoTitle = dialog.findViewById(R.id.info_title);
        TextView infoArtist = dialog.findViewById(R.id.info_artist);
        TextView infoAlbum = dialog.findViewById(R.id.info_album);
        TextView infoDate = dialog.findViewById(R.id.info_date);
        TextView infoDuration = dialog.findViewById(R.id.info_duration);
        TextView infoFile = dialog.findViewById(R.id.info_file);
        RatingBar ratingBar = dialog.findViewById(R.id.rating_bar);
        Button closeButton = dialog.findViewById(R.id.close_button);

        // Remplir les informations
        infoTitle.setText(song.getTitle());
        infoArtist.setText(song.getArtist());
        infoAlbum.setText(song.getAlbum());
        infoDate.setText(song.getDate());
        infoDuration.setText(formatTime((int) (song.getDuration() * 60000)));
        infoFile.setText(song.getMp3());

        // Charger la note existante
        String ratingKey = getSongRatingKey(song);
        float rating = ratingPrefs.getFloat(ratingKey, 0);
        ratingBar.setRating(rating);

        // Configurer le listener de la barre de notation
        ratingBar.setOnRatingBarChangeListener((rBar, value, fromUser) -> {
            SharedPreferences.Editor editor = ratingPrefs.edit();
            editor.putFloat(ratingKey, value);
            editor.apply();
        });

        // Configurer le bouton de fermeture
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Afficher le dialogue avec une taille adaptée en mode paysage
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.show();
    }
    // Méthode pour générer une clé unique pour la note d'une chanson
    private String getSongRatingKey(Song song) {
        return song.getTitle().trim().toLowerCase() + "_" +
                song.getArtist().trim().toLowerCase();
    }

    // Méthode pour sauvegarder la note d'une chanson
    private void saveSongRating(Song song, float rating) {
        String key = getSongRatingKey(song);
        ratingPrefs.edit().putFloat(key, rating).apply();

        // Mettre à jour la note dans l'objet Song
        song.setRating(rating);
    }

    // Méthode pour charger la note d'une chanson
    private void loadSongRating(Song song) {
        String key = getSongRatingKey(song);
        float rating = ratingPrefs.getFloat(key, 0);
        song.setRating(rating);
    }

    private void updateUI(Song song) {
        // Si nous ne sommes pas en train d'animer la cover, la mettre à jour directement
        if (!isAnimatingCover) {
            updateUIWithoutAnimation(song);
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        play.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    // Méthode utilitaire améliorée pour formater le temps en mm:ss
    private String formatTime(int millis) {
        // S'assurer que la valeur est positive
        millis = Math.max(0, millis);

        // Calculer les minutes et secondes
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;

        // Formater avec les zéros de remplissage pour les secondes
        return String.format("%d:%02d", minutes, seconds);
    }

    private void updateLikeIcon(String key) {
        boolean liked = prefs.getBoolean(key, false);
        // Suppression du log inutile
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
        runOnUiThread(() -> {
            // Animer la transition de la cover si une direction a été définie
            if (lastDirection != 0) {
                animateCoverTransition(song, lastDirection);
                lastDirection = 0; // Réinitialiser la direction
            } else {
                // Mise à jour normale sans animation
                updateUI(song);
            }
        });
    }

    @Override
    public void onProgressChanged(int position, int duration) {
        runOnUiThread(() -> {
            seekBar.setMax(duration);
            seekBar.setProgress(position);
            current_time.setText(formatTime(position));
            total_duration.setText(formatTime(duration));
            
            // Suppression du log inutile qui polluait le logcat
        });
    }

    @Override
    public void onQueueChanged(List<Song> queue, int currentIndex) {
        // Non utilisé dans cette activité
    }
}
