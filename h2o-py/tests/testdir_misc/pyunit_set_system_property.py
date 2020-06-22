from __future__ import print_function
import sys
sys.path.insert(1,"../../")
import h2o
from tests import pyunit_utils


def test_set_feature_flag():
    try:
        h2o.backend.H2OCluster.set_feature_flag("enable_evaluation_of_auto_model_param", True)
        assert False, "Should have failed here due invalid feature flag"
    except:
        pass

    h2o.backend.H2OCluster.set_feature_flag("enable_evaluation_of_auto_model_parameters", False)
    h2o.backend.H2OCluster.set_feature_flag("enable_evaluation_of_auto_model_parameters", True)

if __name__ == "__main__":
  pyunit_utils.standalone_test(test_set_feature_flag)
else:
    test_set_feature_flag()
