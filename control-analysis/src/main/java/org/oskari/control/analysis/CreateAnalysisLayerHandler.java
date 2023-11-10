package org.oskari.control.analysis;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.OskariComponentManager;
import fi.nls.oskari.util.ResponseHelper;
import org.oskari.log.AuditLog;
import org.oskari.service.analysis.AnalysisDataService;
import org.oskari.service.analysis.AnalysisDbServiceMybatisImpl;


@OskariActionRoute("CreateAnalysisLayerTest")
public class CreateAnalysisLayerHandler extends RestActionHandler {
    private static final Logger log = LogFactory.getLogger(CreateAnalysisLayerHandler.class);
    private AnalysisDbService analysisService;

    public void setAnalysisService(AnalysisDbService analysisService) {
        this.analysisService = analysisService;
    }

    @Override
    public void init() {
        if (analysisService == null) {
            analysisService = new AnalysisDbServiceMybatisImpl();
        }
    }

    @Override
    public void handlePost(ActionParameters params) throws ActionException {
        params.requireLoggedInUser();

        AuditLog.user(params.getClientIp(), params.getUser())
                .withParam("id", analysisLayer.getId())
                .withParam("uiName", analysisLayer.getName())
                // there can be multiple srcId at least in methodParams.layerId is one place that can have it
                .withParam("srcId", analyseJson.opt("layerId"))
                .added(AuditLog.ResourceType.ANALYSIS);
        ResponseHelper.writeResponse(params, analysisLayerJSON);
    }
}
