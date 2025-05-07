package iut.fspotify.utils;

import android.os.StrictMode;
import android.util.Log;
import java.io.*;
import java.net.*;
import java.util.*;
import iut.fspotify.model.Song;

public class CSVParser {
    public static List<Song> parseCSV() {
        List<Song> songList = new ArrayList<>();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        try {
            URL url = new URL("http://edu.info06.net/lyrics/lyrics.csv");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder csvContent = new StringBuilder();
            String line;

            // Lire tout le fichier en une seule string (permet de gérer les sauts de lignes dans les paroles)
            while ((line = reader.readLine()) != null) {
                csvContent.append(line).append("\n");
            }
            reader.close();

            // Séparer par lignes logiques : sauts de ligne après chaque #duration
            String full = csvContent.toString();
            String[] rawLines = full.split("(?<=#[0-9]+\\.[0-9]{2})\\n");

            for (String raw : rawLines) {
                try {
                    // Nettoyage : retirer les guillemets autour des paroles si présents
                    String[] parts = raw.trim().split("#", -1);
                    if (parts.length != 8) {
                        Log.w("CSVParser", "Ligne ignorée (tokens): " + raw);
                        continue;
                    }
                    String title = parts[0];
                    String album = parts[1];
                    String artist = parts[2];
                    String date = parts[3];
                    String cover = parts[4];
                    String content = parts[5].replace("\"", ""); // enlever les ""
                    String mp3 = parts[6];
                    float duration = Float.parseFloat(parts[7]);

                    songList.add(new Song(title, album, artist, date, cover, content, mp3, duration));
                } catch (Exception e) {
                    Log.e("CSVParser", "Erreur parsing ligne: " + raw, e);
                }
            }
        } catch (IOException e) {
            Log.e("CSVParser", "Erreur d'accès au CSV distant", e);
        }

        Log.d("CSVParser", "Chansons chargées: " + songList.size());
        return songList;
    }
}