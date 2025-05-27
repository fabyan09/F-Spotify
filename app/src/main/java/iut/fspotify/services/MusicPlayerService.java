package iut.fspotify.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import iut.fspotify.R;
import iut.fspotify.activities.PlayerActivity;
import iut.fspotify.model.Song;

public class MusicPlayerService extends Service {
    private static final String TAG = "MusicPlayerService";

    // Constantes pour la notification
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "music_player_channel";

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

    // Durée calculée à partir du CSV (en millisecondes)
    private int calculatedDuration = 0;

    // Durée réelle du fichier audio (en millisecondes)
    private int actualDuration = 0;

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

        // Créer le canal de notification
        createNotificationChannel();

        // Configurer les listeners du MediaPlayer
        mediaPlayer.setOnPreparedListener(mp -> {
            mp.seekTo(currentPosition);
            if (isPlaying) {
                mp.start();
            }

            // Stocker la durée réelle du fichier audio
            actualDuration = mp.getDuration();

            // Utiliser la durée calculée à partir du CSV pour l'affichage
            // mais garder la durée réelle pour la seekbar

            // Log pour déboguer les problèmes de durée
            Log.d(TAG, "onPrepared: calculatedDuration=" + calculatedDuration +
                    ", actualDuration=" + actualDuration);

            notifyPlaybackStateChanged();
            notifyProgressChanged(currentPosition, calculatedDuration);
            startProgressUpdates();
            isLoadingSong = false; // Libérer le verrou une fois la chanson chargée

            // Mettre à jour la notification
            updateNotification();
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            // Passer à la chanson suivante dans la queue
            if (currentQueueIndex < queue.size() - 1) {
                playNextInQueue();
            } else {
                isPlaying = false;
                notifyPlaybackStateChanged();

                // Mettre à jour la notification
                updateNotification();
            }
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Erreur MediaPlayer: " + what + ", " + extra);
            isLoadingSong = false; // Libérer le verrou en cas d'erreur
            return false;
        });
    }

    // Méthode pour créer le canal de notification (obligatoire pour Android 8.0+)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Lecteur de musique",
                    NotificationManager.IMPORTANCE_LOW); // LOW pour éviter le son de notification
            channel.setDescription("Canal pour les notifications du lecteur de musique");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Méthode pour mettre à jour la notification
    private void updateNotification() {
        if (currentSong == null) return;

        // Créer l'intent pour ouvrir l'application quand on clique sur la notification
        Intent intent = new Intent(this, PlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Construire la notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(currentSong.getTitle())
                .setContentText(currentSong.getArtist())
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isPlaying); // La notification reste tant que la musique joue

        // Ajouter des boutons de contrôle (optionnel)
        // Intent pour play/pause
        Intent playPauseIntent = new Intent(this, MusicPlayerService.class);
        playPauseIntent.setAction("PLAY_PAUSE");
        PendingIntent playPausePendingIntent = PendingIntent.getService(
                this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE);

        // Intent pour next
        Intent nextIntent = new Intent(this, MusicPlayerService.class);
        nextIntent.setAction("NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(
                this, 1, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        // Intent pour previous
        Intent prevIntent = new Intent(this, MusicPlayerService.class);
        prevIntent.setAction("PREVIOUS");
        PendingIntent prevPendingIntent = PendingIntent.getService(
                this, 2, prevIntent, PendingIntent.FLAG_IMMUTABLE);

        // Ajouter les boutons à la notification
        builder.addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent);
        builder.addAction(isPlaying ?
                        android.R.drawable.ic_media_pause :
                        android.R.drawable.ic_media_play,
                isPlaying ? "Pause" : "Play",
                playPausePendingIntent);
        builder.addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent);

        // Démarrer en tant que service au premier plan
        startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service démarré");

        // Gérer les actions des boutons de notification
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "PLAY_PAUSE":
                    playPause();
                    break;
                case "NEXT":
                    playNextInQueue();
                    break;
                case "PREVIOUS":
                    playPreviousInQueue();
                    break;
            }
        }

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

            // Calculer la durée à partir du CSV (en minutes) et la convertir en millisecondes
            calculatedDuration = (int) (song.getDuration() * 60000);
            Log.d(TAG, "loadSong: " + song.getTitle() + ", CSV duration=" + song.getDuration() +
                    ", calculatedDuration=" + calculatedDuration + "ms");

            // Préparer de manière asynchrone
            mediaPlayer.prepareAsync();

            // Mettre à jour l'état
            currentSong = song;
            currentPosition = 0;
            isPlaying = wasPlaying;

            // Notifier les listeners
            notifySongChanged();

            // Mettre à jour la notification
            updateNotification();

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

            // Mettre à jour la notification
            updateNotification();
        }
    }

    // Méthode pour avancer de 10 secondes
    public void seekForward() {
        if (mediaPlayer != null) {
            int newPosition = mediaPlayer.getCurrentPosition() + 10000;
            // Utiliser la durée réelle du fichier pour la seekbar
            mediaPlayer.seekTo(Math.min(newPosition, actualDuration));
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
            // Convertir la position relative à la durée calculée en position relative à la durée réelle
            if (calculatedDuration > 0 && actualDuration > 0) {
                // Calculer le ratio de la position par rapport à la durée calculée
                float ratio = (float) position / calculatedDuration;
                // Appliquer ce ratio à la durée réelle
                int actualPosition = (int) (ratio * actualDuration);

                Log.d(TAG, "seekTo: position=" + position + ", calculatedDuration=" + calculatedDuration +
                        ", ratio=" + ratio + ", actualPosition=" + actualPosition +
                        ", actualDuration=" + actualDuration);

                // Utiliser la position convertie
                mediaPlayer.seekTo(Math.min(actualPosition, actualDuration));
            } else {
                // Fallback si les durées ne sont pas disponibles
                mediaPlayer.seekTo(position);
            }
            notifyProgressChanged();
        }
    }

    // Méthode pour obtenir la position actuelle
    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            // Convertir la position réelle en position relative à la durée calculée
            if (calculatedDuration > 0 && actualDuration > 0) {
                float ratio = (float) mediaPlayer.getCurrentPosition() / actualDuration;
                return (int) (ratio * calculatedDuration);
            }
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    // Méthode pour obtenir la durée totale
    public int getDuration() {
        // Toujours retourner la durée calculée pour l'affichage
        if (calculatedDuration > 0) {
            return calculatedDuration;
        } else if (mediaPlayer != null) {
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

                // Utiliser la durée calculée pour l'affichage
                int duration = calculatedDuration > 0 ? calculatedDuration :
                        (mediaPlayer != null ? mediaPlayer.getDuration() : 0);

                // Convertir la position réelle en position relative à la durée calculée
                int position = getCurrentPosition();

                listener.onProgressChanged(position, duration);
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
            // Convertir la position réelle en position relative à la durée calculée
            int position = getCurrentPosition();

            // Utiliser la durée calculée pour l'affichage
            int duration = calculatedDuration > 0 ? calculatedDuration : actualDuration;

            for (OnMusicPlayerListener listener : listeners) {
                listener.onProgressChanged(position, duration);
            }
        }
    }

    // Méthode pour notifier les changements de progression avec des valeurs spécifiques
    private void notifyProgressChanged(int position, int duration) {
        for (OnMusicPlayerListener listener : listeners) {
            listener.onProgressChanged(position, duration);
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
