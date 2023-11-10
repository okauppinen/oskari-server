package fi.nls.oskari.map.analysis.service;

import fi.nls.oskari.domain.map.analysis.Analysis;
import fi.nls.oskari.domain.map.analysis.AnalysisData;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface AnalysisMapper {

    void insertAnalysis(final Analysis analysis);
    List<Analysis> findAnalysisByUuid (String uuid);
    void deleteAnalysisById(final long id);
    void updatePublisherName();
    void mergeAnalysisData(final Analysis analysis);

    void insertAnalysisData
    void insertAnalysisData(@Param ("data") final AnalysisData data, @Param("analysisId") final long analysisId, @Param("srid") final int srid);
    void deleteAnalysisDataByLayerId(final long id);
}
