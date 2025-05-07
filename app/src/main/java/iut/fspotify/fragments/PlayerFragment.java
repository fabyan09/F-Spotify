package iut.fspotify.fragments;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.squareup.picasso.Picasso;
import iut.fspotify.R;
import iut.fspotify.model.Song;

public class PlayerFragment extends Fragment {
    private MediaPlayer mediaPlayer;
    private boolean showingLyrics = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        ImageView cover = view.findViewById(R.id.cover_image);
        TextView lyrics = view.findViewById(R.id.lyrics_text);
        TextView title = view.findViewById(R.id.title_text);
        ImageButton play = view.findViewById(R.id.play_button);

        Song song = new Song("Ghost Town", "Ghost Town", "Isabel LaRosa", "2019",
                "85a160dd6a0c4707a2fbe4aa4593df64.jpg",
                "I met you when we both were just sixteen...",
                "Ghost-Town.mp3", 4.20f);

        Picasso.get()
                .load("http://edu.info06.net/lyrics/images/" + song.cover)
                .into(cover);


        title.setText(song.title);
        lyrics.setText(song.lyrics);
        lyrics.setVisibility(View.GONE);

        cover.setOnClickListener(v -> {
            showingLyrics = !showingLyrics;
            lyrics.setVisibility(showingLyrics ? View.VISIBLE : View.GONE);
        });

        play.setOnClickListener(v -> {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource("http://edu.info06.net/lyrics/mp3/" + song.mp3);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                else mediaPlayer.start();
            }
        });

        return view;
    }
}
