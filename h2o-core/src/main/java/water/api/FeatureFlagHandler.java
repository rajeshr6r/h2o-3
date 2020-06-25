package water.api;

import water.H2O;
import water.MRTask;
import water.api.schemas3.FeatureFlagV3;
import water.rapids.Rapids;
import water.rapids.Val;


public class FeatureFlagHandler extends Handler {
  

  @SuppressWarnings("unused") // called via reflection
  public FeatureFlagV3 setFeatureFlag(int version, FeatureFlagV3 request) throws Exception {
    Val val = Rapids.exec("(setproperty '" + H2O.OptArgs.SYSTEM_PROP_PREFIX + request.property + "' '" + request.value + "')");
    return request;
  }
  
}
