package richard.chard.lu.android.areamapper;

import android.app.Application;

import com.google.android.gms.maps.MapsInitializer;

public class AreaMapperApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, null);
    }
}
