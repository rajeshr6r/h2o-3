package hex.genmodel.algos.gam;

public class GamMojoModel extends GamMojoModelBase {
  String _link;
  double _tweedieLinkPower;
  private boolean _binomial;
  
  GamMojoModel(String[] columns, String[][] domains, String responseColumn) {
    super(columns, domains, responseColumn);
  }
  
  void init() {
    super.init();
    _binomial = _family.equals("binomial");
  }
  
  // generate prediction for binomial/fractional binomial/negative binomial, poisson, tweedie families
  @Override
  double[] gamScore0(double[] data, double[] preds) {
    if (data.length == nfeatures())  // centered data, use center coefficients
      _beta = _beta_center;
    else  // use non-centering coefficients
      _beta = _beta_no_center;

    double eta = 0.0;
    for (int i = 0; i < _catOffsets.length-1; ++i) {  // take care of contribution from categorical columns
      int ival = readCatVal(data[i], i);
      if ((ival < _catOffsets[i + 1]) && (ival >= 0))
        eta += _beta[ival];
    }

    int noff = _catOffsets[_cats] - _cats;
    for(int i = _cats; i < _beta.length - 1 - noff; ++i)
      eta += _beta[noff + i] * data[i];
    eta += _beta[_beta.length - 1]; // add intercept
    double mu = evalLink(eta, _link);

    if (_binomial || _family.equals("fractionalbinomial") || _family.equals("negativebinomial")) {
      preds[0] = (mu >= _defaultThreshold) ? 1 : 0; // threshold given by ROC
      preds[1] = 1.0 - mu; // class 0
      preds[2] =       mu; // class 1
    } else {
      preds[0] = mu;
    }
    return preds;
  }
}
