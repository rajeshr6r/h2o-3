package water.api.schemas3;

import water.Iced;
import water.api.API;

public class FeatureFlagV3 extends RequestSchemaV3<Iced, FeatureFlagV3>  {

  //Input fields
  @API(help = "System property", direction=API.Direction.INPUT)
  public String property = "";

  @API(help = "System property value", direction=API.Direction.INPUT)
  public boolean value = true;
}

