package fi.nls.oskari.statistics.eurostat;

import fi.nls.oskari.control.statistics.plugins.*;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class EurostatStatisticalIndicatorLayer implements StatisticalIndicatorLayer {

    private long id;
    private String indicatorId;
    private String baseUrl;
    private String regionKey;

    public EurostatStatisticalIndicatorLayer(long id, String indicatorId, String baseUrl, String regionKey) {
        this.id = id;
        this.indicatorId = indicatorId;
        this.baseUrl = baseUrl;
        this.regionKey = regionKey;
    }

    @Override
    public long getOskariLayerId() {
        return id;
    }

    @Override
    public IndicatorValueType getIndicatorValueType() {
        return null;
    }

    /*
    "query": [
   {
     "code": "Alue",
     "selection": {
       "filter": "item",
       "values": [
         "0910000000",
         "0911000000",
         "0911101000"
       ]
     }
   },
   {
     "code": "Käyttötarkoitus",
     "selection": {
       "filter": "item",
       "values": [
         "all",
         "01",
         "02"
       ]
     }
   },
   {
     "code": "Toimenpide",
     "selection": {
       "filter": "item",
       "values": [
         "all",
         "1"
       ]
     }
   },
   {
     "code": "Yksikkö",
     "selection": {
       "filter": "item",
       "values": [
         "1",
         "2"
       ]
     }
   },
   {
     "code": "Vuosi",
     "selection": {
       "filter": "item",
       "values": [
         "0",
         "1",
         "2"
       ]
     }
   }
 ],
 "response": {
   "format": "csv"
 }
}
     */
    @Override
    public Map<String, IndicatorValue> getIndicatorValues(StatisticalIndicatorSelectors selectors) {
        Map<String, String> params = new HashMap<>();
        for (StatisticalIndicatorSelector selector : selectors.getSelectors()) {
            if (regionKey.equalsIgnoreCase(selector.getId())) {
                // skip Alue
                continue;
            }
            params.put(selector.getId(), selector.getValue());
        }
        String url = IOHelper.constructUrl(baseUrl +"wdds/rest/data/v2.1/json/en/" + indicatorId, params);


        Map<String, IndicatorValue> values = new HashMap<>();
        try {
            final String data = IOHelper.getURL(url);
            // TODO: parsing
            JSONObject json = JSONHelper.createJSONObject(data);
            JSONObject stats = json.optJSONObject("dataset").optJSONObject("dimension").optJSONObject(regionKey).optJSONObject("category").optJSONObject("index");
            JSONArray responseValues = json.optJSONObject("dataset").optJSONArray("value");
            JSONArray names = stats.names();
            for (int i = 0; i < names.length(); ++i) {
                String region = names.optString(i);
                Double val = responseValues.optDouble(stats.optInt(region));
                if (val.isNaN()) {
                    continue;
                }
                IndicatorValue indicatorValue = new IndicatorValueFloat(val);
                values.put(region, indicatorValue);
            }
        } catch (IOException e) {
            throw new APIException("Couldn't get data from service/parsing failed", e);
        }

        return values;
    }
}
