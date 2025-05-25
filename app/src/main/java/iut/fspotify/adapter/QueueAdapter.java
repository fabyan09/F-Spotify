package iut.fspotify.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import iut.fspotify.R;
import iut.fspotify.model.Song;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private final List<Song> songList;
    private final OnItemClickListener listener;
    private final ItemTouchHelper touchHelper;
    private int currentPlayingPosition = -1;
    private Song currentPlayingSong = null; // Référence à la chanson en cours
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(Song song, int position);
    }

    public QueueAdapter(List<Song> songList, OnItemClickListener listener, ItemTouchHelper touchHelper) {
        this.songList = songList;
        this.listener = listener;
        this.touchHelper = touchHelper;
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue_song, parent, false);
        return new QueueViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.titleTextView.setText(song.getTitle());
        holder.artistTextView.setText(song.getArtist());

        // Chargement de l'image
        try {
            URL url = new URL("http://edu.info06.net/lyrics/images/" + song.getCover());
            InputStream input = url.openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            holder.coverImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            holder.coverImageView.setImageResource(R.drawable.placeholder);
        }

        // Utiliser une comparaison stricte basée sur une combinaison unique de propriétés
        boolean isCurrentSong = currentPlayingSong != null &&
                song.getTitle().equals(currentPlayingSong.getTitle()) &&
                song.getArtist().equals(currentPlayingSong.getArtist()) &&
                song.getMp3().equals(currentPlayingSong.getMp3());

        // Appliquer la sélection visuelle via l'état selected du conteneur
        holder.containerView.setSelected(isCurrentSong);

        // Mettre à jour la position actuelle si nécessaire
        if (isCurrentSong && currentPlayingPosition != position) {
            currentPlayingPosition = position;
        }

        // Gestion du clic
        holder.itemView.setOnClickListener(v -> listener.onItemClick(song, position));

        // Configuration du drag handle pour permettre le déplacement sur plusieurs positions
        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(holder);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    // Méthode pour mettre à jour la position du morceau en cours de lecture
    public void setCurrentPlayingPosition(int position) {
        // Stocker la référence à la chanson elle-même
        if (position >= 0 && position < songList.size()) {
            // Sauvegarder l'ancienne position pour la mise à jour
            int oldPosition = currentPlayingPosition;

            // Mettre à jour la position et la référence à la chanson
            currentPlayingPosition = position;
            currentPlayingSong = songList.get(position);

            // Mettre à jour uniquement les éléments concernés
            if (oldPosition != -1 && oldPosition < getItemCount()) {
                notifyItemChanged(oldPosition);
            }
            notifyItemChanged(currentPlayingPosition);
        } else {
            currentPlayingPosition = -1;
            currentPlayingSong = null;
        }
    }

    // Méthode pour obtenir la position du morceau en cours de lecture
    public int getCurrentPlayingPosition() {
        return currentPlayingPosition;
    }

    // Méthode pour déplacer un élément dans la liste
    // Simplifiée pour le drag & drop multi-positions
    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= songList.size() ||
                toPosition < 0 || toPosition >= songList.size()) {
            return;
        }

        // Si les positions sont identiques, ne rien faire
        if (fromPosition == toPosition) {
            return;
        }

        // Déplacer la chanson dans la liste
        Song movedSong = songList.remove(fromPosition);
        songList.add(toPosition, movedSong);

        // Notifier l'adaptateur du déplacement
        notifyItemMoved(fromPosition, toPosition);
    }

    // Méthode pour obtenir la liste des chansons
    public List<Song> getSongList() {
        return songList;
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView artistTextView;
        ImageView coverImageView;
        ImageView dragHandle;
        LinearLayout containerView;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.item_title);
            artistTextView = itemView.findViewById(R.id.item_artist);
            coverImageView = itemView.findViewById(R.id.item_cover);
            dragHandle = itemView.findViewById(R.id.drag_handle);
            containerView = (LinearLayout) itemView.findViewById(R.id.song_container);
        }
    }
}
