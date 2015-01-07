package richard.chard.lu.android.areamapper.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import richard.chard.lu.android.areamapper.R;
import richard.chard.lu.android.areamapper.ResultCode;

public class ModeSelectionActivity extends ActionBarActivity
    implements View.OnClickListener {

    private static final int REQUEST_CODE_AREA = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                    break;

                default:
                    throw new RuntimeException("Unknown result code: "+resultCode);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_cancel:

                setResult(ResultCode.CANCEL);
                finish();
                break;

            case R.id.button_walk:

                startActivityForResult(
                        new Intent(
                                this,
                                AreaMapperActivity.class
                        ).putExtra(
                                AreaMapperActivity.EXTRA_KEY_MODE,
                                AreaMapperActivity.MODE_WALK
                        ),
                        REQUEST_CODE_AREA
                );
                break;

            case R.id.button_draw:

                startActivityForResult(
                        new Intent(
                                this,
                                AreaMapperActivity.class
                        ).putExtra(
                                AreaMapperActivity.EXTRA_KEY_MODE,
                                AreaMapperActivity.MODE_DRAW
                        ),
                        REQUEST_CODE_AREA
                );
                break;

            default:
                throw new RuntimeException("Unknown view id: "+view.getId());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mode_selection);

        findViewById(R.id.button_cancel).setOnClickListener(this);
        findViewById(R.id.button_walk).setOnClickListener(this);
        findViewById(R.id.button_draw).setOnClickListener(this);
    }
}
