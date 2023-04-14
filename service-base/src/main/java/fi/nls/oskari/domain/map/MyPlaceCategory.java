package fi.nls.oskari.domain.map;

public class MyPlaceCategory extends UserDataLayer {

    private boolean isDefault;

    @Override
    public final String getType() {
        return OskariLayer.TYPE_MYPLACES;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

}
