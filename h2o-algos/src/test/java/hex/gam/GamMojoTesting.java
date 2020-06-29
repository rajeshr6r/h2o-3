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
  
  // test and make sure the h2opredict, mojo predict agrees with binomial dataset that includes
  // both enum and numerical datasets for the binomial family
  @Test
  public void testBinomialPredMojo() {
    try {
      Scope.enter();
      final Frame trainBinomial = Scope.track(createTrainTestFrame(2));
      final SplitFrame sf = new SplitFrame(trainBinomial, new double[]{0.8,0.2}, new Key[] {Key.make("train.hex"), Key.make("test.hex")});
      sf.exec().get();
      final Key[] ksplits = sf._destination_frames;
      final Frame tr = DKV.get(ksplits[0]).get();
      final Frame te = DKV.get(ksplits[1]).get();
      Scope.track(tr);
      Scope.track(te);

      final GAMModel.GAMParameters paramsO =  buildGamParams(tr, GLMModel.GLMParameters.Family.binomial);
      final GAMModel model = new GAM(paramsO).trainModel().get();
      Scope.track_generic(model);
      final Frame pred = model.score(te);
      Scope.track(pred);
      
      Assert.assertTrue(model.testJavaScoring(te, pred, _tol)); // compare scoring result with mojo
    } finally {
      Scope.exit();
    }
  }
  
  // test and make sure the h2opredict, mojo predict agrees with multinomial dataset that includes
  // both enum and numerical datasets for the multinomial family
  @Test
  public void testMultinomialPredMojo() {
    try {
      Scope.enter();
      final Frame trainMultinomial = Scope.track(createTrainTestFrame(5));
      SplitFrame sf = new SplitFrame(trainMultinomial, new double[]{0.8,0.2}, new Key[] {Key.make("train.hex"), 
              Key.make("test.hex")});
      sf.exec().get();
      Key[] ksplits = sf._destination_frames;
      final Frame tr = DKV.get(ksplits[0]).get();
      final Frame te = DKV.get(ksplits[1]).get();
      Scope.track(tr);
      Scope.track(te);

      final GAMModel.GAMParameters paramsO = buildGamParams(tr, GLMModel.GLMParameters.Family.multinomial);
      final GAMModel model = new GAM(paramsO).trainModel().get();
      Scope.track_generic(model);
      final Frame pred = model.score(te);
      Scope.track(pred);

      Assert.assertTrue(model.testJavaScoring(te, pred, _tol)); // compare scoring result with mojo
    } finally {
      Scope.exit();
    }
  }

  // test and make sure the h2opredict, mojo predict agrees with gaussian dataset that includes
  // both enum and numerical datasets for the gaussian family
  @Test
  public void testGaussianPredMojo() {
    try {
      Scope.enter();
      Frame trainGaussian = Scope.track(createTrainTestFrame(1));
      SplitFrame sf = new SplitFrame(trainGaussian, new double[]{0.8,0.2}, new Key[] {Key.make("train.hex"),
              Key.make("test.hex")});
      sf.exec().get();
      Key[] ksplits = sf._destination_frames;
      Frame tr = DKV.get(ksplits[0]).get();
      Frame te = DKV.get(ksplits[1]).get();
      Scope.track(tr);
      Scope.track(te);

      GAMModel.GAMParameters paramsO = buildGamParams(tr, GLMModel.GLMParameters.Family.gaussian);
      GAMModel model = new GAM(paramsO).trainModel().get();
      Scope.track_generic(model);
      Frame pred = model.score(te);
      Scope.track(pred);

      Assert.assertTrue(model.testJavaScoring(te, pred, _tol)); // compare scoring result with mojo
    } finally {
      Scope.exit();
    }
  }
  
  public GAMModel.GAMParameters buildGamParams(Frame train, GLMModel.GLMParameters.Family fam) {
    GAMModel.GAMParameters paramsO = new GAMModel.GAMParameters();
    paramsO._train = train._key;
    paramsO._lambda_search = false;
    paramsO._response_column = "response";
    paramsO._lambda = new double[]{0};
    paramsO._alpha = new double[]{0.001};  // l1pen
    paramsO._objective_epsilon = 1e-6;
    paramsO._beta_epsilon = 1e-4;
    paramsO._standardize = false;
    paramsO._family = fam;
    paramsO._gam_columns =  chooseGamColumns(train, 3);
    return paramsO;
  }

  public String[] chooseGamColumns(Frame trainF, int maxGamCols) {
    int gamCount=0;
    ArrayList<String> numericCols = new ArrayList<>();
    String[] colNames = trainF.names();
    for (String cnames : colNames) {
      if (trainF.vec(cnames).isNumeric() && !trainF.vec(cnames).isInt()) {
        numericCols.add(cnames);
        gamCount++;
      }
      if (gamCount >= maxGamCols)
        break;
    }
    String[] gam_columns = new String[numericCols.size()];
    return numericCols.toArray(gam_columns);
  }
  
  public Frame createTrainTestFrame(int responseFactor) {
    CreateFrame cf = new CreateFrame();
    Random generator = new Random();
    int numRows = generator.nextInt(10000)+15000+200;
    int numCols = generator.nextInt(17)+3;
    cf.rows= numRows;
    cf.cols = numCols;
    cf.factors=10;
    cf.has_response=true;
    cf.response_factors = responseFactor; // 1 for real-value response
    cf.positive_response=true;
    cf.missing_fraction = 0;
    cf.seed = 12345;
    System.out.println("Createframe parameters: rows: "+numRows+" cols:"+numCols+" seed: "+cf.seed);
    return cf.execImpl().get();
  }
}
