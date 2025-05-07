package iut.fspotify.model;

public class Song {
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
}
