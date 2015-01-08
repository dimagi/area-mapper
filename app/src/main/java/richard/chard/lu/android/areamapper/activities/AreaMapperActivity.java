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

    private static final double LOCATION_MIN_ACCURACY = 35;

    private static final float MAP_INITIAL_ZOOM_LEVEL = 16;

    private static final long MAPVIEW_CHECK_DELAY = 200;

    private AreaCalculator areaCalculator = new AreaCalculator(this);

    private GoogleApiClient googleApiClient;

    private boolean isCameraInitiallyPositioned;
    private boolean isRecording;

    private GoogleMap map;
    private MapView mapView;

    private Location previousLocation;

    private TextView textViewArea;

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

        if (!isCameraInitiallyPositioned) {
            isCameraInitiallyPositioned = true;

            map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            latLng,
                            MAP_INITIAL_ZOOM_LEVEL
                    )
            );

            updateProgressState();
        } else {

            map.clear();
            map.addPolygon(
                    areaCalculator.getPolygonOptions()
            );
            map.moveCamera(
                    CameraUpdateFactory.newLatLng(latLng)
            );

            textViewArea.setText(
                    getString(R.string.area_format, area)
            );

        }

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

                isRecording = true;
                break;

            case R.id.button_stop:

                findViewById(R.id.button_stop).setVisibility(View.GONE);
                findViewById(R.id.button_redo).setVisibility(View.VISIBLE);
                findViewById(R.id.button_ok).setVisibility(View.VISIBLE);

                isRecording = false;

                LocationServices.FusedLocationApi.removeLocationUpdates(
                        googleApiClient,
                        this
                );
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

        updateProgressState();

        LOG.trace("Exit");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        LOG.trace("Entry");

        setResult(ResultCode.ERROR);
        finish();

        LOG.trace("Exit");
    }

    @Override
    public void onConnectionSuspended(int i) {
        LOG.trace("Entry");

        setResult(ResultCode.CANCEL);
        finish();

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

        textViewArea.setText(
                getString(R.string.area_format, 0d)
        );

        mapView = (MapView) findViewById(R.id.mapview);

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
                            isRecording &&
                            location.distanceTo(previousLocation) > previousLocation.getAccuracy()
                    );

            if (addLocation) {

                previousLocation = location;

                areaCalculator.addLatLng(
                        new LatLng(
                                location.getLatitude(),
                                location.getLongitude()
                        )
                );
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

        map = mapView.getMap();

        map.getUiSettings().setAllGesturesEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setMyLocationEnabled(true);

        updateProgressState();

        LOG.trace("Exit");
    }

    @Override
    public void onPause() {
        LOG.trace("Entry");

        if (googleApiClient.isConnected()) {
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

        if (!googleApiClient.isConnected()) {
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

    private void updateProgressState() {
        LOG.trace("Entry");

        if (map != null) {

            if (mapView.getVisibility() == View.INVISIBLE) {

                mapView.setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.textview_progress)).setText(R.string.loading_gps);

            }

            if (googleApiClient.isConnected() &&
                    previousLocation != null) {

                findViewById(R.id.linearlayout_progressbar).setVisibility(View.GONE);
                findViewById(R.id.button_start).setEnabled(true);

            }
        }

        LOG.trace("Exit");
    }

}
