package iut.fspotify.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import iut.fspotify.R;
import iut.fspotify.model.Song;
import iut.fspotify.utils.CSVParser;

public class PlayerFragment extends Fragment {
    private static MediaPlayer mediaPlayer;
    private static List<Song> songList;
    private static int currentIndex = 0;

    private boolean showingLyrics = false;
    private SharedPreferences prefs;

    private ImageView cover;
    private ScrollView lyricsScroll;
    private TextView lyrics, title, artistAlbumText;
    private ImageButton play, next, prev, forward, rewind, likeButton;
    private SeekBar seekBar;
    private Handler handler = new Handler();
    private Runnable updateSeekBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        cover = view.findViewById(R.id.cover_image);
        lyrics = view.findViewById(R.id.lyrics_text);
        lyricsScroll = view.findViewById(R.id.lyrics_scroll);
        title = view.findViewById(R.id.title_text);
        artistAlbumText = view.findViewById(R.id.artist_album_text);
        play = view.findViewById(R.id.play_button);
        next = view.findViewById(R.id.next_button);
        prev = view.findViewById(R.id.prev_button);
        forward = view.findViewById(R.id.forward_button);
        rewind = view.findViewById(R.id.rewind_button);
        likeButton = view.findViewById(R.id.like_button);
        seekBar = view.findViewById(R.id.seek_bar);
        FrameLayout mediaContainer = view.findViewById(R.id.media_container);

        prefs = requireContext().getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);

        if (songList == null) {
            songList = CSVParser.parseCSV();
        }

        if (!songList.isEmpty() && mediaPlayer == null) {
            loadSong(currentIndex);
        }

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

                Toast.makeText(getContext(), alreadyLiked ? "Retiré des likés" : "Ajouté aux likés", Toast.LENGTH_SHORT).show();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        return view;
    }

    private void loadSong(int index) {
        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBar);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        Song song = songList.get(index);
        title.setText(song.title);
        artistAlbumText.setText(song.getArtist() + " - " + song.getAlbum());
        lyrics.setText(song.lyrics.replace(";", "\n"));

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        try {
            URL url = new URL("http://edu.info06.net/lyrics/images/" + song.cover);
            InputStream input = url.openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            cover.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource("http://edu.info06.net/lyrics/mp3/" + song.mp3);
            mediaPlayer.prepare(); // ne pas auto-play
            play.setImageResource(android.R.drawable.ic_media_play);
        } catch (Exception e) {
            e.printStackTrace();
        }

        seekBar.setMax(mediaPlayer.getDuration());

        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
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

    public void playSong(Song song) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        songList = List.of(song);
        currentIndex = 0;
        loadSong(0);
    }


    private void updateLikeIcon(String key) {
        boolean liked = prefs.getBoolean(key, false);
        Log.d("PLAYER", "updateLikeIcon: " + key + " = " + liked);
        likeButton.setImageResource(liked ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star);
    }

    public static void playExternalSong(Context context, Song song) {
        currentIndex = 0; // ou -1 si besoin
        songList = List.of(song); // on charge uniquement cette chanson
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // On force le retour au PlayerFragment
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            PlayerFragment fragment = new PlayerFragment();
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();

            // Sélectionner onglet player
            BottomNavigationView nav = activity.findViewById(R.id.bottom_navigation);
            nav.setSelectedItemId(R.id.nav_player);
        }
    }

}
