package org.oskari.myplaces.service;

import fi.nls.oskari.control.ActionConstants;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.MyPlace;
import fi.nls.oskari.domain.map.MyPlaceCategory;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.domain.map.wfs.WFSLayerOptions;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceMybatisImpl;
import fi.nls.oskari.map.layer.formatters.LayerJSONFormatterMYPLACES;
import fi.nls.oskari.map.style.VectorStyleService;
import fi.nls.oskari.service.OskariComponent;
import fi.nls.oskari.service.OskariComponentManager;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.PropertyUtil;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.oskari.geojson.GeoJSON;
import org.oskari.permissions.model.Resource;

import java.util.List;
import java.util.stream.Collectors;

import static fi.nls.oskari.map.geometry.ProjectionHelper.getSRID;

/* For wfs-t */
public abstract class MyPlacesService extends OskariComponent {

    public static final String PERMISSION_TYPE_DRAW = "DRAW";
    public static final String MYPLACES_LAYERID_PREFIX = "myplaces_";

    private String MYPLACES_CLIENT_WMS_URL = PropertyUtil.getOptional("myplaces.client.wmsurl");
    private static final String MYPLACES_BASELAYER_ID = "myplaces.baselayer.id";
    private static final int BASE_WFS_LAYER_ID = PropertyUtil.getOptional(MYPLACES_BASELAYER_ID, -1);
    private static final OskariLayerService mapLayerService = new OskariLayerServiceMybatisImpl();

    private static final LayerJSONFormatterMYPLACES FORMATTER = new LayerJSONFormatterMYPLACES();
    private static final Logger LOGGER = LogFactory.getLogger(MyPlacesService.class);

    public abstract List<MyPlaceCategory> getCategories();

    public abstract MyPlaceCategory findCategory(long id);

    public abstract List<MyPlaceCategory> getMyPlaceLayersById(List<Long> idList);

    public abstract int updatePublisherName(final long id, final String uuid, final String name);


    public abstract boolean canInsert(final User user, final long categoryId);

    public abstract boolean canModifyPlace(final User user, final long placeId);

    public abstract boolean canModifyCategory(final User user, final long categoryId);

    public abstract boolean canModifyCategory(final User user, final String layerId);

    public abstract Resource getResource(final long categoryId);

    public abstract Resource getResource(final String myplacesLayerId);

    public abstract void deleteByUid(final String uid);

    public MyPlacesService() {
        // default 'myplaces.client.wmsurl' to ajax url for tiles if not configured
        if (MYPLACES_CLIENT_WMS_URL == null) {
            // action_route name points to fi.nls.oskari.control.myplaces.MyPlacesTileHandler
            MYPLACES_CLIENT_WMS_URL = PropertyUtil.get("oskari.ajax.url.prefix") + "action_route=MyPlacesTile&myCat=";
        }
    }
    /**
     * Returns the base WFS-layer for myplaces
     */
    public static OskariLayer getBaseLayer() {
        if (BASE_WFS_LAYER_ID == -1) {
            LOGGER.error("Myplaces baseId not defined. Please define", MYPLACES_BASELAYER_ID,
                    "property with value pointing to the baselayer in database.");
            return null;
        }
        return mapLayerService.find(BASE_WFS_LAYER_ID);
    }
    public static MyPlaceCategory createDefaultCategory() {
        MyPlaceCategory category = new MyPlaceCategory();
        category.setName("");
        category.setDefault(true);
        category.setLocale(new JSONObject());
        VectorStyleService vss = OskariComponentManager.getComponentOfType(VectorStyleService.class);
        category.getWFSLayerOptions().setDefaultFeatureStyle(vss.getDefaultFeatureStyle());
        return category;
    }


    public static JSONObject parseLayerToJSON (final MyPlaceCategory mpLayer, final String srs) {
        return FORMATTER.getJSON(getBaseLayer(), mpLayer, srs, PropertyUtil.getDefaultLanguage());
    }

    public static JSONObject parseLayerToJSON (final MyPlaceCategory mpLayer, final String srs, final String lang) {
        return FORMATTER.getJSON(getBaseLayer(), mpLayer, srs, lang);
    }

    private Geometry transformGeometry(Geometry geometry, String sourceSRSName, String targetSRSName) {
        try {
            CoordinateReferenceSystem targetCRS, sourceCRS;
            MathTransform transform;

            try {
                targetCRS = CRS.decode(targetSRSName);
                sourceCRS = CRS.decode(sourceSRSName);
                transform = CRS.findMathTransform(sourceCRS, targetCRS);
            } catch (Exception e) {
                throw new ActionParamsException("Invalid " + ActionConstants.PARAM_SRS);
            }
            Geometry transformed = JTS.transform(geometry, transform);
            transformed.setSRID(getSRID(targetSRSName));
            return transformed;

        } catch(Exception e) {
            LOGGER.warn(e, "Exception transforming geometry");
        }
        return null;
    }
    private JSONObject toGeoJSONFeatureCollection(List<MyPlace> places, String targetSRSName) throws ServiceException {
        if (places == null || places.isEmpty()) {
            return null;
        }
        JSONObject json = new JSONObject();
        try {
            json.put(GeoJSON.TYPE, GeoJSON.FEATURE_COLLECTION);
            // json.put("crs", geojsonWriter.writeCRSObject(targetSRSName));

            JSONArray features = new JSONArray(places.stream().map(place -> this.toGeoJSONFeature(place, targetSRSName)).collect(Collectors.toList()));
            json.put(GeoJSON.FEATURES, features);

        } catch(JSONException ex) {
            LOGGER.warn("Failed to create GeoJSON FeatureCollection");
            throw new ServiceException("Failed to create GeoJSON FeatureCollection");
        }
        return json;
    }

    private JSONObject toGeoJSONFeature(MyPlace place, String targetSRSName) {
        JSONObject feature = new JSONObject();
        JSONObject properties = new JSONObject();
        try {
            feature.put("id", place.getId());
            feature.put("geometry_name", GeoJSON.GEOMETRY);
            feature.put(GeoJSON.TYPE, GeoJSON.FEATURE);

            feature.put(GeoJSON.GEOMETRY, place.getGeometry());

            properties.put("attention_text", place.getAttentionText());
            properties.put("category_id", place.getCategoryId());
            properties.put("created", place.getCreated());
            properties.put("image_url", place.getImageUrl());
            properties.put("link", place.getLink());
            properties.put("name", place.getName());
            properties.put("place_desc", place.getDesc());
            properties.put("updated", place.getUpdated());
            feature.put("properties", properties);

        } catch(JSONException ex) {
            LOGGER.warn("Failed to convert MyPlace to GeoJSONFeature");
        }

        return feature;
    }


}
