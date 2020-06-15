package hex.gam;

import hex.CreateFrame;
import hex.SplitFrame;
import hex.glm.GLMModel;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import water.DKV;
import water.Key;
import water.Scope;
import water.TestUtil;
import water.fvec.Frame;
import water.runner.CloudSize;
import water.runner.H2ORunner;

import java.util.ArrayList;
import java.util.Random;

@RunWith(H2ORunner.class)
@CloudSize(1)
public class GamMojoTesting extends TestUtil {
  public static final double _tol = 1e-6;
  // test and make sure the h2opredict, pojo and mojo predict agrees with multinomial dataset that includes
  // both enum and numerical datasets
  @Test
  public void testBinomialPredMojo() {
    try {
      Scope.enter();
      CreateFrame cf = new CreateFrame();
      Random generator = new Random();
      int numRows = generator.nextInt(10000)+15000+200;
      int numCols = generator.nextInt(17)+3;
      cf.rows= numRows;
      cf.cols = numCols;
      cf.factors=10;
      cf.has_response=true;
      cf.response_factors = 2;
      cf.positive_response=true;
      cf.missing_fraction = 0;
      cf.seed = 12345;
      System.out.println("Createframe parameters: rows: "+numRows+" cols:"+numCols+" seed: "+cf.seed);
      Frame trainBinomial = Scope.track(cf.execImpl().get());
      SplitFrame sf = new SplitFrame(trainBinomial, new double[]{0.8,0.2}, new Key[] {Key.make("train.hex"), Key.make("test.hex")});
      sf.exec().get();
      Key[] ksplits = sf._destination_frames;
      Frame tr = DKV.get(ksplits[0]).get();
      Frame te = DKV.get(ksplits[1]).get();
      Scope.track(tr);
      Scope.track(te);

      GAMModel.GAMParameters paramsO = new GAMModel.GAMParameters();
      paramsO._train = tr._key;
      paramsO._lambda_search = false;
      paramsO._response_column = "response";
      paramsO._lambda = new double[]{0};
      paramsO._alpha = new double[]{0.001};  // l1pen
      paramsO._objective_epsilon = 1e-6;
      paramsO._beta_epsilon = 1e-4;
      paramsO._standardize = false;
      paramsO._family = GLMModel.GLMParameters.Family.binomial;
      paramsO._link = GLMModel.GLMParameters.Link.logit;
      int gamCount=0;
      ArrayList<String> numericCols = new ArrayList<>();
      String[] colNames = trainBinomial.names();
      for (String cnames : colNames) {
        if (trainBinomial.vec(cnames).isNumeric() && !trainBinomial.vec(cnames).isInt()) {
          numericCols.add(cnames);
          gamCount++;
        }
        if (gamCount >= 3)
          break;
      }
      paramsO._gam_columns = new String[numericCols.size()];
      paramsO._gam_columns =  numericCols.toArray(paramsO._gam_columns);
      
      GAMModel model = new GAM(paramsO).trainModel().get();
      Scope.track_generic(model);
      Frame pred = model.score(te);
      Scope.track(pred);
      
      Assert.assertTrue(model.testJavaScoring(te, pred, _tol)); // compare scoring result with mojo
    } finally {
      Scope.exit();
    }
  }
}
