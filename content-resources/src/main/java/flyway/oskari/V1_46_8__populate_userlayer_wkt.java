package flyway.oskari;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import fi.nls.oskari.map.geometry.WKTHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
/*
 * 
 */
public class V1_46_8__populate_userlayer_wkt implements JdbcMigration {
    private static final Logger LOG = LogFactory.getLogger(V1_46_8__populate_userlayer_wkt.class);
    private static final String WGS84_SRS = "EPSG:4326";
    private static final String PROP_GEOMETRY = PropertyUtil.get("userlayer.geometry.name", "geometry");
    private String srsName = null;
    
    public void migrate(Connection connection) throws SQLException {
        //if srs is not found from properties, tries to find from oskari_maplayer
        srsName = PropertyUtil.get("oskari.native.srs", getSrsName (connection));
        if (srsName == null){
            //quit
            LOG.error("Cannot get srs name for userlayer data");
        }
        // add wkt column to user_layer table
        addColumn(connection);
        List <Long> ids = getUserlayerIds(connection);
        int count = 0;
        for (long id : ids){
            String wkt = getWkt(connection, id);
            updateWkt (connection, id, wkt);
            count++;
        }
        LOG.info("Calculated coverage information for", count , "userlayers");
        
    }
    private void addColumn(Connection conn) throws SQLException {
        final String sql = "ALTER TABLE user_layer ADD COLUMN wkt VARCHAR(512) DEFAULT ''::varchar";

        try (final PreparedStatement statement =
                     conn.prepareStatement(sql)) {
            statement.execute();
        }
    }
    private String getSrsName (Connection conn) throws SQLException {
        int baselayerId = PropertyUtil.getOptional("userlayer.baselayer.id", -1);
        String sql = "SELECT srs_name FROM oskari_maplayer WHERE id=?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, baselayerId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()){
                    return rs.getString("srs_name");  
                }
                return null;
            }
        }
    }
    private List<Long> getUserlayerIds (Connection conn) throws SQLException{
        final String sql = "SELECT id FROM user_layer";
        List <Long> ids = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()){
                    ids.add(rs.getLong("id"));
                }
            }
        }
        return ids;
    }
    private void updateWkt (Connection conn, long layerId, String wkt) throws SQLException {
        final String sql = "UPDATE user_layer SET wkt=? WHERE id=?";
        
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, wkt);
            statement.setLong(2, layerId);
            statement.execute();
        }
    }
    private String getWkt (Connection conn, long layerId) throws SQLException{
        final String sql = "SELECT ST_AsText(ST_Extent ("+PROP_GEOMETRY+")) AS extent FROM user_layer_data WHERE user_layer_id=?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setLong(1, layerId);
            try (ResultSet rs = statement.executeQuery()) {
                //transform to wgs84
                String wkt = "";
                if (rs.next()){
                    wkt = WKTHelper.transform(rs.getString("extent"), srsName, WGS84_SRS);
                }
                return wkt;
            }
        }
    }    
}