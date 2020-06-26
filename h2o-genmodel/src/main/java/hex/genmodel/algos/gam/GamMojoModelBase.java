package hex.genmodel.algos.gam;

import hex.genmodel.GenModel;
import hex.genmodel.MojoModel;

public abstract class GamMojoModelBase extends MojoModel {
  String _link;
  boolean _useAllFactorLevels;
  int _cats;
  int[] _catNAFills;
  int[] _catOffsets;
  int _nums;
  double[] _numNAFills;
  boolean _meanImputation;
  double[] _beta;
  double[] _beta_no_center;
  double[] _beta_center;
  double[][] _beta_multinomial;
  double[][] _beta_multinomial_no_center; // coefficients not centered for multinomial/ordinal
  double[][] _beta_multinomial_center; // coefficients not centered for multinomial/ordinal
  String _family;
  String[] _gam_columns;
  int[] _bs;
  int[] _num_knots;
  double[][] _knots;
  double[][][] _binvD;
  double[][][] _zTranspose;
  String[][] _gamColNames;  // expanded gam column names
  String[][] _gamColNamesCenter;
  String[] _names_no_centering; // column names of features with no centering
  int _totFeatureSize; // number of predictors plus gam columns no centered
  int _betaSizePerClass;
  int _betaCenterSizePerClass;
  double _tweedieLinkPower;
  
  public int get_totFeatureSize() { return _totFeatureSize;}
  
  GamMojoModelBase(String[] columns, String[][] domains, String responseColumn) {
    super(columns, domains, responseColumn);
  }
  
  @Override
  public double[] score0(double[] row, double[] preds) {
    if (_meanImputation)
      imputeMissingWithMeans(row);  // perform imputation for each row
    
    return gamScore0(row, preds);
  }
  
  void init() {;}
  
  abstract double[] gamScore0(double[] row, double[] preds);
  
  private void imputeMissingWithMeans(double[] data) {
    for (int ind=0; ind < _cats; ind++)
      if (Double.isNaN(data[ind])) data[ind] = _catNAFills[ind];
    for (int ind=0; ind < _nums; ind++)
      if (Double.isNaN(data[ind+_cats])) data[ind] = _numNAFills[ind];
  }
  
  double evalLink(double val, String linkType) {
    switch (linkType) {
      case "identity": return GenModel.GLM_identityInv(val);
      case "logit": return GenModel.GLM_logitInv(val);
      case "log": return GenModel.GLM_logInv(val);
      case "inverse": return GenModel.GLM_inverseInv(val);
      case "tweedie": return GenModel.GLM_tweedieInv(val, _tweedieLinkPower);
      default: throw new UnsupportedOperationException("Unexpected link function "+linkType);
    }
  }

  // This method will read in categorical value and adjust for when useAllFactorLevels = true or false
  int readCatVal(double data, int dataIndex) {
    int ival = _useAllFactorLevels?((int) data):((int) data-1);
    double targetVal = _useAllFactorLevels?(data):(data-1);
    if (ival != targetVal) throw new IllegalArgumentException("categorical value out of range");

    ival += _catOffsets[dataIndex];
    return ival;
  }
}
