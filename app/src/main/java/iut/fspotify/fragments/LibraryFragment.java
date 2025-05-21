package iut.fspotify.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import iut.fspotify.R;
import iut.fspotify.model.Song;
import iut.fspotify.utils.CSVParser;

public class LibraryFragment extends Fragment {

    private List<Song> likedSongs = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.liked_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        SharedPreferences prefs = requireContext().getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        List<Song> allSongs = CSVParser.parseCSV();

        for (Song song : allSongs) {
            String key = song.title.trim().toLowerCase();
            if (prefs.getBoolean(key, false)) {
                likedSongs.add(song);
            }
        }


        recyclerView.setAdapter(new LikedSongAdapter(getContext(), likedSongs));
        return view;
    }

    private static class LikedSongAdapter extends RecyclerView.Adapter<LikedSongAdapter.ViewHolder> {
        private final List<Song> songs;

        public LikedSongAdapter(Context context, List<Song> songs) {
            this.songs = songs;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_liked_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Song song = songs.get(position);
            holder.title.setText(song.title);
            holder.artist.setText(song.artist);
            holder.duration.setText(String.format("%.2f min", song.duration));

            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
            try {
                URL url = new URL("http://edu.info06.net/lyrics/images/" + song.cover);
                InputStream input = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                holder.cover.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView cover;
            TextView title, artist, duration;

            ViewHolder(View itemView) {
                super(itemView);
                cover = itemView.findViewById(R.id.item_cover);
                title = itemView.findViewById(R.id.item_title);
                artist = itemView.findViewById(R.id.item_artist);
                duration = itemView.findViewById(R.id.item_duration);
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        refreshLikedSongs();
    }

    private void refreshLikedSongs() {
        SharedPreferences prefs = requireContext().getSharedPreferences("LIKED_SONGS", Context.MODE_PRIVATE);
        List<Song> allSongs = CSVParser.parseCSV();
        List<Song> likedSongs = new ArrayList<>();

        for (Song song : allSongs) {
            String key = song.title.trim().toLowerCase();
            if (prefs.getBoolean(key, false)) {
                likedSongs.add(song);
            }
        }

        RecyclerView recyclerView = requireView().findViewById(R.id.liked_recycler_view);
        recyclerView.setAdapter(new LikedSongAdapter(getContext(), likedSongs));
    }


}
