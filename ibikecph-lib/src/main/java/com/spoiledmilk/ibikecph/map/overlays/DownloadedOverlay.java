package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Overlay;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.spoiledmilk.ibikecph.IBikeApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kraen on 21-05-16.
 */
public abstract class DownloadedOverlay implements TogglableOverlay {

    protected String name;
    protected Paint paint;

    protected List<Overlay> overlays = new ArrayList<>();

    public DownloadedOverlay() {
        // Check the existence of the downloaded geojson data
        // Load the geojson into the overlays list
    }

    @Override
    public List<Overlay> getOverlays() {
        return overlays;
    }

    /**
     * Checks if the geojson file has changed on the server and downloads a new version if needed.
     * First a HEAD request is made to identify if the remote file has changed, if that's the case
     * we download it again, if not - we simply reuse the local file already downloaded.
     * @param context
     */
    public void load(Context context) throws IOException {
        boolean shouldDownload = false;

        URL url = getURL();

        // Check if the geojson file has already been downloaded
        File file = new File(context.getFilesDir(), getFilename());
        if(file.exists()) {
            long localLastModified = file.lastModified();
            Log.d("DownloadedOverlay", "Local file last modified: " + localLastModified);
            // Check the server's modified date
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            long remoteLastModified = connection.getHeaderFieldDate("Last-Modified", localLastModified + 1000);
            Log.d("DownloadedOverlay", "Remote file last modified: " + remoteLastModified);
            if (remoteLastModified > localLastModified) {
                shouldDownload = true;
            }
        } else {
            shouldDownload = true;
        }

        if(shouldDownload) {
            Log.d("DownloadedOverlay", "Downloading " + url);

            InputStream input = null;
            OutputStream output = null;

            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                input = connection.getInputStream();
                output = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int bytesWritten;
                while((bytesWritten = input.read(buffer)) > 0) {
                    output.write(buffer, 0, bytesWritten);
                }

                connection.disconnect();
            } finally {
                if(input != null) {
                    input.close();
                }
                if(output != null) {
                    output.close();
                }
            }

        }

        // Read directly from the local file
        Log.d("DownloadedOverlay", "Reading from local file " + file.getAbsolutePath());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(file, JsonNode.class);
        Log.d("DownloadedOverlay", "Loaded " + rootNode);

        parseGeoJson(rootNode);
    }

    protected void parseGeoJson(JsonNode rootNode) {
        try {
            if (!rootNode.has("type") || !rootNode.get("type").asText().equals("FeatureCollection")) {
                throw new RuntimeException("Missing or unexpected type");
            }

            if (!rootNode.has("properties") || !rootNode.get("properties").isObject()) {
                throw new RuntimeException("Missing properties");
            }
            JsonNode properties = rootNode.get("properties");

            // Read the name of the overlay
            if (!properties.has("name") || !properties.get("name").isObject()) {
                throw new RuntimeException("Missing properties.name");
            }
            String languageCode = IBikeApplication.getLanguageString();
            JsonNode name = properties.get("name");
            // Read either the danish or english name
            if (!name.has(languageCode) || !name.get(languageCode).isTextual()) {
                throw new RuntimeException("Missing properties.name." + languageCode);
            }
            this.name = name.get(languageCode).asText();

            // Read the color of the overlay
            if (!properties.has("color") || !properties.get("color").isTextual()) {
                throw new RuntimeException("Missing properties.name");
            }
            String colorString = properties.get("color").asText();
            int color = Color.parseColor(colorString);

            paint = new Paint();
            paint.setColor(color);
            paint.setAntiAlias(true);
            paint.setStrokeWidth(20);
            paint.setAlpha(170); // 66%
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);

            // Parse all the features as separate path overlays
            if (!rootNode.has("features") || !rootNode.get("features").isArray()) {
                throw new RuntimeException("Missing features[]");
            }

            for (JsonNode feature: rootNode.get("features")) {
                PathOverlay overlay = parseFeatureNode(feature);
                overlay.setPaint(paint);
                overlays.add(overlay);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing GeoJSON: " + e.getMessage());
        }
    }

    protected PathOverlay parseFeatureNode(JsonNode feature) {
        PathOverlay overlay = new PathOverlay();
        if (!feature.has("type") || !feature.get("type").asText().equals("Feature")) {
            throw new RuntimeException("Missing or unexpected feature[].type");
        }
        if (!feature.has("geometry") || !feature.get("geometry").isObject()) {
            throw new RuntimeException("Missing feature[].geometry");
        }
        JsonNode geometry = feature.get("geometry");
        if (!geometry.has("type") || !geometry.get("type").asText().equals("LineString")) {
            throw new RuntimeException("Missing or unexpected feature[].type");
        }
        if (!geometry.has("coordinates") || !geometry.get("coordinates").isArray()) {
            throw new RuntimeException("Missing or unexpected feature[].geometry.coordinates");
        }
        for(JsonNode coordinate: geometry.get("coordinates")) {
            if(coordinate.isArray() && coordinate.size() >= 2) {
                // Coordinates are ordered longitude, latitude(, altitude)
                // See http://geojson.org/geojson-spec.html#positions
                double longitude = coordinate.get(0).asDouble();
                double latitude = coordinate.get(1).asDouble();
                LatLng point = new LatLng(latitude, longitude);
                overlay.addPoint(point);
            } else {
                throw new RuntimeException("Unexpected feature[].geometry.coordinates[]");
            }
        }
        return overlay;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Paint getPaint() {
        return paint;
    }

    /**
     * The remote URL of the overlay GeoJSON data file.
     * @return
     */
    protected URL getURL() {
        try {
            return new URL("http://assets.ibikecph.dk/geodata/" + getFilename());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The remote and local filename of the downloadable overlay.
     * @return
     */
    abstract String getFilename();

    @Override
    public boolean isSelected() {
        return TogglableOverlayFactory.getInstance().isSelected(this);
    }

    @Override
    public void setSelected(boolean selected) {
        TogglableOverlayFactory.getInstance().setSelected(this, selected);
    }

}
