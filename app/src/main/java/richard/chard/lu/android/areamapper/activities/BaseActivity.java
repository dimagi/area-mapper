package richard.chard.lu.android.areamapper.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.MapsInitializer.Renderer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;

import richard.chard.lu.android.areamapper.Logger;
import richard.chard.lu.android.areamapper.ResultCode;

public class BaseActivity extends AppCompatActivity implements OnMapsSdkInitializedCallback {

    private static final Logger LOG = Logger.create(BaseActivity.class);

    private static final int REQUEST_CODE_AREA = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOG.trace("Entry, requestCode={}, resultCode={}",
                requestCode,
                resultCode);

        switch (requestCode) {
            case REQUEST_CODE_AREA:

                switch (resultCode) {
                    case ResultCode.CANCEL:

                        setResult(ResultCode.CANCEL);
                        finish();
                        break;

                    case ResultCode.OK:

                        setResult(ResultCode.OK, data);
                        finish();
                        break;

                    case ResultCode.REDO:

                        getIntent().putExtras(data);
                        startAreaMapperActivity();
                        break;

                    case ResultCode.ERROR:

                        setResult(ResultCode.ERROR);
                        finish();
                        break;

                    default:
                        throw new RuntimeException("Unknown result code: " + resultCode);
                }
                break;

            default:
                throw new RuntimeException("Unknown request code: " + requestCode);
        }

        LOG.trace("Exit");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOG.trace("Entry");
        super.onCreate(savedInstanceState);

        startAreaMapperActivity();
        LOG.trace("Exit");
    }


    protected void startAreaMapperActivity() {
        LOG.trace("Entry");

        startActivityForResult(
                new Intent(
                        this,
                        AreaMapperActivity.class
                ).putExtras(
                        getIntent()
                ),
                REQUEST_CODE_AREA
        );

        LOG.trace("Exit");
    }

    @Override
    public void onMapsSdkInitialized(@NonNull Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d("MapsRenderer", "The latest version of the renderer is used.");
                break;
            case LEGACY:
                Log.d("MapsRenderer", "The legacy version of the renderer is used.");
                break;
        }
    }
}
