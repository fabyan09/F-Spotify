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
            String line;
            boolean skipFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (skipFirstLine) {
                    skipFirstLine = false;
                    continue;
                }

                // Gestion des guillemets et des points-virgules dans les paroles
                String[] tokens = line.split("#", -1);
                if (tokens.length == 8) {
                    String title = tokens[0];
                    String album = tokens[1];
                    String artist = tokens[2];
                    String date = tokens[3];
                    String cover = tokens[4];
                    String content = tokens[5].replace("\"", ""); // Enlève les guillemets
                    String mp3 = tokens[6];
                    float duration = Float.parseFloat(tokens[7]);

                    songList.add(new Song(title, album, artist, date, cover, content, mp3, duration));
                } else {
                    Log.w("CSVParser", "Ligne ignorée (mauvais format) : " + line);
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e("CSVParser", "Erreur pendant le parsing CSV", e);
        }

        Log.d("CSVParser", "Nombre de chansons chargées : " + songList.size());
        return songList;
    }

}