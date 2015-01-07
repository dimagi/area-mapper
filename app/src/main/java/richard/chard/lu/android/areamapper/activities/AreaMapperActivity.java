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
import richard.chard.lu.android.areamapper.Logger;
import richard.chard.lu.android.areamapper.R;
import richard.chard.lu.android.areamapper.ResultCode;

/**
 * @author Richard Lu
 */
public class AreaMapperActivity extends ActionBarActivity
        implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, AreaCalculator.Listener {

    private static final Logger LOG = Logger.create(AreaMapperActivity.class);

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
        LOG.trace("Entry");

        setResult(ResultCode.CANCEL);
        finish();

        LOG.trace("Exit");
    }

    @Override
    public void onAreaChange(LatLng latLng, double area) {
        LOG.trace("Entry, area={}", area);

        // TODO
        map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        latLng,
                        MAP_INITIAL_ZOOM_LEVEL));

        LOG.trace("Exit");
    }

    @Override
    public void onClick(View view) {
        LOG.trace("Entry");

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

        LOG.trace("Exit");
    }

    @Override
    public void onConnected(Bundle bundle) {
        LOG.trace("Entry");

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

        LOG.trace("Exit");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        LOG.trace("Entry");

        LOG.trace("Exit");
    }

    @Override
    public void onConnectionSuspended(int i) {
        LOG.trace("Entry");

        LOG.trace("Exit");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOG.trace("Entry");

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

        LOG.trace("Exit");
    }

    @Override
    public void onDestroy() {
        LOG.trace("Entry");

        mapView.onDestroy();

        super.onDestroy();

        LOG.trace("Exit");
    }

    @Override
    public void onLowMemory() {
        LOG.trace("Entry");

        mapView.onLowMemory();

        super.onLowMemory();

        LOG.trace("Exit");
    }

    protected void onMapAvailable() {
        LOG.trace("Entry");

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

        LOG.trace("Exit");
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
        LOG.trace("Entry");

        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }

        mapView.onPause();

        super.onPause();

        LOG.trace("Exit");
    }

    @Override
    public void onResume() {
        LOG.trace("Entry");

        super.onResume();

        mapView.onResume();

        if (googleApiClient != null) {
            googleApiClient.connect();
        }

        LOG.trace("Exit");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        LOG.trace("Entry");

        super.onSaveInstanceState(outState);

        mapView.onSaveInstanceState(outState);

        LOG.trace("Exit");
    }

}
