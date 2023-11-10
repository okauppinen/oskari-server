package org.oskari.myplaces.service.mybatis;

import fi.nls.oskari.db.DatasourceHelper;
import fi.nls.oskari.domain.map.MyPlace;
import fi.nls.oskari.domain.map.MyPlaceCategory;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.mybatis.MyBatisHelper;

import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.PropertyUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.oskari.myplaces.service.MyPlacesFeaturesService;

import javax.sql.DataSource;
import java.util.List;
import static fi.nls.oskari.map.geometry.ProjectionHelper.getSRID;

public class MyPlacesFeaturesServiceMybatisImpl implements MyPlacesFeaturesService {
    private static final Logger LOG = LogFactory.getLogger(MyPlacesFeaturesServiceMybatisImpl.class);
    private static final String NATIVE_SRS = "oskari.native.srs";
    private final int srid;
    private SqlSessionFactory factory = null;

    public MyPlacesFeaturesServiceMybatisImpl() {
        final DatasourceHelper helper = DatasourceHelper.getInstance();
        final DataSource dataSource = helper.getDataSource(helper.getOskariDataSourceName("myplaces"));
        if(dataSource != null) {
            factory = initializeMyBatis(dataSource);
        }
        else {
            LOG.error("Couldn't get datasource for myplaces");
        }
        srid = getSRID(PropertyUtil.get(NATIVE_SRS, "EPSG:4326"));
    }

    private SqlSessionFactory initializeMyBatis(final DataSource dataSource) {
        final Configuration configuration = MyBatisHelper.getConfig(dataSource);
        MyBatisHelper.addAliases(configuration, MyPlace.class, MyPlaceCategory.class);
        MyBatisHelper.addMappers(configuration, MyPlaceMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    @Override
    public List<MyPlace> getFeaturesByCategoryId(long categoryId) throws ServiceException {
        try (SqlSession session = factory.openSession()) {
            LOG.debug("getFeatures by category id: ", categoryId);
            final MyPlaceMapper mapper = session.getMapper(MyPlaceMapper.class);
            return mapper.findAllByCategoryId(categoryId);
        } catch (Exception e) {
            LOG.warn(e, "Exception when trying to get MyPlaces ");
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public List<MyPlace> getFeaturesByUserId(String uuid) throws ServiceException {
        try (SqlSession session = factory.openSession()) {
            LOG.debug("getFeatures by user id: ", uuid);
            final MyPlaceMapper mapper = session.getMapper(MyPlaceMapper.class);
            return mapper.findAllByUuId(uuid);
        } catch (Exception e) {
            LOG.warn(e, "Exception when trying to get myplaces by uuid ", uuid);
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public List<MyPlace> getFeaturesByMyPlaceId(long[] ids) throws ServiceException {
        try (SqlSession session = factory.openSession()) {
            LOG.debug("getFeatures by my place ids: ", ids);
            final MyPlaceMapper mapper = session.getMapper(MyPlaceMapper.class);
            return mapper.findAllByPlaceIdIn(ids);
        } catch (Exception e) {
            LOG.warn(e, "Exception when trying get features by place ids ", ids);
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public List<MyPlace> getFeatures(int categoryId, ReferencedEnvelope bbox)  throws ServiceException{
        try (SqlSession session = factory.openSession()) {
            LOG.debug("getFeatures by bbox: ", bbox);

            final MyPlaceMapper mapper = session.getMapper(MyPlaceMapper.class);
            return mapper.findAllByBBOX(categoryId, bbox, srid);
        } catch (Exception e) {
            LOG.warn(e, "Exception when trying to get features by bounding box ", bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public long[] insert(List<MyPlace> places) throws ServiceException {
        try (SqlSession session = factory.openSession()) {
            LOG.debug("Adding new places: ", places);
            final MyPlaceMapper mapper = session.getMapper(MyPlaceMapper.class);
            for (MyPlace place : places) {
                mapper.addMyPlace(place);
                LOG.info("inserted myplace: ", place.getId());
            }
            session.commit();
            return places.stream().mapToLong(MyPlace::getId).toArray();
        } catch (Exception e) {
            LOG.warn(e, "Exception when trying to add MyPlaces: ");
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public int update(List<MyPlace> places) throws ServiceException {
        try (SqlSession session = factory.openSession()) {
            LOG.debug("Adding new places: ", places);
            final MyPlaceMapper mapper = session.getMapper(MyPlaceMapper.class);
            for (MyPlace place : places) {
                mapper.updateMyPlace(place);
                LOG.info("updated myplace: ", place.getId());
            }
            session.commit();
            return places.size();
        } catch (Exception e) {
            LOG.warn(e, "Exception when trying to add MyPlaces ");
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public int delete(long[] ids) throws ServiceException {
        try (SqlSession session = factory.openSession()) {
            LOG.debug("Deleting from myPlaces: ", ids);
            final MyPlaceMapper mapper = session.getMapper(MyPlaceMapper.class);
            for (long id : ids) {
                mapper.deleteMyPlace(id);
                LOG.info("deleted myplace: ", id);
            }
            session.commit();
            return ids.length;
        } catch (Exception e) {
            LOG.warn(e, "Exception when trying to add MyPlaces ");
            throw new ServiceException(e.getMessage());
        }
    }
}
