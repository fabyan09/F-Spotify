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
import androidx.fragment.app.Fragment;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import iut.fspotify.R;
import iut.fspotify.model.Song;
import iut.fspotify.utils.CSVParser;

public class PlayerFragment extends Fragment {
    private static MediaPlayer mediaPlayer;
    private static boolean showingLyrics = false;
    private static int currentIndex = 0;
    private static List<Song> songList;
    private SharedPreferences prefs;

    private ImageView cover;
    private ScrollView lyricsScroll;
    private TextView lyrics, title;
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

        SharedPreferences prefs = requireContext().getSharedPreferences("PLAYER_STATE", Context.MODE_PRIVATE);
        currentIndex = prefs.getInt("currentIndex", 0);
        int savedPosition = prefs.getInt("currentPosition", 0);

        if (mediaPlayer == null && !songList.isEmpty()) {
            loadSong(currentIndex);
            mediaPlayer.seekTo(savedPosition);
        } else if (mediaPlayer != null) {
            updateUI(); // met à jour l’UI sans recharger
        }


        mediaContainer.setOnClickListener(v -> {
            showingLyrics = !showingLyrics;
            cover.setVisibility(showingLyrics ? View.GONE : View.VISIBLE);
            lyricsScroll.setVisibility(showingLyrics ? View.VISIBLE : View.GONE);
        });

        play.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                else mediaPlayer.start();
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
            Song song = songList.get(currentIndex);
            boolean alreadyLiked = prefs.getBoolean(song.title, false);
            prefs.edit().putBoolean(song.title, !alreadyLiked).apply();
            Toast.makeText(getContext(), alreadyLiked ? "Retiré des likés" : "Ajouté aux likés", Toast.LENGTH_SHORT).show();
            updateLikeIcon(song.title);
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
        lyrics.setText(song.lyrics);

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
            mediaPlayer.prepare();
            mediaPlayer.start();
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

        updateLikeIcon(song.title);
        cover.setVisibility(View.VISIBLE);
        lyricsScroll.setVisibility(View.GONE);
        showingLyrics = false;
    }

    private void updateLikeIcon(String title) {
        boolean liked = prefs.getBoolean(title, false);
        likeButton.setImageResource(liked ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            int position = mediaPlayer.getCurrentPosition();
            SharedPreferences prefs = requireContext().getSharedPreferences("PLAYER_STATE", Context.MODE_PRIVATE);
            prefs.edit()
                    .putInt("currentIndex", currentIndex)
                    .putInt("currentPosition", position)
                    .apply();
        }
    }

    private void updateUI() {
        Song song = songList.get(currentIndex);
        title.setText(song.title);
        lyrics.setText(song.lyrics);
        updateLikeIcon(song.title);
        cover.setVisibility(View.VISIBLE);
        lyricsScroll.setVisibility(View.GONE);
        showingLyrics = false;

        // Recharge l'image sans recréer le player
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        try {
            URL url = new URL("http://edu.info06.net/lyrics/images/" + song.cover);
            InputStream input = url.openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            cover.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Recharge la seekBar
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
    }


}
