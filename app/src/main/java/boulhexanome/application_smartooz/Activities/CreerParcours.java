package boulhexanome.application_smartooz.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.ClusterRenderer;

import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import boulhexanome.application_smartooz.Model.Circuit;
import boulhexanome.application_smartooz.Model.Place;
import boulhexanome.application_smartooz.Model.User;
import boulhexanome.application_smartooz.R;
import boulhexanome.application_smartooz.Utils.Config;
import boulhexanome.application_smartooz.Utils.MyClusterRenderer;
import boulhexanome.application_smartooz.Utils.Tools;
import boulhexanome.application_smartooz.WebServices.GetTask;
import boulhexanome.application_smartooz.WebServices.PostTask;

public class CreerParcours extends AppCompatActivity implements OnMapReadyCallback {

    private static final int ASK_FOR_ACCESS_COARSE_LOCATION = 1;
    private static final int ASK_FOR_ACCESS_FINE_LOCATION = 2;
    private GoogleMap mMap;
    private ActionMode mActionModeAjout;
    private ActionMode mActionModeRecherche;
    private ClusterManager mClusterManager;

    final LatLngBounds GRAND_LYON = new LatLngBounds(new LatLng(45.720301, 4.779128), new LatLng(45.797678, 4.926584));

    Polyline currentLine;

    ArrayList<Marker> markers;

    boolean boucle;
    boolean modeAjout;
    boolean modeRechercher;

    Circuit parcours;
    ArrayList<Place> places = new ArrayList<>();
    ArrayList<MyCluster> clusterItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creer_parcours);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar toolbar = getSupportActionBar();
        EditText barre_recherche = (EditText) findViewById(R.id.barre_recherche);
        barre_recherche.setVisibility(View.INVISIBLE);
        toolbar.setDisplayShowTitleEnabled(true);
        toolbar.setTitle("Créer Parcours");
        toolbar.setDisplayHomeAsUpEnabled(true);

        currentLine = null;
        modeRechercher = false;
        boucle = false;
        markers = new ArrayList<Marker>();

        parcours = new Circuit();
        final FloatingActionButton ajouterPI = (FloatingActionButton) findViewById(R.id.action_ajouterPI);
        ajouterPI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the CAB using the ActionMode.Callback defined above
                if (modeAjout == false) {
                    ajouterPI.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccentPlus)));
                    modeAjout = true;
                    Toast.makeText(CreerParcours.this, "Sélectionnez les étapes du parcours...", Toast.LENGTH_SHORT).show();
                } else {
                    ajouterPI.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
                    modeAjout = false;
                }
            }
        });

        final FloatingActionButton save = (FloatingActionButton) findViewById(R.id.action_save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < markers.size(); i++){
                    for (int j = 0; j < places.size(); j++){
                        if (places.get(j).getPosition().equals(markers.get(i).getPosition())){
                            User.getInstance().getCircuit_en_creation().addPlace(places.get(j));
                            j = places.size();
                        }
                    }
                }
                Intent intent = new Intent(CreerParcours.this, ChoixDuThemeActivity.class);
                startActivity(intent);
            }
        });

        final FloatingActionButton boucle = (FloatingActionButton) findViewById(R.id.action_boucle);
        boucle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CreerParcours.this.markers.size() >= 2) {
                    if (CreerParcours.this.boucle) {
                        markers.remove(markers.size() - 1);
                        CreerParcours.this.boucle = false;
                        boucle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
                        Toast.makeText(CreerParcours.this, "Le circuit ne boucle plus", Toast.LENGTH_SHORT).show();
                        showPolyline();
                    } else {
                        markers.add(markers.get(0));
                        CreerParcours.this.boucle = true;
                        boucle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccentPlus)));
                        Toast.makeText(CreerParcours.this, "Le circuit boucle", Toast.LENGTH_SHORT).show();
                        showPolyline();
                    }
                }
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.moveCamera(CameraUpdateFactory
                .newLatLngBounds(GRAND_LYON,10));

        GetTask getTask = new GetTask(Config.getRequest(Config.GET_PLACES));
        getTask.delegate = new HandleGetPlaces(this);
        getTask.execute();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    ASK_FOR_ACCESS_COARSE_LOCATION);
        }
        if (
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ASK_FOR_ACCESS_FINE_LOCATION);
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        try{
            mMap.setMyLocationEnabled(true);
            // Setting a custom info window adapter for the google map
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                // Use default InfoWindow frame
                @Override
                public View getInfoWindow(Marker arg0) {
                    return null;
                }

                // Defines the contents of the InfoWindow
                @Override
                public View getInfoContents(final Marker arg0) {

                    // Getting view from the layout file info_window_layout
                    View v = getLayoutInflater().inflate(R.layout.custom_info_contents, null);
                    v.setFocusableInTouchMode(true);

                    LatLng position = arg0.getPosition();
                    Place placeMarked = null;
                    for (int i = 0; i < places.size(); i++){
                        if (places.get(i).getPosition().equals(position)){
                            placeMarked = places.get(i);
                            i = places.size();
                        }
                    }

                    if (placeMarked != null) {

                        TextView numero = (TextView) v.findViewById(R.id.numero_place);
                        TextView title = (TextView) v.findViewById(R.id.title_place);
                        TextView description = (TextView) v.findViewById(R.id.description);
                        TextView noteOn5 = (TextView) v.findViewById(R.id.noteon5);
                        TextView tags = (TextView) v.findViewById(R.id.tags_infowindow);

                        String numeroListe ="";
                        for (int i = 0; i < markers.size(); i++) {
                            if (placeMarked.getPosition().equals(markers.get(i).getPosition())){
                                numeroListe = numeroListe + String.valueOf(i+1) + "&";
                            }
                        }

                        if (numeroListe.endsWith("&")){
                            numero.setHeight(75);
                            numeroListe = numeroListe.substring(0,numeroListe.length()-1);
                            numero.setText("Etape " + numeroListe);
                        } else {
                            numero.setHeight(0);
                            numero.setText("");
                        }
                        title.setText(placeMarked.getName());
                        description.setText(placeMarked.getDescription());
                        noteOn5.setText("Note : " + String.valueOf(placeMarked.getNoteOn5()) + " / 5");


                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Tags : ");
                        for (int i = 0; i < placeMarked.getKeywords().size(); i++) {
                            stringBuilder.append(placeMarked.getKeywords().get(i) + " ");
                        }
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                        tags.setText(stringBuilder.toString());

                        final Place finalPlaceMarked = placeMarked;
                    }

                    // Returning the view containing InfoWindow contents
                    return v;
                }
            });

            mClusterManager = new ClusterManager(this, mMap);
            mMap.setOnCameraChangeListener(mClusterManager);
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    if (!modeAjout){
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                        marker.showInfoWindow();
                        return true;
                    } else {
                        if (markers.contains(marker)){
                            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                            markers.remove(marker);
                            //Affichage dynamique du parcours
                            showPolyline();
                            return true;
                        } else {
                            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                            markers.add(marker);
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                            marker.showInfoWindow();
                            //Affichage dynamique du parcours
                            if (markers.size() >= 2) {
                                showPolyline();
                            }
                            return true;
                        }
                    }
                }
            });

        }catch (SecurityException e){
            System.out.println(e);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == android.R.id.home){
            if(modeRechercher){
                modeRechercher = false;
                ActionBar toolbar = getSupportActionBar();
                EditText barre_recherche = (EditText) findViewById(R.id.barre_recherche);
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
                barre_recherche.setVisibility(View.INVISIBLE);
                toolbar.setDisplayShowTitleEnabled(true);
            } else {
                finish();
            }
        }

        if (id ==R.id.action_rechercher){
            if(modeRechercher){
                EditText barre_recherche = (EditText) findViewById(R.id.barre_recherche);
                String keyword = barre_recherche.getText().toString();
                filter(keyword);

            } else {
                modeRechercher = true;
                ActionBar toolbar = getSupportActionBar();
                EditText barre_recherche = (EditText) findViewById(R.id.barre_recherche);
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
                barre_recherche.setVisibility(View.VISIBLE);
                toolbar.setDisplayShowTitleEnabled(false);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_creer_parcours, menu);
        return true;
    }

    public void filter(String keyword){
        mMap.clear();
        mClusterManager.clearItems();
        keyword = keyword.toUpperCase();
        if (keyword == "") {
            for (int i = 0; i < places.size(); i++){
                mClusterManager.addItem(new MyCluster(places.get(i).getPosition().latitude,places.get(i).getPosition().longitude));
            }
        }
        for (int i = 0; i < places.size(); i++){
            ArrayList<String> keywords = places.get(i).getKeywords();
            for (int j = 0; j < keywords.size(); j++) {
                if (keywords.get(j).contains(keyword) || places.get(i).getName().toUpperCase().contains(keyword)) {
                    mClusterManager.addItem(new MyCluster(places.get(i).getPosition().latitude, places.get(i).getPosition().longitude));
                }
            }
        }
        mMap.moveCamera(CameraUpdateFactory
                .newLatLngBounds(GRAND_LYON,10));
    }

    public void showPolyline() {
        if (currentLine != null) {
            currentLine.remove();
        }
        URL url = Tools.generateGoogleMapURL(markers);
        PostTask postTask = new PostTask(url.toString());
        postTask.delegate = new HandleVisualization(CreerParcours.this);
        postTask.execute();
    }

    public void visualizeReceived(JsonObject results){
        if (results != null) {
            List<LatLng> listePoints = Tools.decodeDirections(results);
            currentLine = mMap.addPolyline(new PolylineOptions()
                    .addAll(listePoints != null ? listePoints : new ArrayList<LatLng>()));
        }
    }

    public void getPlacesReceived(JsonObject results){
        if (results != null) {
            JsonArray resultsArray = results.getAsJsonArray("places");
            if (resultsArray != null) {
                for (int i = 0; i < resultsArray.size(); i++) {
                    places.add(new Place(resultsArray.get(i).getAsJsonObject()));
                    clusterItems.add(new MyCluster(places.get(i).getPosition().latitude,places.get(i).getPosition().longitude));
                    mClusterManager.addItem(new MyCluster(places.get(i).getPosition().latitude,places.get(i).getPosition().longitude));
                }
                mMap.moveCamera(CameraUpdateFactory
                        .newLatLngBounds(GRAND_LYON,10));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ASK_FOR_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 & grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!

                } else {

                    // permission denied, boo!
                }
                return;
            }
            case ASK_FOR_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 & grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                } else {
                    // permission denied, boo!
                }
                return;
            }
         }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 2) {
            finish();
        }
    }

    public ArrayList<Marker> getMarkers() {
        return markers;
    }
}

class HandleVisualization implements PostTask.AsyncResponse{

    private CreerParcours creerParcours;

    public HandleVisualization(CreerParcours creerParcours) {
        this.creerParcours = creerParcours;
    }

    @Override
    public void processFinish(JsonObject results) {
        this.creerParcours.visualizeReceived(results);
    }
}

class HandleGetPlaces implements GetTask.AsyncResponse{

    private CreerParcours creerParcours;

    public HandleGetPlaces(CreerParcours creerParcours) {
        this.creerParcours = creerParcours;
    }

    @Override
    public void processFinish(JsonObject results) {
        this.creerParcours.getPlacesReceived(results);
    }
}

