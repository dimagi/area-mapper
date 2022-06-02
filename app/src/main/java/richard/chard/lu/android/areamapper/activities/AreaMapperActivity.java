package richard.chard.lu.android.areamapper.activities;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import richard.chard.lu.android.areamapper.AreaCalculator;
import richard.chard.lu.android.areamapper.Logger;
import richard.chard.lu.android.areamapper.R;
import richard.chard.lu.android.areamapper.ResultCode;
import richard.chard.lu.android.areamapper.SaveImageAsyncTask;
import richard.chard.lu.android.areamapper.StopPropagationTouchListener;

/**
 * @author Richard Lu
 */
public class AreaMapperActivity extends AppCompatActivity
        implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, AreaCalculator.Listener,
        LocationListener, OnMapReadyCallback, SeekBar.OnSeekBarChangeListener,
        ValueAnimator.AnimatorUpdateListener, GoogleMap.SnapshotReadyCallback,
        SaveImageAsyncTask.OnImageSavedListener {

    private static final int ANIMATION_DURATION_MS = 150;

    public static final String EXTRA_KEY_ACCURACY = "accuracy";
    public static final String EXTRA_KEY_COORDINATES = "coordinates";
    public static final String EXTRA_KEY_IMAGE = "image";
    public static final String EXTRA_KEY_INTERVAL_METERS = "interval_meters";
    public static final String EXTRA_KEY_INTERVAL_MILLIS = "interval_millis";
    public static final String EXTRA_KEY_IS_REDO = "is_redo";
    public static final String EXTRA_KEY_PERIMETER = "perimeter";
    public static final String EXTRA_KEY_RESPONSE_BUNDLE = "odk_intent_bundle";
    public static final String CASE_NAME = "case_name";
    public static final String PLOT_TYPE = "plot_type";
    public static final String CASE_LABEL = "case_label";
    public static final String PLOT_LABEL = "plot_label";
    public static final String INTENT_RESULT = "odk_intent_data";

    private static final String IMAGE_FILE_FOLDER = "AreaMapperImages";
    private static final String IMAGE_FILE_PREFIX = "map_image-";
    private static final String IMAGE_FILE_SUFFIX = ".png";

    private static final int LOCATION_MIN_MAX_ACCURACY = 50;
    private static final int LOCATION_MIN_MIN_ACCURACY = 10;

    private static final Logger LOG = Logger.create(AreaMapperActivity.class);

    private static final float MAP_INITIAL_ZOOM_LEVEL = 16;

    private static final float MAP_SCROLL_PX = 150;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1001;


    private ValueAnimator settingsMenuAnimator = ValueAnimator.ofFloat(0f, 1f);

    private AreaCalculator areaCalculator = new AreaCalculator(this);

    private ImageButton buttonSettings;

    private GoogleApiClient googleApiClient;

    private boolean isCameraInitiallyPositioned;

    private boolean isCoordinatesReturnRequired;
    private boolean isImageReturnRequired;

    private boolean isRecording;

    private boolean isRedo;

    private LinearLayout linearLayoutSettings;

    private int locationMinAccuracy = 35;

    private GoogleMap map;
    private String mapSnapshotPath;
    private MapView mapView;

    private Location previousLocation;

    private int recordingIntervalMeters = 0;
    private int recordingIntervalMillis = 0;

    private TextView textViewArea;
    private TextView textViewLocationAccuracy;

    private String caseName;
    private String plotType;
    private String caseNameLabel = "";
    private String plotTypeLabel = "";


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
                throw new RuntimeException("Unknown orientation: " + ORIENTATION);
        }

        LOG.trace("Exit");
    }

    private int getColorResource(int colorId) {
        return getResources().getColor(colorId);
    }

    @Override
    public Polygon getPolygon(PolygonOptions polygonOptions) {
        LOG.trace("Entry");

        Polygon polygon = map.addPolygon(
                polygonOptions
                        .strokeColor(
                                getColorResource(R.color.lightblue_500)
                        )
                        .fillColor(
                                getColorResource(R.color.lightblue_100_transparent_99)
                        )
        );

        LOG.trace("Exit");
        return polygon;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void initializeParameters() {
        LOG.trace("Entry");

        final LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        Bundle params = getIntent().getExtras();

        if (params != null) {

            if (params.containsKey(EXTRA_KEY_ACCURACY)) {
                locationMinAccuracy = Math.max(
                        LOCATION_MIN_MIN_ACCURACY,
                        Math.min(
                                LOCATION_MIN_MAX_ACCURACY,
                                Integer.valueOf(params.getString(EXTRA_KEY_ACCURACY))
                        )
                );
            }
            if (params.containsKey(EXTRA_KEY_INTERVAL_METERS)) {
                recordingIntervalMeters =
                        Integer.valueOf(params.getString(EXTRA_KEY_INTERVAL_METERS));
            }
            if (params.containsKey(EXTRA_KEY_INTERVAL_MILLIS)) {
                recordingIntervalMillis =
                        Integer.valueOf(params.getString(EXTRA_KEY_INTERVAL_MILLIS));
            }
            if (params.containsKey(EXTRA_KEY_COORDINATES)) {
                isCoordinatesReturnRequired =
                        Boolean.valueOf(params.getString(EXTRA_KEY_COORDINATES));
            }
            if (params.containsKey(EXTRA_KEY_IMAGE)) {
                isImageReturnRequired =
                        Boolean.valueOf(params.getString(EXTRA_KEY_IMAGE));
            }
            if (params.containsKey(EXTRA_KEY_IS_REDO)) {
                isRedo = params.getBoolean(EXTRA_KEY_IS_REDO);
            }
            if (params.containsKey(CASE_NAME)) {
                caseName = params.getString(CASE_NAME);
            }
            if (params.containsKey(PLOT_TYPE)) {
                plotType = params.getString(PLOT_TYPE);
            }
            if (params.containsKey(PLOT_LABEL)) {
                plotTypeLabel = params.getString(PLOT_LABEL);
            }
            if (params.containsKey(CASE_LABEL)) {
                caseNameLabel = params.getString(CASE_LABEL);
            }


        }

        LOG.trace("Exit");
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

        textViewArea = findViewById(R.id.textview_area);

        textViewArea.setText(
                getString(R.string.area_format, 0d)
        );

        mapView = findViewById(R.id.mapview);

        mapView.getMapAsync(this);

        if (caseName != null) {
            TextView caseNameView = findViewById(R.id.case_name);
            caseNameView.setVisibility(View.VISIBLE);
            caseNameView.setText(caseNameLabel + caseName);
        }

        if (plotType != null) {
            TextView plotTypeView = findViewById(R.id.plot_type);
            plotTypeView.setVisibility(View.VISIBLE);
            plotTypeView.setText(plotTypeLabel + plotType);
        }

        buttonSettings = findViewById(R.id.button_settings);
        buttonSettings.setOnClickListener(this);

        linearLayoutSettings = findViewById(R.id.linearlayout_settings);
        linearLayoutSettings.setOnTouchListener(StopPropagationTouchListener.getInstance());

        SeekBar seekBarLocationAccuracy = findViewById(R.id.seekbar_location_accuracy);
        seekBarLocationAccuracy.setMax(LOCATION_MIN_MAX_ACCURACY - LOCATION_MIN_MIN_ACCURACY);
        seekBarLocationAccuracy.setProgress(locationMinAccuracy - LOCATION_MIN_MIN_ACCURACY);
        seekBarLocationAccuracy.setOnSeekBarChangeListener(this);

        textViewLocationAccuracy = findViewById(R.id.textview_location_accuracy);
        textViewLocationAccuracy.setText(
                getString(
                        R.string.location_accuracy_format,
                        locationMinAccuracy
                )
        );

        LOG.trace("Exit");
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {

        float value = (float)valueAnimator.getAnimatedValue();
        linearLayoutSettings.setAlpha(value);
        buttonSettings.setRotation(value * 180);

        if (value > 0) {
            linearLayoutSettings.setVisibility(View.VISIBLE);
        } else {
            linearLayoutSettings.setVisibility(View.INVISIBLE);
        }

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
    public void onBackPressed() {
        LOG.trace("Entry");

        setResult(ResultCode.CANCEL);
        finish();

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

                ListView listViewCoordinates = findViewById(R.id.listview_coordinates);
                listViewCoordinates.setAdapter(
                        areaCalculator.getArrayAdapter(
                                this,
                                android.R.layout.simple_list_item_1
                        )
                );

                ((TextView)findViewById(R.id.textview_perimeter)).setText(
                        getString(R.string.perimeter_format, areaCalculator.getPerimeter())
                );

                View frameLayoutMapContainer = findViewById(R.id.framelayout_map_container);
                View linearLayoutResult = findViewById(R.id.linearlayout_result);

                frameLayoutMapContainer.getLayoutParams().width = frameLayoutMapContainer.getWidth();
                linearLayoutResult.getLayoutParams().width = frameLayoutMapContainer.getWidth();

                linearLayoutResult.setVisibility(View.VISIBLE);
                linearLayoutResult.post(() -> ((HorizontalScrollView)findViewById(R.id.scrollview_result))
                        .smoothScrollBy(
                                mapView.getWidth() / 10,
                                0
                        ));

                if (isImageReturnRequired) {
                    findViewById(R.id.button_ok).setEnabled(false);
                    map.snapshot(this);
                }
                break;

            case R.id.button_ok:

                Bundle result = new Bundle();

                result.putString(
                        EXTRA_KEY_PERIMETER,
                        Double.toString(areaCalculator.getPerimeter())
                );

                if (isCoordinatesReturnRequired) {
                    result.putString(
                            EXTRA_KEY_COORDINATES,
                            areaCalculator.getCoordinatesString()
                    );
                }
                if (isImageReturnRequired) {
                    result.putString(
                            EXTRA_KEY_IMAGE,
                            mapSnapshotPath
                    );
                }

                Intent data = new Intent();

                data.putExtra(INTENT_RESULT, Double.toString(areaCalculator.getArea()));

                data.putExtra(
                        EXTRA_KEY_RESPONSE_BUNDLE,
                        result
                );

                setResult(ResultCode.OK, data);
                finish();
                break;

            case R.id.button_redo:

                setResult(
                        ResultCode.REDO,
                        new Intent().putExtra(
                                EXTRA_KEY_IS_REDO,
                                true
                        )
                );
                finish();
                break;

            case R.id.button_settings:

                if (!settingsMenuAnimator.isRunning()) {
                    if (linearLayoutSettings.getAlpha() == 0) {
                        settingsMenuAnimator.start();
                    } else {
                        settingsMenuAnimator.reverse();
                    }
                }
                break;

            default:
                throw new RuntimeException("Unknown view id: " + view.getId());
        }

        LOG.trace("Exit");
    }

    @Override
    public void onConnected(Bundle bundle) {
        LOG.trace("Entry");
        requestLocationUpdates();
        updateProgressState();
        LOG.trace("Exit");
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient,
                    LocationRequest
                            .create()
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .setInterval(0)
                    ,
                    this
            );

            if (isRedo) {
                onLocationChanged(
                        LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
                );
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
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

        initializeParameters();

        super.onCreate(savedInstanceState);

        freezeOrientation();

        settingsMenuAnimator.setDuration(ANIMATION_DURATION_MS);
        settingsMenuAnimator.addUpdateListener(this);

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
    public void onImageSaved(String filePath) {
        LOG.trace("Entry, filePath={}", filePath);
        mapSnapshotPath = filePath;
        findViewById(R.id.button_ok).setEnabled(true);
        LOG.trace("Exit");
    }

    @Override
    public void onLocationChanged(Location location) {
        LOG.trace("Entry");

        if (location.getAccuracy() <= locationMinAccuracy) {
            LOG.debug("location.getAccuracy()={}", location.getAccuracy());

            boolean addLocation = previousLocation == null ||
                    (
                            isRecording &&
                                    location.distanceTo(previousLocation) >=
                                            location.getAccuracy() + previousLocation.getAccuracy()
                                    && location.getTime() - previousLocation.getTime() >=
                                    recordingIntervalMillis
                                    && location.distanceTo(previousLocation) >=
                                    recordingIntervalMeters
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


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap map) {
        LOG.trace("Entry");

        this.map = map;

        map.getUiSettings().setAllGesturesEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(true);

        if (checkForLocationPermissions()) {
            map.setMyLocationEnabled(true);
        }

        updateProgressState();
        LOG.trace("Exit");
    }

    private boolean checkForLocationPermissions() {
        if (isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                buildPermissionRequestDialog();
            } else {
                requestLocationPermissions();
            }
            return false;
        }
    }

    private boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void buildPermissionRequestDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.request_location_permission_dialog_title)
                .setMessage(R.string.request_location_permission_dialog_message)
                .setPositiveButton(R.string.ok, (dialog1, which) -> {
                    dialog1.dismiss();
                    requestLocationPermissions();
                }).show();
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE_LOCATION_PERMISSION);
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

        locationMinAccuracy = LOCATION_MIN_MIN_ACCURACY + progress;
        textViewLocationAccuracy.setText(
                getString(
                        R.string.location_accuracy_format,
                        locationMinAccuracy
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
    public void onSnapshotReady(Bitmap snapshot) {
        LOG.trace("Entry");

        new SaveImageAsyncTask(
                snapshot,
                Bitmap.CompressFormat.PNG,
                100,
                IMAGE_FILE_FOLDER,
                IMAGE_FILE_PREFIX,
                IMAGE_FILE_SUFFIX,
                this
        ).execute();

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
                ((TextView)findViewById(R.id.textview_progress)).setText(R.string.loading_gps);

            }

            if (googleApiClient.isConnected() &&
                    previousLocation != null) {

                findViewById(R.id.linearlayout_progressbar).setVisibility(View.GONE);
                findViewById(R.id.button_start).setEnabled(true);

            }
        }

        LOG.trace("Exit");
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i]) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (map != null) {
                        map.setMyLocationEnabled(true);
                    }
                    requestLocationUpdates();
                }
            }
        }
    }
}
