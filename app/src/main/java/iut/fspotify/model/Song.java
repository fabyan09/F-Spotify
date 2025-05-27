package iut.fspotify.model;

import java.io.Serializable;

public class Song implements Serializable {
    public String title, album, artist, date, cover, lyrics, mp3;
    public float duration;
    public float rating = 0; // Note de 0 à 5 étoiles, 0 par défaut (non noté)

    public Song(String title, String album, String artist, String date,
                String cover, String lyrics, String mp3, float duration) {
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.date = date;
        this.cover = cover;
        this.lyrics = lyrics;
        this.mp3 = mp3;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }
    public String getLyrics() {
        return lyrics;
    }
    public String getMp3() {
        return mp3;
    }
    public String getCover() {
        return cover;
    }
    public String getAlbum() {
        return album;
    }
    public String getArtist() {
        return artist;
    }
    public String getDate() {
        return date;
    }
    public float getDuration() {
        return duration;
    }
    
    // Méthodes pour la gestion des notes
    public float getRating() {
        return rating;
    }
    
    public void setRating(float rating) {
        if (rating >= 0 && rating <= 5) {
            this.rating = rating;
        }
    }
}
