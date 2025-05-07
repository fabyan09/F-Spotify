package iut.fspotify;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import iut.fspotify.fragments.*;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_player) {
                selectedFragment = new PlayerFragment();
            } else if (id == R.id.nav_queue) {
                selectedFragment = new QueueFragment();
            } else if (id == R.id.nav_library) {
                selectedFragment = new LibraryFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });


        // Load default fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new PlayerFragment()).commit();
    }
}
