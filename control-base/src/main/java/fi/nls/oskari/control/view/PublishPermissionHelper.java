package fi.nls.oskari.control.view;

import fi.nls.oskari.analysis.AnalysisHelper;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.MyPlaceCategory;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.domain.map.analysis.Analysis;
import fi.nls.oskari.domain.map.userlayer.UserLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.analysis.service.AnalysisDbService;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.myplaces.MyPlacesService;
import fi.nls.oskari.service.OskariComponentManager;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.util.ConversionHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.oskari.map.userlayer.service.UserLayerDbService;
import org.oskari.permissions.PermissionService;
import org.oskari.permissions.model.*;
import org.oskari.service.util.ServiceFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by SMAKINEN on 17.8.2015.
 */
public class PublishPermissionHelper {

    private static final Logger LOG = LogFactory.getLogger(PublishPermissionHelper.class);

    private MyPlacesService myPlaceService = null;
    private AnalysisDbService analysisService = null;
    private UserLayerDbService userLayerService = null;
    private OskariLayerService layerService = null;
    private PermissionService permissionsService = null;

    private static final String PREFIX_MYPLACES = "myplaces_";
    private static final String PREFIX_ANALYSIS = "analysis_";
    private static final String PREFIX_USERLAYER = "userlayer_";

    public void init() {
        if (myPlaceService == null) {
            setMyPlacesService(OskariComponentManager.getComponentOfType(MyPlacesService.class));
        }

        if (analysisService == null) {
            setAnalysisService(OskariComponentManager.getComponentOfType(AnalysisDbService.class));
        }

        if (userLayerService == null) {
            setUserLayerService(OskariComponentManager.getComponentOfType(UserLayerDbService.class));
        }

        if (permissionsService == null) {
            setPermissionsService(OskariComponentManager.getComponentOfType(PermissionService.class));
        }

        if (layerService == null) {
            setOskariLayerService(ServiceFactory.getMapLayerService());
        }
    }

    public void setMyPlacesService(final MyPlacesService service) {
        myPlaceService = service;
    }

    public void setAnalysisService(final AnalysisDbService service) {
        analysisService = service;
    }

    public void setUserLayerService(final UserLayerDbService service) {
        userLayerService = service;
    }

    public void setPermissionsService(final PermissionService service) {
        permissionsService = service;
    }
    public void setOskariLayerService(final OskariLayerService service) {
        layerService = service;
    }

    public void setupDrawPermission(final String drawLayerId, final User user) throws ActionException {
        if(!myPlaceService.canModifyCategory(user, drawLayerId)) {
            throw new ActionDeniedException("Trying to publish another users layer as drawlayer!");
        }
        Resource resource = myPlaceService.getResource(drawLayerId);
        if(resource.hasPermission(user, myPlaceService.PERMISSION_TYPE_DRAW)) {
            // clear up any previous DRAW permissions
            resource.removePermissionsFromAllUsers(myPlaceService.PERMISSION_TYPE_DRAW);
        }
        try {
            // add DRAW permission for all roles currently in the system
            for(Role role: UserService.getInstance().getRoles()) {
                final Permission perm = new Permission();
                perm.setRoleId((int) role.getId());
                perm.setType(myPlaceService.PERMISSION_TYPE_DRAW);
                resource.addPermission(perm);
            }
        } catch (Exception e) {
            LOG.error(e, "Error generating DRAW permissions for myplaces layer");
        }
        permissionsService.saveResource(resource);
    }


    JSONArray getPublishableLayers(final JSONArray selectedLayers, final User user) throws ActionException {
        if(selectedLayers == null || user == null) {
            throw new ActionParamsException("Could not get selected layers");
        }
        final JSONArray filteredList = new JSONArray();
        LOG.debug("Selected layers:", selectedLayers);

        String userUuid = user.getUuid();
        try {
            for (int i = 0; i < selectedLayers.length(); ++i) {
                JSONObject layer = selectedLayers.getJSONObject(i);
                final String layerId = layer.getString("id");
                if (layerId.startsWith(PREFIX_MYPLACES)) {
                    // check publish right for published myplaces layer
                    if (hasRightToPublishMyPlaceLayer(layerId, userUuid, user.getScreenname())) {
                        filteredList.put(layer);
                    }
                } else if (layerId.startsWith(PREFIX_ANALYSIS)) {
                    // check publish right for published analysis layer
                    if (hasRightToPublishAnalysisLayer(layerId, user)) {
                        filteredList.put(layer);
                    }
                } else if (layerId.startsWith(PREFIX_USERLAYER)) {
                    // check publish rights for user layer
                    if (hasRightToPublishUserLayer(layerId, user)) {
                        filteredList.put(layer);
                    }
                } else if (hasRightToPublishLayer(layerId, user)) {
                    // check publish right for normal layer
                    filteredList.put(layer);
                }
            }
        } catch (Exception e) {
            LOG.error(e, "Error parsing myplaces layers from published layers", selectedLayers);
        }
        LOG.debug("Filtered layers:", filteredList);
        return filteredList;
    }

    private boolean hasRightToPublishMyPlaceLayer(final String layerId, final String userUuid, final String publisherName) {
        final long categoryId = ConversionHelper.getLong(layerId.substring(PREFIX_MYPLACES.length()), -1);
        if (categoryId == -1) {
            LOG.warn("Error parsing layerId:", layerId);
            return false;
        }
        final List<Long> publishedMyPlaces = new ArrayList<Long>();
        publishedMyPlaces.add(categoryId);
        final List<MyPlaceCategory> myPlacesLayers = myPlaceService.getMyPlaceLayersById(publishedMyPlaces);
        for (MyPlaceCategory place : myPlacesLayers) {
            if (place.isOwnedBy(userUuid)) {
                myPlaceService.updatePublisherName(categoryId, userUuid, publisherName); // make it public
                return true;
            }
        }
        LOG.warn("Found my places layer in selected that isn't users own or isn't published any more! LayerId:", layerId, "User UUID:", userUuid);
        return false;
    }


    private boolean hasRightToPublishAnalysisLayer(final String layerId, final User user) {
        final long analysisId = AnalysisHelper.getAnalysisIdFromLayerId(layerId);
        if(analysisId == -1) {
            LOG.warn("Error parsing layerId:", layerId);
            return false;
        }
        final Analysis analysis = analysisService.getAnalysisById(analysisId);
        if (analysis.isOwnedBy(user.getUuid())) {
            // write publisher name for analysis
            analysisService.updatePublisherName(analysisId, user.getUuid(), user.getScreenname());
            return true;
        }
        LOG.warn("Found analysis layer in selected that isn't users own! LayerId:", layerId, "User UUID:", user.getUuid(), "Analysis UUID:", analysis.getUuid());
        return false;
    }

    private boolean hasRightToPublishUserLayer(final String layerId, final User user) {
        final long id = ConversionHelper.getLong(layerId.substring(PREFIX_USERLAYER.length()), -1);
        if (id == -1) {
            LOG.warn("Error parsing layerId:", layerId);
            return false;
        }
        final UserLayer userLayer = userLayerService.getUserLayerById(id);
        if (userLayer.isOwnedBy(user.getUuid())) {
            userLayerService.updatePublisherName(id, user.getUuid(), user.getScreenname());
            return true;
        } else {
            return false;
        }
    }

    private boolean hasRightToPublishLayer(final String layerId, final User user) {
        // layerId might be external so don't use it straight up
        int id = ConversionHelper.getInt(layerId, -1);
        if (id == -1) {
            // invalid id
            LOG.warn("Invalid layer with id:", layerId);
            return false;
        }
        final OskariLayer layer = layerService.find(id);
        if (layer == null) {
            LOG.warn("Couldn't find layer with id:", id);
            return false;
        }
        boolean hasPermission = permissionsService.findResource(ResourceType.maplayer, Integer.toString(layer.getId()))
                .filter(r -> r.hasPermission(user, PermissionType.PUBLISH)).isPresent();
        if (!hasPermission) {
            LOG.warn("User tried to publish layer with no publish permission. LayerID:", layerId, "- User:", user);
        }
        return hasPermission;
    }
}
