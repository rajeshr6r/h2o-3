package hex.genmodel.algos.gam;

import hex.genmodel.GenModel;
import hex.genmodel.MojoModel;
import hex.genmodel.easy.RowData;
import hex.genmodel.utils.ArrayUtils;

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
  int _num_gam_columns;
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
  double[][] _basisVals; // store basis values array for each gam column
  double[][] _hj;   // difference between knot values
  
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
  
  void init() {
    _basisVals = new double[_gam_columns.length][];
    _hj = new double[_gam_columns.length][];
    for (int ind=0; ind < _num_gam_columns; ind++) {
      _basisVals[ind] = new double[_num_knots[ind]];
      _hj[ind] = ArrayUtils.eleDiff(_knots[ind]);
    }
  }
  
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
  
  public void addExpandGamCols(double[] rawData, RowData rowData) { // add all expanded gam columns here
    int colCounter=0;
    for (String cname : _names_no_centering) {  // first enter values from regular predictors
      if (rowData.containsKey(cname))
        rawData[colCounter++] = (double) rowData.get(cname);
      else
        break;
    }
    // add expanded gam columns to rowData
    int dataIndStart = colCounter; // starting index to fill out the rawData
    for (int cind = 0; cind < _num_gam_columns; cind++) {
      if (_bs[cind]==0) { // to generate basis function values for cubic regression spline
        GamUtilsCubicRegression.expandOneGamCol((double) rowData.get(_gam_columns[cind]), _binvD[cind], 
                _basisVals[cind], _hj[cind], _knots[cind]);
      } else {
        throw new IllegalArgumentException("spline type not implemented!");
      }
      System.arraycopy(_basisVals, 0, rawData, dataIndStart, _num_knots[cind]); // copy expanded gam to rawData
      dataIndStart += _num_knots[cind];
    }
  }
}
