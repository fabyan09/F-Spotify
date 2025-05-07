package iut.fspotify.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.StrictMode;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import iut.fspotify.R;
import iut.fspotify.model.Song;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private final List<Song> songs;
    private final SongAdapter.OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Song song);
    }

    public SongAdapter(List<Song> songs, OnItemClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song, listener);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        ImageView cover;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_title);
            artist = itemView.findViewById(R.id.item_artist);
            cover = itemView.findViewById(R.id.item_cover);
        }

        public void bind(final Song song, final OnItemClickListener listener) {
            title.setText(song.title);
            artist.setText(song.artist);

            // Chargement simple de l'image depuis l'URL sans bibliothèque externe
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            try {
                URL url = new URL("http://edu.info06.net/lyrics/images/" + song.cover);
                InputStream input = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                cover.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

            itemView.setOnClickListener(v -> listener.onItemClick(song));
        }
    }
}
