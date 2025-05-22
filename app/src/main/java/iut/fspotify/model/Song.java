package iut.fspotify.model;

import java.io.Serializable;

public class Song implements Serializable {
    public String title, album, artist, date, cover, lyrics, mp3;
    public float duration;

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
}