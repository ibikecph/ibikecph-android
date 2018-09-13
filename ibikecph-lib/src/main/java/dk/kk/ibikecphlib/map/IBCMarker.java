package dk.kk.ibikecphlib.map;

import android.graphics.PointF;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;

/**
 * Created by jens on 7/20/15.
 */
public class IBCMarker extends Marker {

    private MarkerType type;

    public IBCMarker(String title, String description, LatLng latLng, MarkerType type) {
        super(title, description, latLng);
        this.type = type;
    }

    public MarkerType getType() {
        return type;
    }

    @Override
    public IBCMarker setIcon(Icon i) {
        IBCMarker m = (IBCMarker) super.setIcon(i);

        if (this.type == MarkerType.ADDRESS || this.type == MarkerType.PATH_ENDPOINT) {
            // Make sure the anchor fits the graphic.
            this.setAnchor(new PointF(0.5f, 1.7f));
        }

        return m;
    }
}
