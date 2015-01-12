package richard.chard.lu.android.areamapper.activities;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import richard.chard.lu.android.areamapper.AreaCalculator;
import richard.chard.lu.android.areamapper.Logger;
import richard.chard.lu.android.areamapper.R;
import richard.chard.lu.android.areamapper.ResultCode;
import richard.chard.lu.android.areamapper.StopPropagationTouchListener;

/**
 * @author Richard Lu
 */
public class AreaMapperActivity extends ActionBarActivity
        implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, AreaCalculator.Listener,
        LocationListener, OnMapReadyCallback, SeekBar.OnSeekBarChangeListener,
        ValueAnimator.AnimatorUpdateListener {

    private static final Logger LOG = Logger.create(AreaMapperActivity.class);

    private static final int ANIMATION_DURATION_MS = 150;

    private static int LOCATION_MIN_ACCURACY = 35;
    private static final int LOCATION_MIN_MAX_ACCURACY = 50;
    private static final int LOCATION_MIN_MIN_ACCURACY = 10;

    private static final float MAP_INITIAL_ZOOM_LEVEL = 16;

    private static final float MAP_SCROLL_PX = 150;

    private ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

    private AreaCalculator areaCalculator = new AreaCalculator(this);

    private ImageButton buttonSettings;

    private GoogleApiClient googleApiClient;

    private boolean isCameraInitiallyPositioned;
    private boolean isRecording;

    private LinearLayout linearLayoutSettings;

    private GoogleMap map;
    private MapView mapView;

    private Location previousLocation;

    private TextView textViewArea;
    private TextView textViewLocationAccuracy;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void freezeOrientation() {
        LOG.trace("Entry");

        final int ORIENTATION = getResources().getConfiguration().orientation;

        final int PORTRAIT = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 ?
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT :
                ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;

        final int LANDSCAPE = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 ?
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE :
                ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE;

        switch (ORIENTATION) {
            case Configuration.ORIENTATION_PORTRAIT:

                setRequestedOrientation(PORTRAIT);
                break;

            case Configuration.ORIENTATION_LANDSCAPE:

                setRequestedOrientation(LANDSCAPE);
                break;

            default:
                throw new RuntimeException("Unknown orientation: "+ORIENTATION);
        }

        LOG.trace("Exit");
    }

    private int getColor(int colorId) {
        return getResources().getColor(colorId);
    }

    @Override
    public Polygon getPolygon(PolygonOptions polygonOptions) {
        LOG.trace("Entry");

        Polygon polygon =  map.addPolygon(
                polygonOptions
                        .strokeColor(
                                getColor(R.color.lightblue_500)
                        )
                        .fillColor(
                                getColor(R.color.lightblue_100_transparent_99)
                        )
        );

        LOG.trace("Exit");
        return polygon;
    }

    private void initializeViews() {
        LOG.trace("Entry");

        setContentView(R.layout.activity_area_mapper);

        findViewById(R.id.linearlayout_progressbar).setOnTouchListener(
                StopPropagationTouchListener.getInstance());

        findViewById(R.id.button_map_pan_left).setOnClickListener(this);
        findViewById(R.id.button_map_pan_up).setOnClickListener(this);
        findViewById(R.id.button_map_pan_right).setOnClickListener(this);
        findViewById(R.id.button_map_pan_down).setOnClickListener(this);

        findViewById(R.id.button_cancel).setOnClickListener(this);
        findViewById(R.id.button_start).setOnClickListener(this);
        findViewById(R.id.button_pause).setOnClickListener(this);
        findViewById(R.id.button_resume).setOnClickListener(this);
        findViewById(R.id.button_stop).setOnClickListener(this);
        findViewById(R.id.button_redo).setOnClickListener(this);
        findViewById(R.id.button_ok).setOnClickListener(this);

        textViewArea = (TextView) findViewById(R.id.textview_area);

        textViewArea.setText(
                getString(R.string.area_format, 0d)
        );

        mapView = (MapView) findViewById(R.id.mapview);

        mapView.getMapAsync(this);

        buttonSettings = (ImageButton) findViewById(R.id.button_settings);
        buttonSettings.setOnClickListener(this);

        linearLayoutSettings = (LinearLayout) findViewById(R.id.linearlayout_settings);
        linearLayoutSettings.setOnTouchListener(StopPropagationTouchListener.getInstance());

        SeekBar seekBarLocationAccuracy = (SeekBar) findViewById(R.id.seekbar_location_accuracy);
        seekBarLocationAccuracy.setMax(LOCATION_MIN_MAX_ACCURACY - LOCATION_MIN_MIN_ACCURACY);
        seekBarLocationAccuracy.setProgress(LOCATION_MIN_ACCURACY - LOCATION_MIN_MIN_ACCURACY);
        seekBarLocationAccuracy.setOnSeekBarChangeListener(this);

        textViewLocationAccuracy = (TextView) findViewById(R.id.textview_location_accuracy);
        textViewLocationAccuracy.setText(
                getString(
                        R.string.location_accuracy_format,
                        LOCATION_MIN_ACCURACY
                )
        );

        LOG.trace("Exit");
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {

        float value = (float) valueAnimator.getAnimatedValue();
        linearLayoutSettings.setAlpha(value);
        buttonSettings.setRotation(value * 180);

        if (value > 0) {
            linearLayoutSettings.setVisibility(View.VISIBLE);
        } else {
            linearLayoutSettings.setVisibility(View.INVISIBLE);
        }

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
            case R.id.button_map_pan_left:

                panMap(
                        -MAP_SCROLL_PX,
                        0
                );
                break;

            case R.id.button_map_pan_up:

                panMap(
                        0,
                        -MAP_SCROLL_PX
                );
                break;

            case R.id.button_map_pan_right:

                panMap(
                        MAP_SCROLL_PX,
                        0
                );
                break;

            case R.id.button_map_pan_down:

                panMap(
                        0,
                        MAP_SCROLL_PX
                );
                break;

            case R.id.button_cancel:

                setResult(ResultCode.CANCEL);
                finish();
                break;

            case R.id.button_start:
            case R.id.button_resume:

                findViewById(R.id.button_start).setVisibility(View.GONE);
                findViewById(R.id.button_resume).setVisibility(View.GONE);
                findViewById(R.id.button_pause).setVisibility(View.VISIBLE);
                findViewById(R.id.button_stop).setVisibility(View.VISIBLE);

                isRecording = true;
                break;

            case R.id.button_pause:

                findViewById(R.id.button_pause).setVisibility(View.GONE);
                findViewById(R.id.button_resume).setVisibility(View.VISIBLE);

                isRecording = false;
                break;

            case R.id.button_stop:

                findViewById(R.id.button_resume).setVisibility(View.GONE);
                findViewById(R.id.button_pause).setVisibility(View.GONE);
                findViewById(R.id.button_stop).setVisibility(View.GONE);
                findViewById(R.id.button_redo).setVisibility(View.VISIBLE);
                findViewById(R.id.button_ok).setVisibility(View.VISIBLE);

                isRecording = false;

                LocationServices.FusedLocationApi.removeLocationUpdates(
                        googleApiClient,
                        this
                );

                ListView listViewCoordinates = (ListView) findViewById(R.id.listview_coordinates);
                listViewCoordinates.setAdapter(
                        areaCalculator.getArrayAdapter(
                                this,
                                android.R.layout.simple_list_item_1
                        )
                );

                View frameLayoutMapContainer = findViewById(R.id.framelayout_map_container);

                frameLayoutMapContainer.getLayoutParams().width = frameLayoutMapContainer.getWidth();
                listViewCoordinates.getLayoutParams().width = frameLayoutMapContainer.getWidth();

                listViewCoordinates.setVisibility(View.VISIBLE);
                listViewCoordinates.post(new Runnable() {
                    @Override
                    public void run() {
                        ((HorizontalScrollView) findViewById(R.id.scrollview_result))
                                .smoothScrollBy(
                                        mapView.getWidth() / 10,
                                        0
                                );
                    }
                });
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

            case R.id.button_settings:

                if (!animator.isRunning()) {
                    if (linearLayoutSettings.getAlpha() == 0) {
                        animator.start();
                    } else {
                        animator.reverse();
                    }
                }
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

        freezeOrientation();

        animator.setDuration(ANIMATION_DURATION_MS);
        animator.addUpdateListener(this);

        initializeViews();

        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mapView.onCreate(savedInstanceState);

        LOG.trace("Exit");
    }

    @Override
    protected void onDestroy() {
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

    @Override
    public void onMapReady(GoogleMap map) {
        LOG.trace("Entry");

        MapsInitializer.initialize(getApplicationContext());

        this.map = map;

        map.getUiSettings().setAllGesturesEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setMyLocationEnabled(true);

        updateProgressState();

        LOG.trace("Exit");
    }

    @Override
    protected void onPause() {
        LOG.trace("Entry");

        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    googleApiClient,
                    this
            );
        }

        mapView.onPause();

        super.onPause();

        LOG.trace("Exit");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        LOCATION_MIN_ACCURACY = LOCATION_MIN_MIN_ACCURACY + progress;
        textViewLocationAccuracy.setText(
                getString(
                        R.string.location_accuracy_format,
                        LOCATION_MIN_ACCURACY
                )
        );

    }

    @Override
    protected void onResume() {
        LOG.trace("Entry");

        super.onResume();

        mapView.onResume();

        LOG.trace("Exit");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LOG.trace("Entry");

        super.onSaveInstanceState(outState);

        mapView.onSaveInstanceState(outState);

        LOG.trace("Exit");
    }

    @Override
    protected void onStart() {
        LOG.trace("Entry");

        super.onStart();

        googleApiClient.connect();

        LOG.trace("Exit");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        LOG.trace("Entry");

        LOG.trace("Exit");
    }

    @Override
    protected void onStop() {
        LOG.trace("Entry");

        googleApiClient.disconnect();

        super.onStop();

        LOG.trace("Exit");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        LOG.trace("Entry");

        LOG.trace("Exit");
    }

    private void panMap(float dx, float dy) {

        map.animateCamera(
                CameraUpdateFactory.scrollBy(dx, dy),
                ANIMATION_DURATION_MS,
                null
        );

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
