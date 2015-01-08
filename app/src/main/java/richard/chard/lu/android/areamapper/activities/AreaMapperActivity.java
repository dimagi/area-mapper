package richard.chard.lu.android.areamapper.activities;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
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
        GoogleApiClient.OnConnectionFailedListener, AreaCalculator.Listener,
        LocationListener {

    private static final Logger LOG = Logger.create(AreaMapperActivity.class);

    public static final String EXTRA_KEY_MODE = "area_mapper_mode";

    private static final double LOCATION_MIN_ACCURACY = 35;

    private static final float MAP_INITIAL_ZOOM_LEVEL = 16;

    private static final long MAPVIEW_CHECK_DELAY = 200;

    public static final int MODE_WALK = 0;

    private AreaCalculator areaCalculator = new AreaCalculator(this);

    private GoogleApiClient googleApiClient;

    private boolean isStarted;

    private GoogleMap map;
    private MapView mapView;

    private Location previousLocation;

    private TextView textViewArea;

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

        map.clear();
        map.addPolygon(
                areaCalculator.getPolygonOptions()
        );
        map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        latLng,
                        MAP_INITIAL_ZOOM_LEVEL
                )
        );

        textViewArea.setText(area+" sq m");

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

                isStarted = true;
                break;

            case R.id.button_stop:

                findViewById(R.id.button_stop).setVisibility(View.GONE);
                findViewById(R.id.button_redo).setVisibility(View.VISIBLE);
                findViewById(R.id.button_ok).setVisibility(View.VISIBLE);

                isStarted = false;

                LocationServices.FusedLocationApi.removeLocationUpdates(
                        googleApiClient,
                        this
                );

                // TODO: show full area on map
                break;

            case R.id.button_ok:

                Bundle result = new Bundle();
                result.putDouble(
                        "area",
                        areaCalculator.getArea()
                );

                Intent data = new Intent();
                data.putExtra(
                        "odk_intent_bundle",
                        result
                );

                setResult(ResultCode.OK, data);
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

        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient,
                LocationRequest
                        .create()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(0)
                ,
                this
        );

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

        textViewArea = (TextView) findViewById(R.id.textview_area);

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
    public void onLocationChanged(Location location) {
        LOG.trace("Entry");

        if (location.getAccuracy() <= LOCATION_MIN_ACCURACY) {
            LOG.debug("location.getAccuracy()={}", location.getAccuracy());

            boolean addLocation = previousLocation == null ||
                    (
                            isStarted &&
                            location.distanceTo(previousLocation) > previousLocation.getAccuracy()
                    );

            if (addLocation) {

                areaCalculator.addLatLng(
                        new LatLng(
                                location.getLatitude(),
                                location.getLongitude()
                        )
                );

                previousLocation = location;
            }
        }

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
                map.getUiSettings().setZoomControlsEnabled(true);
                map.setMyLocationEnabled(true);
                break;

            default:
                throw new RuntimeException("Unknown mode: "+getMode());
        }

        LOG.trace("Exit");
    }

    @Override
    public void onPause() {
        LOG.trace("Entry");

        if (googleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    googleApiClient,
                    this
            );
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
