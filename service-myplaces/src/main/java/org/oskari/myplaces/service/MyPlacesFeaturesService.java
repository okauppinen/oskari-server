package org.oskari.myplaces.service;

import fi.nls.oskari.domain.map.MyPlace;
import fi.nls.oskari.service.ServiceException;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.util.List;

public interface MyPlacesFeaturesService {
    public List<MyPlace> getFeaturesByCategoryId(long categoryId) throws ServiceException;
    public List<MyPlace> getFeaturesByUserId(String uuid) throws ServiceException;
    public List<MyPlace> getFeaturesByMyPlaceId(long[] ids) throws ServiceException;
    public List<MyPlace> getFeatures(int categoryId, ReferencedEnvelope bbox) throws ServiceException;

    public long[] insert(List<MyPlace> places) throws ServiceException;
    public int update(List<MyPlace> places) throws ServiceException;
    public int delete(long[] ids) throws ServiceException;
}
