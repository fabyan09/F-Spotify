package iut.fspotify.fragments;

import static android.content.ContentValues.TAG;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
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
    private MediaPlayer mediaPlayer;
    private boolean showingLyrics = false;
    private int currentIndex = 0;
    private List<Song> songList;

    private ImageView cover;
    private TextView lyrics;
    private TextView title;
    private ImageButton play, next, prev, forward, rewind;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        cover = view.findViewById(R.id.cover_image);
        lyrics = view.findViewById(R.id.lyrics_text);
        title = view.findViewById(R.id.title_text);
        play = view.findViewById(R.id.play_button);
        next = view.findViewById(R.id.next_button);
        prev = view.findViewById(R.id.prev_button);
        forward = view.findViewById(R.id.forward_button);
        rewind = view.findViewById(R.id.rewind_button);

        lyrics.setVisibility(View.GONE);

        songList = CSVParser.parseCSV();
        Toast.makeText(getContext(), "Songs loaded: " + songList.size(), Toast.LENGTH_SHORT).show();
        if (!songList.isEmpty()) {
            loadSong(currentIndex);
        }

        cover.setOnClickListener(v -> {
            showingLyrics = !showingLyrics;
            lyrics.setVisibility(showingLyrics ? View.VISIBLE : View.GONE);
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

        return view;
    }

    private void loadSong(int index) {
        if (mediaPlayer != null) {
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
    }
}