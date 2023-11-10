package org.oskari.control.analysis;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.service.OskariComponentManager;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.ConversionHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;
import org.oskari.log.AuditLog;
import org.oskari.service.analysis.AnalysisDataService;

@OskariActionRoute("DeleteAnalysisLayerTest")
public class DeleteAnalysisLayerHandler extends RestActionHandler {

    private final static String PARAM_ID = "id";
    private final static Logger log = LogFactory.getLogger(DeleteAnalysisLayerHandler.class);

    private AnalysisDbService analysisDataService;

    @Override
    public void init() {
        super.init();
        if(analysisDataService == null) {
            analysisDataService = OskariComponentManager.getComponentOfType(AnalysisDbService.class);
        }
    }

    @Override
    public void handlePost(ActionParameters params) throws ActionException {
        params.requireLoggedInUser();
        final long id = ConversionHelper.getLong(params.getHttpParam(PARAM_ID), -1);
        if(id == -1) {
            throw new ActionParamsException("Parameter missing or non-numeric: " + PARAM_ID + "=" + params.getHttpParam(PARAM_ID));
        }

        final Analysis analysis = analysisDataService.getAnalysisById(id);
        if(analysis == null) {
            throw new ActionParamsException("Analysis id didn't match any analysis: " + id);
        }
        if(!analysis.isOwnedBy(params.getUser().getUuid())) {
            throw new ActionDeniedException("Analysis belongs to another user");
        }

        try {
            // remove analysis
            analysisDataService.deleteAnalysis(analysis);
            AuditLog.user(params.getClientIp(), params.getUser())
                    .withParam("id", id)
                    .deleted(AuditLog.ResourceType.ANALYSIS);
            // write static response to notify success {"result" : "success"}
            ResponseHelper.writeResponse(params, JSONHelper.createJSONObject("result", "success"));
        } catch (ServiceException ex) {
            throw new ActionException("Error deleting analysis", ex);
        }
    }
}