package iut.fspotify.adapter;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.List;
import iut.fspotify.R;
import iut.fspotify.model.Song;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private final List<Song> songs;
    private final Context context;

    public interface OnItemClickListener {
        void onItemClick(Song song);
    }

    private final OnItemClickListener listener;

    public SongAdapter(Context context, List<Song> songs, OnItemClickListener listener) {
        this.context = context;
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
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
            Picasso.get()
                    .load("http://edu.info06.net/lyrics/images/" + song.cover)
                    .into(cover);

            itemView.setOnClickListener(v -> listener.onItemClick(song));
        }
    }
}