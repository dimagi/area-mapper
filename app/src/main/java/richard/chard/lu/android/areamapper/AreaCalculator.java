package richard.chard.lu.android.areamapper;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

/**
 * @author Richard Lu
 */
public class AreaCalculator {

    public interface Listener {

        public Polygon getPolygon(PolygonOptions polygonOptions);

        public void onAreaChange(LatLng latLng, double areaSqMeters);

    }

    private static final Logger LOG = Logger.create(AreaCalculator.class);

    public AreaCalculator(Listener listener) {
        this.listener = listener;
    }

    private ArrayList<LatLng> boundingLatLngs = new ArrayList<>();

    private Listener listener;

    private Polygon polygon;

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

        if (polygon == null) {
            polygon = listener.getPolygon(
                    new PolygonOptions()
                            .addAll(boundingLatLngs)
            );
        } else {
            polygon.setPoints(boundingLatLngs);
        }

        LOG.trace("Exit");
    }

    public ArrayAdapter getArrayAdapter(Context context, int layoutId) {
        return new ArrayAdapter<>(
                context,
                layoutId,
                boundingLatLngs
        );
    }

    public double getArea() {
        if (boundingLatLngs.size() > 3) {
            return SphericalUtil.computeArea(boundingLatLngs);
        } else {
            return 0;
        }
    }

}
