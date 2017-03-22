package felipe.palma.com.br.kdetu;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import felipe.palma.com.br.kdetu.model.LocalTrack;
import felipe.palma.com.br.kdetu.utils.Constant;


public class MainActivity extends AppCompatActivity implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.InfoWindowAdapter,
        GoogleApiClient.OnConnectionFailedListener {

    //Constantes
    private final static long INTERVALO = 1000 * 5;
    private final static long DISTANCIA_INTERVALO = 1000 * 5;
    private final static int PERMISSAO = 123;
    private final FirebaseDatabase rootRef = FirebaseDatabase.getInstance();
    private final DatabaseReference mDatabase = rootRef.getReference(Constant.FIREBASE_ROOT);

    private String android_id;

    private ValueEventListener valueEventListenerUserConnected;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mGoogleMap;
    private Location mCurrentLocation;
    private LatLng latLng;
    private List<LocalTrack> locais = new ArrayList<>();
    private LocalTrack localTrack;

    private String lastUpdateTime;
    private Button btnTrack;
    private String descricao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapa);

        if (!isGoogleServiceAvaliable()) {
            finish();
        }
        createLocationRequest();

        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);

        btnTrack = (Button) findViewById(R.id.btn_eu);

        /*
            ID android
         */
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mapa, fragment)
                .commit();
        fragment.getMapAsync(this); //OK para o map

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    private void mostrarDialogo() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Informe a Descrição");
        final EditText input = new EditText(this);
        final Spinner spnLinhas = new Spinner(this);

        List<String> linhas = new ArrayList<>();
        linhas.add("414");
        linhas.add("416");
        linhas.add("046");
        linhas.add("054");

        ArrayAdapter adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,linhas);
        spnLinhas.setAdapter(adapter);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        alertBuilder.setView(input);
        alertBuilder.setView(spnLinhas);


        alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override

            public void onClick(DialogInterface dialog, int which) {
                /*
                Criando refencia no Firebase
                 */

                descricao = spnLinhas.getSelectedItem().toString();//input.getText().toString();
                lastUpdateTime = getLastTimeUpdate();
                Toast.makeText(getApplication(),"Selecionado: " + spnLinhas.getSelectedItem().toString(),Toast.LENGTH_LONG).show();

                String key = android_id;
                localTrack = new LocalTrack();
                localTrack.setId(key);
                localTrack.setDescricao(descricao);
                localTrack.setLatitude(latLng.latitude);
                localTrack.setLongitude(latLng.longitude);
                localTrack.setAtualizacao(lastUpdateTime);
                //novoLocal(localTrack);

                atualizarMeuLocal(localTrack);
                moverCamera(mGoogleMap, latLng);


            }
        });
        alertBuilder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertBuilder.show();
    }

    private String getLastTimeUpdate() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy\nHH:mm:ss");
        return format.format(c.getTime());
    }

    /*
    Adiciona nova posição no Firebase e Atualiza
     */

    private void atualizarMeuLocal(final LocalTrack local) {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(android_id)) {
                    if (dataSnapshot.child(android_id).hasChild(Constant.CHILD_DESCRICAO)) {
                        mDatabase.child(android_id).setValue(local);

                    } else {
                        mDatabase.child(android_id).setValue(local);
                    }

                } else {
                    novoLocal(local);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("SERVICO", "INICIADO");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("SERVICO", "DESLIGADO");
        //mDatabase.removeEventListener(valueEventListenerUserConnected);
        mDatabase.removeEventListener(getValueEventListenerLocalTracks);
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //updateUI();
        btnTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogo();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d("APP", "APP PAUSADO: " + android_id);
    }

    @Override
    public void finish() {
        super.finish();

        Log.d("APP", "APP FINALIZADO: " + android_id);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("APP", "Servico funcionando");
        //mGoogleMap.clear();
        mCurrentLocation = location;
        lastUpdateTime = getLastTimeUpdate();

        latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        /*
        Minha posição
        */
        if (localTrack != null) {
            localTrack.setLatitude(mCurrentLocation.getLatitude());
            localTrack.setLongitude(mCurrentLocation.getLongitude());
            localTrack.setAtualizacao(lastUpdateTime);
        }

        updateUI();


    }

    private void updateUI() {

        if (null != mCurrentLocation) {
            latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

            /*
            Limpar Array de Locais
             */
            /*
            Atualização constante
             */
            atualizarMeuLocal(localTrack);
            getLocalsFromFirebase();
            mGoogleMap.clear();
        } else {
            Log.d("SERVICO LOCATION", "location is null ...............");
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSAO: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                for (int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], grantResults[i]);
                }
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "Deu Erro", Toast.LENGTH_LONG).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {
        final List<String> permissionsList = new ArrayList<>();
        final List<String> permissionsNeeded = new ArrayList<>();

        if (!addPermisson(permissionsList, android.Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("GPS");
        if (!addPermisson(permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION))
            permissionsNeeded.add("GPS");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                String message = "Permitir acesso à: " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++) {
                    message = message + " , " + permissionsNeeded.get(i);
                }
                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                        PERMISSAO);
                            }
                        });
                return;
            }
        }
    }

    protected void startLocationUpdates() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions();

        }



        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSAO);
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSAO);
            return;
        }
        PendingResult<Status> pendingResults = LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);





    }

    private void showMessageOKCancel(String message,DialogInterface.OnClickListener okListener){
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK",okListener)
                .setNegativeButton("Cancelar",null)
                .create()
                .show();
    }

    public boolean addPermisson(List<String> permissionsList,String permission){
        if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
            permissionsList.add(permission);
            if(!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        PendingResult<Status> pendingResults = LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);


        return true;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("CONEXAO", "Connection failed: " + connectionResult.toString());

    }

    protected void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVALO);
        mLocationRequest.setFastestInterval(DISTANCIA_INTERVALO);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        // **************************

        builder.setAlwaysShow(true); // this is the key ingredient

        // *

    }

    private boolean isGoogleServiceAvaliable(){
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(ConnectionResult.SUCCESS == status){
            return true;
        }else{
            GooglePlayServicesUtil.getErrorDialog(status,this,0).show();
            return false;
        }
    }



    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.setInfoWindowAdapter(this);

        latLng = new LatLng(-3.0446598,-60.0371444);
        //adicionarMarcador(mGoogleMap, latLng);
        moverCamera(mGoogleMap,latLng);

        mGoogleMap.clear();
        getLocalsFromFirebase();

    }

    /*
    Recupera dados do Firebase
     */
    private ValueEventListener getValueEventListenerLocalTracks = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            mGoogleMap.clear();
            for(DataSnapshot data: dataSnapshot.getChildren()){
                LocalTrack localTrack = data.getValue(LocalTrack.class);
                adicionarMarcador(mGoogleMap,localTrack,new LatLng(localTrack.getLatitude(),localTrack.getLongitude()));
                locais.add(localTrack);

            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private void getLocalsFromFirebase() {
        mDatabase.addValueEventListener(getValueEventListenerLocalTracks);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    private void novoLocal(final LocalTrack localTrack){
        DatabaseReference mDatabase = rootRef.getReference().child("local");
        mDatabase.child(android_id).setValue(localTrack);


    }


    private void moverCamera(final GoogleMap googleMap, LatLng latLng) {
        CameraPosition cameraPosition = CameraPosition.builder()
                .target(latLng)
                .zoom(12)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void adicionarMarcador(GoogleMap googleMap,LocalTrack localTrack, LatLng latLng) {
        String texto = localTrack.getAtualizacao();
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(localTrack.getDescricao())
                .snippet(texto)
                //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))//Alterar a cor do marcador padrao
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bus))// Custom imagem

                //.draggable(true)//permite mover o marcador
                ;
        googleMap.addMarker(markerOptions);
        //Log.i("MARCADOR","ADICIONADO");



    }


    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        //return null;
        //return prepareInfoView(marker);

        View v = getLayoutInflater().inflate(R.layout.infor_bus,null);
        TextView txtAtualizacao = (TextView)v.findViewById(R.id.txt_atualizacao);

        TextView txtTitulo = (TextView)v.findViewById(R.id.txt_titulo);

        txtAtualizacao.setText(marker.getSnippet());
        txtTitulo.setText(marker.getTitle());

        return v;


    }

    private View prepareInfoView(Marker marker){
        //prepare InfoView programmatically
        LinearLayout infoView = new LinearLayout(MainActivity.this);
        LinearLayout.LayoutParams infoViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        infoView.setOrientation(LinearLayout.HORIZONTAL);
        infoView.setLayoutParams(infoViewParams);

        ImageView infoImageView = new ImageView(MainActivity.this);
        //Drawable drawable = getResources().getDrawable(R.mipmap.ic_launcher);
        Drawable drawable = getResources().getDrawable(android.R.drawable.ic_dialog_map);
        infoImageView.setImageDrawable(drawable);

        infoView.addView(infoImageView);

        LinearLayout subInfoView = new LinearLayout(MainActivity.this);
        LinearLayout.LayoutParams subInfoViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subInfoView.setOrientation(LinearLayout.VERTICAL);
        subInfoView.setLayoutParams(subInfoViewParams);



        TextView subInfoTitle = new TextView(MainActivity.this);
        subInfoTitle.setText(marker.getTitle());
        TextView subInfoAtualizacao = new TextView(MainActivity.this);
        subInfoTitle.setText(marker.getSnippet().toString());
        TextView subInfoLat = new TextView(MainActivity.this);
        subInfoLat.setText("Lat: " + marker.getPosition().latitude);
        TextView subInfoLnt = new TextView(MainActivity.this);
        subInfoLnt.setText("Lnt: " + marker.getPosition().longitude);

        subInfoView.addView(subInfoTitle);
        subInfoView.addView(subInfoAtualizacao);
        subInfoView.addView(subInfoLat);
        subInfoView.addView(subInfoLnt);
        infoView.addView(subInfoView);

        return infoView;
    }
}
