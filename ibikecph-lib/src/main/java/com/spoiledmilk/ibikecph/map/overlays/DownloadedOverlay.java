package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.format.DateUtils;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by kraen on 21-05-16.
 */
public abstract class DownloadedOverlay implements TogglableOverlay {

    protected String name;
    protected Paint paint;

    protected List<Overlay> overlays = new ArrayList<>();

    protected static final DateFormat HTTP_DATE =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    protected static final long EXPECTED_MODIFICATION_DELAY = 1000 * 60 * 60 * 24;

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
     * First a GET request is made with the If-Modified-Since header to identify if the remote file
     * has changed, if that's the case we download it again, if not - we simply reuse the local file
     * already downloaded.
     * @param context Used to access local files
     */
    public void load(Context context) throws IOException {
        load(context, false);
    }

    /**
     * Checks if the geojson file has changed on the server and downloads a new version if needed.
     * First a GET request is made with the If-Modified-Since header to identify if the remote file
     * has changed, if that's the case we download it again, if not - we simply reuse the local file
     * already downloaded.
     * @param context Used to access local files
     * @param forced Should the local file be deleted before requesting to the remote server?
     * @throws IOException
     */
    public void load(Context context, boolean forced) throws IOException {
        URL url = getURL();

        File file = new File(context.getFilesDir(), getFilename());

        // If the update is forced, lets delete the file.
        if(file.exists() && forced) {
            file.delete();
        }

        InputStream input = null;
        OutputStream output = null;

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // If the GeoJSON has already been downloaded, we add the if-modified-since header
            if(file.exists()) {
                long localLastModified = file.lastModified();
                String ifModifiedSince = HTTP_DATE.format(new Date(localLastModified - EXPECTED_MODIFICATION_DELAY));
                connection.setRequestProperty("If-Modified-Since", ifModifiedSince);
                Log.d("DownloadedOverlay", "Requested " + url + " if-modified-since: " + ifModifiedSince);
            } else {
                Log.d("DownloadedOverlay", "Requested " + url);
            }

            if(connection.getResponseCode() == 200) { // OK
                Log.d("DownloadedOverlay", "Local file didn't exist or remote file was modified!");

                // Download the content of the updated overlay
                input = connection.getInputStream();
                output = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int bytesWritten;
                while ((bytesWritten = input.read(buffer)) > 0) {
                    output.write(buffer, 0, bytesWritten);
                }
            } else if(connection.getResponseCode() == 304) { // Not Modified
                // Let's do nothing ...
                Log.d("DownloadedOverlay", "The remote file has not been modified.");
            } else {
                throw new RuntimeException("Unexpected response code: " + connection.getResponseCode());
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

        // Read directly from the local file
        try {
            Log.d("DownloadedOverlay", "Reading from local file " + file.getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readValue(file, JsonNode.class);

            parseGeoJson(rootNode);
        } catch (Exception e) {
            // Assuming that the file has been corrupted somehow.
            if(forced) {
                throw new RuntimeException("Retried forced but error loading overlay", e);
            } else {
                Log.e("DownloadedOverlay", "Error loading overlay: '" + e.getMessage() + "' trying again.");
                // And try again - forced
                load(context, true);
            }
        }
    }

    protected void parseGeoJson(JsonNode rootNode) {
        // Clear all overlays before adding new
        overlays.clear();
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
            paint.setStrokeWidth(8); // iOS is 4pt
            paint.setAlpha(170); // 66%
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);

            // Parse all the features as separate path overlays
            if (!rootNode.has("features") || !rootNode.get("features").isArray()) {
                throw new RuntimeException("Missing features[]");
            }

            for (JsonNode feature: rootNode.get("features")) {
                List<PathOverlay> featureOverlays = parseFeatureNode(feature);
                for(PathOverlay overlay: featureOverlays) {
                    overlay.setPaint(paint);
                    overlays.add(overlay);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing GeoJSON", e);
        }
    }

    protected List<PathOverlay> parseFeatureNode(JsonNode feature) {
        List<PathOverlay> overlays = new ArrayList<>();
        if (!feature.has("type") || !feature.get("type").asText().equals("Feature")) {
            throw new RuntimeException("Missing or unexpected feature[].type");
        }
        if (!feature.has("geometry") || !feature.get("geometry").isObject()) {
            throw new RuntimeException("Missing feature[].geometry");
        }
        JsonNode geometry = feature.get("geometry");
        if (geometry.has("type") && geometry.get("type").asText().equals("LineString")) {
            overlays.add(parseLineString(geometry));
        } else if (geometry.has("type") && geometry.get("type").asText().equals("MultiLineString")) {
            overlays.addAll(parseMultiLineString(geometry));
        } else {
            throw new RuntimeException("Missing or unexpected feature[].type");
        }
        return overlays;
    }

    protected PathOverlay parseLineString(JsonNode geometry) {
        if (!geometry.has("coordinates") || !geometry.get("coordinates").isArray()) {
            throw new RuntimeException("Missing or unexpected feature[].geometry.coordinates");
        }
        return parseCoordinates(geometry.get("coordinates"));
    }

    protected List<PathOverlay> parseMultiLineString(JsonNode geometry) {
        List<PathOverlay> overlays = new ArrayList<>();
        if (!geometry.has("coordinates") || !geometry.get("coordinates").isArray()) {
            throw new RuntimeException("Missing or unexpected feature[].geometry.coordinates");
        }
        for(JsonNode coordinates: geometry.get("coordinates")) {
            PathOverlay overlay = parseCoordinates(coordinates);
            overlays.add(overlay);
        }
        return overlays;
    }

    protected PathOverlay parseCoordinates(JsonNode coordinates) {
        PathOverlay overlay = new PathOverlay();
        for(JsonNode coordinate: coordinates) {
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
