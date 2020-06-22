package water.api;

import water.MRTask;
import water.api.schemas3.FeatureFlagV3;
import water.rapids.Rapids;
import water.rapids.Val;


public class FeatureFlagHandler extends Handler {
  

  @SuppressWarnings("unused") // called via reflection
  public FeatureFlagV3 setFeatureFlag(int version, FeatureFlagV3 request) throws Exception {
    Val val = Rapids.exec("(setproperty 'sys.ai.h2o." + request.property + "' '" + request.value + "')");
    new MRTask() {
      @Override
      protected void setupLocal() {
        if (!(String.valueOf(request.value)).equals(System.getProperty("sys.ai.h2o." + request.property))) {
          throw new IllegalStateException("System property was not set");
        }
      }
    }.doAllNodes();
    return request;
  }
  
}
