package iut.fspotify;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import iut.fspotify.activities.PlayerActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Rediriger directement vers PlayerActivity
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
        
        // Terminer MainActivity pour qu'elle ne reste pas dans la pile d'activités
        finish();
    }
}
