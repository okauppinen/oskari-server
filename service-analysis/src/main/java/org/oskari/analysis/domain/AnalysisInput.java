package org.oskari.analysis.domain;

public class AnalysisInput {
    private Analysis analysis;
    private Filter layerFilter;
    private Filter targetFilter;

    private class Analysis {
        private String layerId;
        private String name;
        private String method; // TODO: enum
        private String layerType; // oskari maplayer type
        private int opacity;
        private List<String> fields; // selected properties
        private Map<String, String>; // property types (number, string) TODO: get from layer
        private Map<String, Object> style; //oskari vector style
        private Map<String, Double> bbox; // extent TODO: or Float
        private Map<String, Object> methodParams;
    }
    private class Filter {
        private Map<String, Double> bbox; // extent TODO: or Float
        private List<String> featureId;
        private Map<String, Object> wfs;
    }

}
