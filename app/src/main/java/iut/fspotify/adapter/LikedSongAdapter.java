package iut.fspotify.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import iut.fspotify.R;
import iut.fspotify.model.Song;

public class LikedSongAdapter extends RecyclerView.Adapter<LikedSongAdapter.ViewHolder> {

    private final List<Song> likedSongs;
    private final Context context;
    private OnItemClickListener listener;

    public LikedSongAdapter(Context context, List<Song> likedSongs) {
        this.context = context;
        this.likedSongs = likedSongs;
    }

    @NonNull
    @Override
    public LikedSongAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_liked_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LikedSongAdapter.ViewHolder holder, int position) {
        Song song = likedSongs.get(position);
        holder.title.setText(song.title);
        holder.artist.setText(song.artist);

        // Afficher la durée uniquement si elle est disponible
        if (song.duration > 0) {
            holder.duration.setVisibility(View.VISIBLE);
            holder.duration.setText(String.format("%.2f min", song.duration));
        } else {
            holder.duration.setVisibility(View.GONE);
        }

        // Charger la pochette uniquement si elle est disponible
        if (song.cover != null && !song.cover.isEmpty()) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
            try {
                URL url = new URL("http://edu.info06.net/lyrics/images/" + song.cover);
                InputStream input = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                holder.cover.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                holder.cover.setImageResource(R.drawable.placeholder); // Image par défaut
            }
        } else {
            holder.cover.setImageResource(R.drawable.placeholder); // Image par défaut
        }

        // Notifier le listener lors d'un clic
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(song);
            }
        });
    }

    @Override
    public int getItemCount() {
        return likedSongs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, artist, duration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.item_cover);
            title = itemView.findViewById(R.id.item_title);
            artist = itemView.findViewById(R.id.item_artist);
            duration = itemView.findViewById(R.id.item_duration);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Song song);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}