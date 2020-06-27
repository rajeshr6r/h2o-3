package hex.genmodel.algos.gam;

import java.util.Arrays;

public class GamMojoMultinomialModel extends GamMojoModelBase {
    private boolean _multinomial; // true multinomial, false ordinal
    private int _num_offset;

    GamMojoMultinomialModel(String[] columns, String[][] domains, String responseColumn) {
        super(columns, domains, responseColumn);
    }

    void init() {
        super.init();
        _multinomial = _family.equals("multinomial");
        _num_offset = _catOffsets[_cats];
    }
    
    @Override
    double[] gamScore0(double[] row, double[] preds) {
        if (row.length == nfeatures())
            _beta_multinomial = _beta_multinomial_center;
        else
            _beta_multinomial = _beta_multinomial_no_center;
        
        Arrays.fill(preds, 0.0);
        for (int c=0; c<_nclasses; ++c) {
            for (int i = 0; i < _catOffsets.length-1; ++i) {  // take care of contribution from categorical columns
                int ival = readCatVal(row[i], i);
                if (ival < _catOffsets[i + 1])
                    preds[c+1] += _beta_multinomial[c][ival];
            }
            
            for (int i=0; i < _nums; i++)
                preds[c+1] += _beta_multinomial[c][i+_num_offset]*row[i+_cats];
            preds[c+1] += _beta_multinomial[c][_beta_multinomial[c].length-1];
        }
        if (_multinomial)
            return postPredMultinomial(preds);
        else // post process predict for ordinal family
            return postPredOrdinal(preds);
    }
    
    double[] postPredMultinomial(double[] preds) {
        double max_row = 0;
        double sum_exp = 0;
        for (int c = 1; c < preds.length; ++c) if (preds[c] > max_row) max_row = preds[c];
        for (int c = 1; c < preds.length; ++c) { sum_exp += (preds[c] = Math.exp(preds[c]-max_row));}
        sum_exp = 1/sum_exp;
        double max_p = 0;
        for (int c = 1; c < preds.length; ++c) if ((preds[c] *= sum_exp) > max_p) { max_p = preds[c]; preds[0] = c-1; }
        return preds;
    }
    
    double[] postPredOrdinal(double[] preds) {
        double previousCDF = 0.0;
        for (int cInd = 0; cInd < _nclasses; cInd++) { // classify row and calculate PDF of each class
            double eta = preds[cInd + 1];
            double currCDF = 1.0 / (1 + Math.exp(-eta));
            preds[cInd + 1] = currCDF - previousCDF;
            previousCDF = currCDF;

            if (eta > 0) { // found the correct class
                preds[0] = cInd;
                break;
            }
        }
        for (int cInd = (int) preds[0] + 1; cInd < _nclasses; cInd++) {  // continue PDF calculation
            double currCDF = 1.0 / (1 + Math.exp(-preds[cInd + 1]));
            preds[cInd + 1] = currCDF - previousCDF;
            previousCDF = currCDF;

        }
        preds[_nclasses] = 1-previousCDF;
        return preds;
    }
}
