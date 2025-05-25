package iut.fspotify.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import iut.fspotify.model.Song;

public class MusicPlayerService extends Service {
    private static final String TAG = "MusicPlayerService";
    
    // MediaPlayer pour la lecture audio
    private MediaPlayer mediaPlayer;
    
    // Chanson en cours de lecture
    private Song currentSong;
    
    // Position actuelle dans la chanson
    private int currentPosition = 0;
    
    // État de lecture
    private boolean isPlaying = false;
    
    // File d'attente de lecture
    private List<Song> queue = new ArrayList<>();
    private int currentQueueIndex = -1;
    
    // Binder pour les clients
    private final IBinder binder = new MusicBinder();
    
    // Liste des listeners
    private List<OnMusicPlayerListener> listeners = new ArrayList<>();
    
    // Thread pour les mises à jour de progression
    private Thread progressThread;
    private boolean isProgressThreadRunning = false;
    
    // Verrou pour éviter les appels multiples
    private boolean isLoadingSong = false;
    
    // Interface pour les callbacks
    public interface OnMusicPlayerListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onSongChanged(Song song);
        void onProgressChanged(int position, int duration);
        void onQueueChanged(List<Song> queue, int currentIndex);
    }
    
    public class MusicBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service créé");
        mediaPlayer = new MediaPlayer();
        
        // Configurer les listeners du MediaPlayer
        mediaPlayer.setOnPreparedListener(mp -> {
            mp.seekTo(currentPosition);
            if (isPlaying) {
                mp.start();
            }
            notifyPlaybackStateChanged();
            startProgressUpdates();
            isLoadingSong = false; // Libérer le verrou une fois la chanson chargée
        });
        
        mediaPlayer.setOnCompletionListener(mp -> {
            // Passer à la chanson suivante dans la queue
            if (currentQueueIndex < queue.size() - 1) {
                playNextInQueue();
            } else {
                isPlaying = false;
                notifyPlaybackStateChanged();
            }
        });
        
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Erreur MediaPlayer: " + what + ", " + extra);
            isLoadingSong = false; // Libérer le verrou en cas d'erreur
            return false;
        });
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service démarré");
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopProgressUpdates();
        super.onDestroy();
        Log.d(TAG, "Service détruit");
    }
    
    // Méthode pour charger une chanson
    public void loadSong(Song song) {
        if (song == null || isLoadingSong) return; // Éviter les appels multiples
        
        isLoadingSong = true; // Activer le verrou
        
        try {
            // Sauvegarder l'état actuel
            boolean wasPlaying = isPlaying;
            
            // Réinitialiser le MediaPlayer
            mediaPlayer.reset();
            
            // Définir la nouvelle source
            mediaPlayer.setDataSource("http://edu.info06.net/lyrics/mp3/" + song.getMp3());
            
            // Préparer de manière asynchrone
            mediaPlayer.prepareAsync();
            
            // Mettre à jour l'état
            currentSong = song;
            currentPosition = 0;
            isPlaying = wasPlaying;
            
            // Notifier les listeners
            notifySongChanged();
            
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors du chargement de la chanson", e);
            isLoadingSong = false; // Libérer le verrou en cas d'erreur
        }
    }
    
    // Méthode pour jouer ou mettre en pause
    public void playPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                currentPosition = mediaPlayer.getCurrentPosition();
                isPlaying = false;
            } else {
                mediaPlayer.start();
                isPlaying = true;
                startProgressUpdates();
            }
            notifyPlaybackStateChanged();
        }
    }
    
    // Méthode pour avancer de 10 secondes
    public void seekForward() {
        if (mediaPlayer != null) {
            int newPosition = mediaPlayer.getCurrentPosition() + 10000;
            mediaPlayer.seekTo(Math.min(newPosition, mediaPlayer.getDuration()));
            notifyProgressChanged();
        }
    }
    
    // Méthode pour reculer de 10 secondes
    public void seekBackward() {
        if (mediaPlayer != null) {
            int newPosition = mediaPlayer.getCurrentPosition() - 10000;
            mediaPlayer.seekTo(Math.max(newPosition, 0));
            notifyProgressChanged();
        }
    }
    
    // Méthode pour se déplacer à une position spécifique
    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
            notifyProgressChanged();
        }
    }
    
    // Méthode pour obtenir la position actuelle
    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }
    
    // Méthode pour obtenir la durée totale
    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }
    
    // Méthode pour obtenir la chanson actuelle
    public Song getCurrentSong() {
        return currentSong;
    }
    
    // Méthode pour vérifier si la lecture est en cours
    public boolean isPlaying() {
        return isPlaying;
    }
    
    // Méthodes pour la gestion de la file d'attente
    public void setQueue(List<Song> newQueue) {
        queue.clear();
        if (newQueue != null) {
            queue.addAll(newQueue);
        }
        notifyQueueChanged();
    }
    
    public List<Song> getQueue() {
        return new ArrayList<>(queue);
    }
    
    public int getCurrentQueueIndex() {
        return currentQueueIndex;
    }
    
    // Méthode pour mettre à jour l'index de la queue sans déclencher de lecture
    public void setCurrentQueueIndexSilently(int index) {
        if (index >= 0 && index < queue.size()) {
            currentQueueIndex = index;
            notifyQueueChanged();
        }
    }
    
    public void playQueueItem(int index) {
        if (index >= 0 && index < queue.size() && !isLoadingSong) {
            currentQueueIndex = index;
            loadSong(queue.get(index));
            isPlaying = true; // Définir l'état de lecture à true
            // Ne pas appeler playPause() ici pour éviter les appels multiples
            notifyQueueChanged();
        }
    }
    
    public void playNextInQueue() {
        if (currentQueueIndex < queue.size() - 1 && !isLoadingSong) {
            currentQueueIndex++;
            loadSong(queue.get(currentQueueIndex));
            isPlaying = true; // Définir l'état de lecture à true
            notifyQueueChanged();
        }
    }
    
    public void playPreviousInQueue() {
        if (currentQueueIndex > 0 && !isLoadingSong) {
            currentQueueIndex--;
            loadSong(queue.get(currentQueueIndex));
            isPlaying = true; // Définir l'état de lecture à true
            notifyQueueChanged();
        }
    }
    
    // Méthode pour ajouter un listener
    public void addListener(OnMusicPlayerListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            
            // Envoyer immédiatement l'état actuel au nouveau listener
            if (currentSong != null) {
                listener.onSongChanged(currentSong);
                listener.onPlaybackStateChanged(isPlaying);
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    listener.onProgressChanged(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
                }
            }
            listener.onQueueChanged(queue, currentQueueIndex);
        }
    }
    
    // Méthode pour supprimer un listener
    public void removeListener(OnMusicPlayerListener listener) {
        listeners.remove(listener);
    }
    
    // Méthode pour notifier les changements d'état de lecture
    private void notifyPlaybackStateChanged() {
        for (OnMusicPlayerListener listener : listeners) {
            listener.onPlaybackStateChanged(isPlaying);
        }
    }
    
    // Méthode pour notifier les changements de chanson
    private void notifySongChanged() {
        for (OnMusicPlayerListener listener : listeners) {
            listener.onSongChanged(currentSong);
        }
    }
    
    // Méthode pour notifier les changements de progression
    private void notifyProgressChanged() {
        if (mediaPlayer != null) {
            for (OnMusicPlayerListener listener : listeners) {
                listener.onProgressChanged(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
            }
        }
    }
    
    // Méthode pour notifier les changements de file d'attente
    private void notifyQueueChanged() {
        for (OnMusicPlayerListener listener : listeners) {
            listener.onQueueChanged(queue, currentQueueIndex);
        }
    }
    
    // Méthode pour démarrer les mises à jour de progression
    private void startProgressUpdates() {
        // Arrêter le thread existant s'il est en cours d'exécution
        stopProgressUpdates();
        
        // Créer et démarrer un nouveau thread
        isProgressThreadRunning = true;
        progressThread = new Thread(() -> {
            while (isProgressThreadRunning && mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    notifyProgressChanged();
                }
                try {
                    Thread.sleep(500); // Mise à jour plus fréquente pour une meilleure réactivité
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        progressThread.start();
    }
    
    // Méthode pour arrêter les mises à jour de progression
    private void stopProgressUpdates() {
        isProgressThreadRunning = false;
        if (progressThread != null) {
            progressThread.interrupt();
            progressThread = null;
        }
    }
}
