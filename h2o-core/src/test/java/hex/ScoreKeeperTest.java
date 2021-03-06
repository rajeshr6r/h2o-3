package hex;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import water.TestUtil;
import water.util.Log;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertTrue;

public class ScoreKeeperTest extends TestUtil {
  @BeforeClass
  public static void stall() { stall_till_cloudsize(1); }

  // implement early stopping logic from scratch
  private static boolean stopEarly(double[] val, int k, double tolerance, boolean moreIsBetter, boolean verbose) {
    if (val.length-1 < 2*k) return false; //need 2k scoring events (+1 to skip the very first one, which might be full of NaNs)
    double[] moving_avg = new double[k+1]; //one moving avg for the last k+1 scoring events (1 is reference, k consecutive attempts to improve)

    // compute moving average(s)
    for (int i=0;i<moving_avg.length;++i) {
      moving_avg[i]=0;
      int startidx=val.length-2*k+i;
      for (int j=0;j<k;++j)
        moving_avg[i]+=val[startidx+j];
      moving_avg[i]/=k;
    }
    if (verbose) Log.info("JUnit: moving averages: " + Arrays.toString(moving_avg));

    // check if any of the moving averages is better than the reference (by at least tolerance relative improvement)
    double ref = moving_avg[0];
    boolean improved = false;
    for (int i=1;i<moving_avg.length;++i) {
      if (moreIsBetter)
        improved |= (moving_avg[i] > ref*(1+tolerance));
      else
        improved |= (moving_avg[i] < ref*(1-tolerance));
      if (improved && verbose)
        Log.info("JUnit: improved from " + ref + " to " + moving_avg[i] + " by at least " + tolerance + " relative tolerance");
    }
    if (improved) {
      if (verbose) Log.info("JUnit: Still improving.");
      return false;
    }
    else {
      if (verbose) Log.info("JUnit: Stopped.");
      return true;
    }
  }

  // helper
  private static ScoreKeeper[] fillScoreKeeperArray(double[] values, boolean moreIsBetter) {
    ScoreKeeper[] sk = new ScoreKeeper[values.length];
    for (int i=0;i<values.length;++i) {
      sk[i] = new ScoreKeeper();
      if (moreIsBetter)
        sk[i]._AUC = values[i];
      else
        sk[i]._logloss = values[i];
    }
    return sk;
  }

  @Test
  public void testConvergenceScoringHistory() {
    Random rng = new Random(0xC0FFEE);
    int count=0;
    while (count++ < 100) {
      boolean moreIsBetter = rng.nextBoolean();
      ScoreKeeper.StoppingMetric metric = moreIsBetter ? ScoreKeeper.StoppingMetric.AUC : ScoreKeeper.StoppingMetric.logloss;
      double tol = rng.nextFloat()*1e-1;
      int N = 5+rng.nextInt(10);
      double[] values = new double[N];
      for (int i=0;i<N;++i) {
        //random walk around linearly increasing (or decreasing) values around 20 (not around 0 to avoid zero-crossing issues)
        values[i] = (moreIsBetter ? 10 + (double) i / N : 10 - (double) i / N) + rng.nextGaussian() * 0.33;
      }
      ScoreKeeper[] sk = fillScoreKeeperArray(values, moreIsBetter);
      Log.info();
      Log.info("series: " + Arrays.toString(values));
      Log.info("moreIsBetter: " + moreIsBetter);
      Log.info("relative tolerance: " + tol);
      for (int k=values.length-1;k>0;k--) {
        boolean c = stopEarly(values, k, tol, moreIsBetter, false /*verbose*/);
        boolean d = ScoreKeeper.stopEarly(sk, k, ScoreKeeper.ProblemType.classification, metric, tol, "JUnit's", false /*verbose*/);
        // for debugging
//        Log.info("Checking for stopping condition with k=" + k + ": " + c + " " + d);
        if (c || d) Log.info("Stopped for k=" + k);
//        if (!c && !d && k==1) Log.info("Still improving.");
//        if (d!=c) {
//          Log.info("k="+ k);
//          Log.info("tol="+ tol);
//          Log.info("moreIsBetter="+ moreIsBetter);
//          stopEarly(values, k, tol, moreIsBetter, true /*verbose*/);
//          ScoreKeeper.stopEarly(sk, k, true /*classification*/, metric, tol, "JUnit", true /*verbose*/);
//        }
        assertTrue("For k="+k+", JUnit: " + c + ", ScoreKeeper: " + d, c == d);
      }
    }
  }

  @Test
  public void testGridSearch() {
    Random rng = new Random(0xDECAF);
    int count=0;
    while (count++<100) {
      final boolean moreIsBetter = rng.nextBoolean();

      Double[] Dvalues;
      double tol;
      if (true) {
        // option 1: random values
        int N = 5 + rng.nextInt(10);
        tol = rng.nextDouble() * 0.1;
        Dvalues = new Double[N];
        for (int i = 0; i < N; ++i)
          Dvalues[i] = 10 + rng.nextDouble(); //every grid search models has a random score between 10 and 11 (not around 0 to avoid zero-crossing issues)
      } else {
        // option 2: manual values
        tol = 0;
        Dvalues = new Double[]{0.91, 0.92, 0.95, 0.94, 0.93}; //in order of occurrence
      }

      // sort to get "leaderboard"
      Arrays.sort(Dvalues, new Comparator<Double>() {
        @Override
        public int compare(Double o1, Double o2) {
          int val = o1.doubleValue() < o2.doubleValue() ? 1 : o1.doubleValue()==o2.doubleValue() ? 0 : -1;
          if (moreIsBetter) val=-val;
          return val;
        }
      });
      double[] values = new double[Dvalues.length];
      for (int i=0;i<values.length;++i) values[i] = Dvalues[i].doubleValue();
      Log.info("Sorted values (leaderboard) - rightmost is best: " + Arrays.toString(values));
      for (int k=1;k<values.length;++k) {
        Log.info("Testing k=" + k);
        ScoreKeeper.StoppingMetric metric = moreIsBetter ? ScoreKeeper.StoppingMetric.AUC : ScoreKeeper.StoppingMetric.logloss;
        ScoreKeeper[] sk = fillScoreKeeperArray(values, moreIsBetter);
        boolean c = stopEarly(values, k, tol, moreIsBetter, true /*verbose*/);
        boolean d = ScoreKeeper.stopEarly(sk, k, ScoreKeeper.ProblemType.classification, metric, tol, "JUnit's", true /*verbose*/);
        assertTrue("For k=" + k + ", JUnit: " + c + ", ScoreKeeper: " + d, c == d);
      }
    }
  }

  @Test
  public void testHitLowerBound() {
    ScoreKeeper[] sks = new ScoreKeeper[]{
            new ScoreKeeper(0.3),
            new ScoreKeeper(0.2),
            new ScoreKeeper(0.1),
            new ScoreKeeper(0.0),
            new ScoreKeeper(0.0)
    };
    
    boolean stopEarly = ScoreKeeper.stopEarly(sks, 1, ScoreKeeper.ProblemType.regression, ScoreKeeper.StoppingMetric.MSE, 0.01, "MSE", true);
    assertTrue(stopEarly);
  }

  @Test
  public void testQueryConvergenceStrategy() { // ConvergenceStrategy gets the right input to make the decision
    ScoreKeeper.IConvergenceStrategy mockConvergence = mock(ScoreKeeper.IConvergenceStrategy.class);
    ScoreKeeper.IStoppingMetric mockMetric = mock(ScoreKeeper.IStoppingMetric.class);
    when(mockMetric.getConvergenceStrategy()).thenReturn(mockConvergence);
    when(mockMetric.metricValue(any(ScoreKeeper.class))).thenAnswer(new Answer<Double>() {
      @Override
      public Double answer(InvocationOnMock invocation)  {
        return ((ScoreKeeper) invocation.getArgument(0))._mse;
      }
    });
    when(mockConvergence.extremePoint(anyDouble(), anyDouble(), anyDouble())).thenReturn(0.1);

    ScoreKeeper[] sks = new ScoreKeeper[]{
            new ScoreKeeper(0.6),
            new ScoreKeeper(0.5),
            new ScoreKeeper(0.4),
            new ScoreKeeper(0.3),
            new ScoreKeeper(0.2),
            new ScoreKeeper(0.1)
    };

    ScoreKeeper.stopEarly(sks, 2, ScoreKeeper.ProblemType.regression, mockMetric, 0.01, "Mock", true);

    verify(mockConvergence).extremePoint(0.35d, 0.15000000000000002d, 0.25);
    verify(mockConvergence).stopEarly(0.35d, 0.15000000000000002d, 0.25, 0.01);
  }

  @Test
  public void testConvergenceStrategy_extremePoint() {
    assertEquals(0.2, ScoreKeeper.ConvergenceStrategy.MORE_IS_BETTER.extremePoint(0, 0.1, 0.2), 0);
    assertEquals(0.1, ScoreKeeper.ConvergenceStrategy.LESS_IS_BETTER.extremePoint(0, 0.1, 0.2), 0);
    assertEquals(0.1, ScoreKeeper.ConvergenceStrategy.NON_DIRECTIONAL.extremePoint(0.16, 0.1, 0.2), 0);
    assertEquals(0.2, ScoreKeeper.ConvergenceStrategy.NON_DIRECTIONAL.extremePoint(0.14, 0.1, 0.2), 0);
  }

  @Test
  public void testConvergenceStrategy_stopEarly() {
    assertFalse(ScoreKeeper.ConvergenceStrategy.MORE_IS_BETTER.stopEarly(0.1, 0.1, 0.2, 0.01));
    assertFalse(ScoreKeeper.ConvergenceStrategy.MORE_IS_BETTER.stopEarly(0.1, 0.1, 0.2, 0.1));
    assertTrue(ScoreKeeper.ConvergenceStrategy.MORE_IS_BETTER.stopEarly(0.1, 0.1, 0.2, 1.0));

    assertFalse(ScoreKeeper.ConvergenceStrategy.LESS_IS_BETTER.stopEarly(0.2, 0.1, 0.3, 0.01));
    assertFalse(ScoreKeeper.ConvergenceStrategy.LESS_IS_BETTER.stopEarly(0.2, 0.1, 0.3, 0.1));
    assertTrue(ScoreKeeper.ConvergenceStrategy.LESS_IS_BETTER.stopEarly(0.2, 0.1, 0.3, 1.0));

    assertFalse(ScoreKeeper.ConvergenceStrategy.NON_DIRECTIONAL.stopEarly(0.14, 0.1, 0.2, 0.01));
    assertFalse(ScoreKeeper.ConvergenceStrategy.NON_DIRECTIONAL.stopEarly(0.14, 0.1, 0.2, 0.1));
    assertTrue(ScoreKeeper.ConvergenceStrategy.NON_DIRECTIONAL.stopEarly(0.14, 0.1, 0.2, 1.0));

    assertFalse(ScoreKeeper.ConvergenceStrategy.NON_DIRECTIONAL.stopEarly(0.16, 0.1, 0.2, 0.01));
    assertFalse(ScoreKeeper.ConvergenceStrategy.NON_DIRECTIONAL.stopEarly(0.16, 0.1, 0.2, 0.1));
    assertTrue(ScoreKeeper.ConvergenceStrategy.NON_DIRECTIONAL.stopEarly(0.16, 0.1, 0.2, 1.0));
  }

}
