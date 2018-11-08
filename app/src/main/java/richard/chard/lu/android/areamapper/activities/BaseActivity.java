package richard.chard.lu.android.areamapper.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


import org.acra.ACRA;
import org.acra.ACRAConfiguration;

import java.io.IOException;
import java.util.Properties;

import richard.chard.lu.android.areamapper.FileUtil;
import richard.chard.lu.android.areamapper.Logger;
import richard.chard.lu.android.areamapper.ResultCode;

public class BaseActivity extends AppCompatActivity {

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
                        throw new RuntimeException("Unknown result code: "+resultCode);
                }
                break;

            default:
                throw new RuntimeException("Unknown request code: "+requestCode);
        }

        LOG.trace("Exit");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOG.trace("Entry");

        super.onCreate(savedInstanceState);

        ACRA.init(this.getApplication(), initACRA());

        startAreaMapperActivity();

        LOG.trace("Exit");
    }    public ACRAConfiguration initACRA(){
        try {
            Properties properties = FileUtil.loadProperties(this.getBaseContext());
            ACRAConfiguration mAcraConfig = new ACRAConfiguration();
            mAcraConfig.setFormUriBasicAuthLogin(properties.getProperty("ACRA_USER"));
            mAcraConfig.setFormUriBasicAuthPassword(properties.getProperty("ACRA_PASSWORD"));
            mAcraConfig.setFormUri(properties.getProperty("ACRA_URL"));

            System.out.println("user: " + properties.getProperty("ACRA_USER"));

            mAcraConfig.setReportType(org.acra.sender.HttpSender.Type.JSON);
            mAcraConfig.setHttpMethod(org.acra.sender.HttpSender.Method.PUT);

            return mAcraConfig;
        } catch (IOException e){
            LOG.trace("Couldn't load ACRA credentials.");
        }
        return null;
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
}
