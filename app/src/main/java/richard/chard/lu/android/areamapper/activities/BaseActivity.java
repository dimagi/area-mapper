package richard.chard.lu.android.areamapper.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import richard.chard.lu.android.areamapper.Logger;
import richard.chard.lu.android.areamapper.R;
import richard.chard.lu.android.areamapper.ResultCode;

public class BaseActivity extends ActionBarActivity {

    private static final Logger LOG = Logger.create(BaseActivity.class);

    private static final int REQUEST_CODE_AREA = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOG.trace("Entry, requestCode={}, resultCode={}",
                requestCode,
                resultCode);

        if (requestCode == REQUEST_CODE_AREA) {
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

                    startAreaMapperActivity();
                    break;

                default:
                    throw new RuntimeException("Unknown result code: "+resultCode);
            }
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
                ),
                REQUEST_CODE_AREA
        );

        LOG.trace("Exit");
    }
}
