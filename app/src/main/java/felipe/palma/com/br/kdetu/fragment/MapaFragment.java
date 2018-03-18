package felipe.palma.com.br.kdetu.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import felipe.palma.com.br.kdetu.R;

/**
 * Created by Roberlandio on 24/11/2016.
 */
public class MapaFragment extends Fragment {
    protected Button btnEu;

    public static MapaFragment create(){

        return new MapaFragment();
    }
    /*
    OnCreateView criar o Mapa

     */
    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View view = layoutInflater.inflate(R.layout.mapa,viewGroup,false);
        btnEu = (Button) view.findViewById(R.id.btn_eu);

        return view;
    }
}
