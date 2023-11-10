package org.oskari.control.analysis;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.service.OskariComponentManager;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetAnalysisResults")
public class GetAnalysisResultsHandler extends RestActionHandler {

    @Override
    public void handlePost(ActionParameters params) throws ActionException {


        ResponseHelper.writeResponse(params, ulayer);
    }
}
