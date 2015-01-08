package richard.chard.lu.android.areamapper;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

/**
 * @author Richard Lu
 */
public class AreaCalculator {

    public interface Listener {

        public void onAreaChange(LatLng latLng, double areaSqMeters);

    }

    private static final Logger LOG = Logger.create(AreaCalculator.class);

    public AreaCalculator(Listener listener) {
        this.listener = listener;
    }

    private ArrayList<LatLng> boundingLatLngs = new ArrayList<>();

    private Listener listener;

    public void addLatLng(LatLng latLng) {
        LOG.trace("Entry");

        if (boundingLatLngs.size() == 0) {
            boundingLatLngs.add(latLng);
        }

        boundingLatLngs.add(
                boundingLatLngs.size() - 1,
                latLng
        );

        listener.onAreaChange(latLng, getArea());

        LOG.trace("Exit");
    }

    public double getArea() {
        if (boundingLatLngs.size() > 3) {
            return SphericalUtil.computeArea(boundingLatLngs);
        } else {
            return 0;
        }
    }

    public PolygonOptions getPolygonOptions() {
        return new PolygonOptions()
                .addAll(boundingLatLngs)
                .strokeColor(R.color.lightblue_900)
                .fillColor(R.color.cyan_100_transparent);
    }

}
