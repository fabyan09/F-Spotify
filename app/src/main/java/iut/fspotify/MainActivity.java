package iut.fspotify;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import iut.fspotify.fragments.*;

public class MainActivity extends AppCompatActivity {

    private PlayerFragment playerFragment = new PlayerFragment();
    private QueueFragment queueFragment = new QueueFragment();
    private LibraryFragment libraryFragment = new LibraryFragment();
    private Fragment activeFragment = playerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Ajouter tous les fragments une seule fois
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, libraryFragment, "LIBRARY").hide(libraryFragment)
                .add(R.id.fragment_container, queueFragment, "QUEUE").hide(queueFragment)
                .add(R.id.fragment_container, playerFragment, "PLAYER")
                .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            int itemId = item.getItemId();

            if (itemId == R.id.nav_player) {
                transaction.hide(activeFragment).show(playerFragment);
                activeFragment = playerFragment;
            } else if (itemId == R.id.nav_queue) {
                transaction.hide(activeFragment).show(queueFragment);
                activeFragment = queueFragment;
            } else if (itemId == R.id.nav_library) {
                transaction.hide(activeFragment).show(libraryFragment);
                activeFragment = libraryFragment;
            }


            transaction.commit();
            return true;
        });

        // Sélectionner le fragment par défaut
        bottomNav.setSelectedItemId(R.id.nav_player);
    }

    public PlayerFragment getPlayerFragment() {
        return playerFragment;
    }

}
