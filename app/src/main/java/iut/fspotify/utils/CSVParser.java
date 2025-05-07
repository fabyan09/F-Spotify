package iut.fspotify.utils;

import android.content.Context;
import java.io.*;
import java.util.*;
import iut.fspotify.model.Song;

public class CSVParser {
    public static List<Song> parseCSV(Context context) {
        List<Song> songList = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("BDD/lyrics.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("#");
                if (tokens.length == 8) {
                    songList.add(new Song(
                            tokens[0], tokens[1], tokens[2], tokens[3],
                            tokens[4], tokens[5], tokens[6], Float.parseFloat(tokens[7])
                    ));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return songList;
    }
}
