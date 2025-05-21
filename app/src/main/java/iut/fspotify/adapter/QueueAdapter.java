package iut.fspotify.adapter;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import iut.fspotify.R;
import iut.fspotify.model.Song;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private final List<Song> songList;

    public QueueAdapter(List<Song> songList) {
        this.songList = songList;
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.titleTextView.setText(song.getTitle());
        holder.artistTextView.setText(song.getArtist());

        // Chargement de l'image depuis l'URL
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            URL url = new URL("http://edu.info06.net/lyrics/images/" + song.getCover());
            InputStream input = url.openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            holder.coverImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            holder.coverImageView.setImageResource(R.drawable.placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView artistTextView;
        ImageView coverImageView;



        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.item_title);
            artistTextView = itemView.findViewById(R.id.item_artist);
            coverImageView = itemView.findViewById(R.id.item_cover);
        }
    }
}