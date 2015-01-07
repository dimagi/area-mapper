package richard.chard.lu.android.areamapper.activities;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;

import richard.chard.lu.android.areamapper.AreaCalculator;
import richard.chard.lu.android.areamapper.R;
import richard.chard.lu.android.areamapper.ResultCode;

/**
 * @author Richard Lu
 */
public class AreaMapperActivity extends ActionBarActivity
        implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, AreaCalculator.Listener {

    public static final String EXTRA_KEY_MODE = "area_mapper_mode";

    private static final float MAP_INITIAL_ZOOM_LEVEL = 16;

    private static final long MAPVIEW_CHECK_DELAY = 200;

    public static final int MODE_WALK = 0;
    public static final int MODE_DRAW = 1;

    private AreaCalculator areaCalculator = new AreaCalculator(this);

    private GoogleApiClient googleApiClient;

    private GoogleMap map;
    private MapView mapView;

    private int getMode() {
        return getIntent().getIntExtra(
                EXTRA_KEY_MODE,
                MODE_WALK
        );
    }

    @Override
    public void onBackPressed() {

        setResult(ResultCode.CANCEL);
        finish();

    }

    @Override
    public void onAreaChange(LatLng latLng, double area) {
        // TODO
        map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        latLng,
                        MAP_INITIAL_ZOOM_LEVEL));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_cancel:

                setResult(ResultCode.CANCEL);
                finish();
                break;

            case R.id.button_start:

                findViewById(R.id.button_start).setVisibility(View.GONE);
                findViewById(R.id.button_stop).setVisibility(View.VISIBLE);
                // TODO
                break;

            case R.id.button_stop:

                findViewById(R.id.button_stop).setVisibility(View.GONE);
                findViewById(R.id.button_redo).setVisibility(View.VISIBLE);
                findViewById(R.id.button_ok).setVisibility(View.VISIBLE);

                // TODO: show full area on map
                break;

            case R.id.button_ok:

                Intent result = new Intent();
                // TODO: save area
                setResult(ResultCode.OK, result);
                finish();
                break;

            case R.id.button_redo:

                setResult(ResultCode.REDO);
                finish();
                break;

            default:
                throw new RuntimeException("Unknown view id: "+view.getId());
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        mapView.post(new Runnable() {

            @Override
            public void run() {
                if (mapView.getMap() != null) {
                    onMapAvailable();
                } else {
                    mapView.postDelayed(
                            this,
                            MAPVIEW_CHECK_DELAY);
                }
            }

        });

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_area_mapper);

        findViewById(R.id.button_cancel).setOnClickListener(this);
        findViewById(R.id.button_start).setOnClickListener(this);
        findViewById(R.id.button_stop).setOnClickListener(this);
        findViewById(R.id.button_redo).setOnClickListener(this);
        findViewById(R.id.button_ok).setOnClickListener(this);

        mapView = (MapView) findViewById(R.id.mapview);

        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mapView.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {

        mapView.onDestroy();

        super.onDestroy();

    }

    @Override
    public void onLowMemory() {

        mapView.onLowMemory();

        super.onLowMemory();

    }

    protected void onMapAvailable() {

        MapsInitializer.initialize(getApplicationContext());

        findViewById(R.id.linearlayout_progressbar).setVisibility(View.GONE);
        findViewById(R.id.linearlayout_areamapper).setVisibility(View.VISIBLE);

        map = mapView.getMap();

        switch (getMode()) {
            case MODE_WALK:

                map.getUiSettings().setAllGesturesEnabled(false);
                map.setMyLocationEnabled(true);

                // TODO
                Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

                if (location != null) {
                    areaCalculator.addLatLng(
                            new LatLng(
                                    location.getLatitude(),
                                    location.getLongitude()
                            )
                    );
                }
                break;

            case MODE_DRAW:

//            map.setOnMapLongClickListener(this);
//            map.setOnMarkerDragListener(this);
                break;

            default:
                throw new RuntimeException("Unknown mode: "+getMode());
        }

    }

//    @Override
//    public void onMapLongClick(LatLng latLng) {
//
//        map.clear();
//
//        CircleOptions circleOptions = new CircleOptions()
//                .center(latLng)
//                .radius(selectedLocationRadius)
//                .fillColor(Color.argb(150, 255, 0, 0));
//
//        selectionCircle = map
//                .addCircle(circleOptions);
//
//        MarkerOptions markerOptions = new MarkerOptions()
//                .flat(true)
//                .draggable(true)
//                .position(latLng);
//
//        Marker selectionMarker = map.addMarker(markerOptions);
//        selectedLocation = selectionMarker.getPosition();
//
//        if (isLocationFrozen) {
//            selectionMarker.setDraggable(false);
//        } else {
//            buttonSave.setEnabled(true);
//        }
//
//        LOG.trace("Exit");
//    }

//    @Override
//    public void onMarkerDragStart(Marker marker) {
//        LOG.trace("Entry");
//
//        LOG.trace("Exit");
//    }
//
//    @Override
//    public void onMarkerDrag(Marker marker) {
//        selectionCircle.setCenter(marker.getPosition());
//    }
//
//    @Override
//    public void onMarkerDragEnd(Marker marker) {
//        LOG.trace("Entry");
//
//        selectedLocation = marker.getPosition();
//
//        LOG.trace("Exit");
//    }

    @Override
    public void onPause() {

        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }

        mapView.onPause();

        super.onPause();

    }

    @Override
    public void onResume() {

        super.onResume();

        mapView.onResume();

        if (googleApiClient != null) {
            googleApiClient.connect();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        mapView.onSaveInstanceState(outState);

    }

}
