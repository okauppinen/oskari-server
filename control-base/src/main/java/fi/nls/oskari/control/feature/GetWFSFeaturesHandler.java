package fi.nls.oskari.control.feature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.oskari.service.user.UserLayerService;
import org.oskari.service.wfs.client.OskariWFSClient;

import com.vividsolutions.jts.geom.Envelope;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionConstants;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.service.ServiceRuntimeException;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetWFSFeatures")
public class GetWFSFeaturesHandler extends AbstractWFSFeaturesHandler {

    protected static final String ERR_BBOX_INVALID = "Invalid bbox";
    protected static final String ERR_BBOX_OUT_OF_CRS = "bbox not within CRS extent";
    protected static final String ERR_GEOJSON_ENCODE_FAIL = "Failed to write GeoJSON";
    protected static final String ERR_FAILED_TO_RETRIEVE_FEATURES = "Failed to retrieve features";

    private static final String PARAM_BBOX = "bbox";

    private static final String GEOJSON_CONTENT_TYPE = "application/vnd.geo+json";
    private static final byte[] EMPTY_GEOJSON_FEATURE_COLLECTION =
            "{\"type\": \"FeatureCollection\", \"features\": []}".getBytes(StandardCharsets.UTF_8);

    // For WGS84: 11.132mm precision at equator, more precise elsewhere, max error 5.5mm
    private static final int NUM_DECIMAL_PLACES_DEGREE = 7;
    // For metric projections: 10mm precision, max error 5mm
    private static final int NUM_DECIMAL_PLACES_OTHER = 2;

    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        String id = params.getRequiredParam(ActionConstants.PARAM_ID);
        String bboxStr = params.getRequiredParam(PARAM_BBOX);

        Optional<UserLayerService> contentProcessor = getUserContentProsessor(id);
        OskariLayer layer = findLayer(id, params.getUser(), contentProcessor);
        String uuid = params.getUser().getUuid();

        String targetSRS = params.getHttpParam(ActionConstants.PARAM_SRS, "EPSG:3857");
        CoordinateReferenceSystem targetCRS;
        try {
            targetCRS = CRS.decode(targetSRS);
        } catch (Exception e) {
            throw new ActionParamsException("Invalid " + ActionConstants.PARAM_SRS);
        }

        // TODO: Figure out if layer supports targetSrsName
        // If it does let the WFS service do the transformation
        ReferencedEnvelope bbox = parseBbox(bboxStr, targetCRS);

        SimpleFeatureCollection fc;
        try {
            fc = featureClient.getFeatures(id, uuid, layer, bbox, targetCRS, contentProcessor);
        } catch (ServiceRuntimeException e) {
            // ActionParamsException because we don't want to log stacktrace of these  
            throw new ActionParamsException(ERR_FAILED_TO_RETRIEVE_FEATURES, e);
        }

        if (fc.isEmpty()) {
            ResponseHelper.writeResponse(params, 200,
                    GEOJSON_CONTENT_TYPE, EMPTY_GEOJSON_FEATURE_COLLECTION);
            return;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int decimals = getNumDecimals(targetCRS);
            new FeatureJSON(new GeometryJSON(decimals)).writeFeatureCollection(fc, baos);
            ResponseHelper.writeResponse(params, 200, GEOJSON_CONTENT_TYPE, baos);
        } catch (IOException e) {
            ResponseHelper.writeError(params, ERR_GEOJSON_ENCODE_FAIL);
        }
    }

    @Override
    protected OskariWFSClient createWFSClient() {
        // Override the default functionality, return non-caching OskariWFSClient
        return new OskariWFSClient();
    }

    protected ReferencedEnvelope parseBbox(String bbox, CoordinateReferenceSystem crs)
            throws ActionParamsException {
        String[] a = bbox.split(",", 4);
        if (a.length != 4) {
            throw new ActionParamsException(ERR_BBOX_INVALID);
        }
        try {
            double x1 = Double.parseDouble(a[0]);
            double y1 = Double.parseDouble(a[1]);
            double x2 = Double.parseDouble(a[2]);
            double y2 = Double.parseDouble(a[3]);
            Envelope envelope = new Envelope(x1, x2, y1, y2);
            if (!featureClient.isWithin(crs, envelope)) {
                throw new ActionParamsException(ERR_BBOX_OUT_OF_CRS);
            }
            return new ReferencedEnvelope(envelope, crs);
        } catch (NumberFormatException e) {
            throw new ActionParamsException(ERR_BBOX_INVALID);
        }
    }

    /**
     * Get number of decimal places to use (maximum) when writing out the GeoJSON response.
     * The goal is to reduce the size of the actual response thereby reducing the amount
     * of memory and network used to serve the response while maintaining a precision that
     * still far exceedes the needs for our purposes
     *
     * @returns number of decimal places to use, the number depends on the unit of measure
     * of the axes of the coordinate system:
     * - NUM_DECIMAL_PLACES_DEGREE for degrees
     * - NUM_DECIMAL_PLACES_OTHER for others (metres, feet, what have you)
     */
    private int getNumDecimals(CoordinateReferenceSystem crs) {
        boolean degrees = "°".equals(crs.getCoordinateSystem().getAxis(0).getUnit().toString());
        return degrees ? NUM_DECIMAL_PLACES_DEGREE : NUM_DECIMAL_PLACES_OTHER;
    }

}