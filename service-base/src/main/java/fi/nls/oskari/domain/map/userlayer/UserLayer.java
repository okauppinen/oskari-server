package fi.nls.oskari.domain.map.userlayer;

import fi.nls.oskari.domain.map.JSONLocalizedName;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.domain.map.UserDataLayer;
import org.json.JSONArray;

public class UserLayer extends UserDataLayer {
    private static final String LOCALE_DESC = "desc";
    private static final String LOCALE_SOURCE = "source";

    private JSONArray fields;
    private int features_count;
    private int features_skipped; //if geojson feature doesn't have geometry object or it's null, feature is skipped
    private String wkt;

    @Override
    public final String getType() {
        return OskariLayer.TYPE_USERLAYER;
    }

    public JSONArray getFields() {
        return fields;
    }

    public void setFields(JSONArray fields) {
        if (fields == null) {
            this.fields = new JSONArray();
            return;
        }
        this.fields = fields;
    }

    public int getFeatures_count (){
        return features_count;
    }

    public void setFeatures_count (int count){
        this.features_count = count;
    }
    public int getFeatures_skipped (){
        return features_skipped;
    }

    public void setFeatures_skipped (int noGeometry){
        this.features_skipped = noGeometry;
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }


    public String getDesc(final String language) {
        return getLocalizedValue(language, LOCALE_DESC);
    }
    public void setDesc(final String language, final String desc) {
        setLocalizedValue(language, LOCALE_DESC, desc);
    }

    public String getSource(final String language) {
        return getLocalizedValue(language, LOCALE_SOURCE);
    }
    public void setSource(final String language, final String source) { setLocalizedValue(language, LOCALE_SOURCE, source); }


}
