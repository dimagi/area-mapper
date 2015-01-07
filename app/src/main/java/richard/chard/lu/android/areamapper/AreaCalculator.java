package richard.chard.lu.android.areamapper;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * @author Richard Lu
 */
public class AreaCalculator {

    public interface Listener {

        public void onAreaChange(LatLng latLng, double areaSqMeters);

    }

    public AreaCalculator(Listener listener) {
        this.listener = listener;
    }

    private ArrayList<LatLng> boundingLatLngs = new ArrayList<>();

    private Listener listener;

    public void addLatLng(LatLng latLng) {

        boundingLatLngs.add(latLng);

        listener.onAreaChange(latLng, getArea());

    }

    public double getArea() {
        return 0;
    }

}
