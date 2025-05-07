package iut.fspotify.fragments;

import android.os.Bundle;
import android.view.*;
import androidx.fragment.app.Fragment;
import iut.fspotify.R;

public class LibraryFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }
}

