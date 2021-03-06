package tw.chikuo.lockersmap.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.ui.IconGenerator;

import java.util.HashMap;
import java.util.Map;

import tw.chikuo.lockersmap.R;
import tw.chikuo.lockersmap.object.Stop;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private GeoQuery geoQuery;
    private GeoLocation currentLocation;
    private Map<String,Stop> stopsMap = new HashMap<>();
    private HashMap<Marker,Stop>  markerStopsHashMap = new HashMap<>();

    private FirebaseDatabase database;
    private DatabaseReference locationsRef;
    private DatabaseReference stopsRef;

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Setup Firebase library
        database = FirebaseDatabase.getInstance();
        locationsRef = database.getReference("Locations");
        stopsRef = database.getReference("Stop");

        // Show permission for android 6.0
        String perm = android.Manifest.permission.ACCESS_FINE_LOCATION;
        if (ContextCompat.checkSelfPermission(MapsActivity.this, perm) != PackageManager.PERMISSION_GRANTED) {
            // Permission did not granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(MapsActivity.this,new String[]{perm},REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            // Permission granted
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(MapsActivity.this);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mMap != null) {

            // Check Permission
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            // Show my location and room in
            mMap.setMyLocationEnabled(true);
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
            if (location != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                        .zoom(15)                   // Sets the zoom
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                // Update
                if (geoQuery != null) {
                    currentLocation = new GeoLocation(location.getLatitude(), location.getLongitude());
                    geoQuery.setCenter(currentLocation);
                }
            }

            // Query stops
            GeoFire geoFire = new GeoFire(locationsRef);
            currentLocation = new GeoLocation(25.0478, 121.517); // Default
            geoQuery = geoFire.queryAtLocation(currentLocation, 20); // KM
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(final String key, final GeoLocation location) {

                    if (!stopsMap.containsKey(key)) {
                        // Query for each location & add to map
                        Query stopsQuery = stopsRef.child(key);
                        stopsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Stop stop = dataSnapshot.getValue(Stop.class);
                                stop.setId(key);
                                if (stop != null && stop.getName() != null) {
                                    stopsMap.put(key, stop);

                                    LatLng latLng = new LatLng(location.latitude, location.longitude);
                                    addIcon(stop, latLng);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    }

                    Log.d("geoQuery", "key = " + key + ",longitude = " + location.longitude + ", latitude = " + location.latitude);
                }

                @Override
                public void onKeyExited(String key) {
                    Log.d("geoQuery", "");

                    // TODO Need delete ?
                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                    Log.d("geoQuery", "");
                }

                @Override
                public void onGeoQueryReady() {
                    Log.d("geoQuery", "");
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    Log.d("geoQuery", "");
                }
            });

//            // Map infoWindow click
//            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
//                @Override
//                public void onInfoWindowClick(Marker marker) {
//                    Stop stop = markerStopsHashMap.get(marker);
//
//                    Intent intent = new Intent(MapsActivity.this, StopActivity.class);
//                    intent.putExtra("stop", stop);
//                    startActivity(intent);
//                }
//            });

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {

                    Stop stop = markerStopsHashMap.get(marker);

                    Intent intent = new Intent(MapsActivity.this, StopActivity.class);
                    intent.putExtra("stop", stop);
                    startActivity(intent);
                    return false;
                }
            });

            mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                @Override
                public void onCameraMove() {
                    LatLng latLng = mMap.getCameraPosition().target;
                    if (latLng != null) {
                        geoQuery.setCenter(new GeoLocation(latLng.latitude, latLng.longitude));
                    }
                    float level = mMap.getCameraPosition().zoom;
                    if (level <= 8){
                        geoQuery.setRadius(200);
                    } else if (level <= 10){
                        geoQuery.setRadius(100);
                    } else {
                        geoQuery.setRadius(20);
                    }
                }
            });

        }

    }


    private void addIcon(Stop stop, LatLng position) {

        // Add marker to map , Use Google Maps Android API utility library

        IconGenerator iconFactory = new IconGenerator(MapsActivity.this);
        iconFactory.setTextAppearance(R.style.mapIconText);

        CharSequence text = stop.getLowest_price();
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(text))).
                position(position).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

        // Setup marker with seller calendar data.
        Marker marker = mMap.addMarker(markerOptions);
        markerStopsHashMap.put(marker, stop);
    }

    private void addMarker(Stop stop, LatLng latLng) {

        // Add marker to map
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(stop.getName())
                .flat(true);
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.)
        Marker marker = mMap.addMarker(markerOptions);
        markerStopsHashMap.put(marker, stop);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // granted
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        } else {
            // no granted
            // TODO show error message
        }
    }
}
