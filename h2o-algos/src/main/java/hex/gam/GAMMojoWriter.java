package hex.gam;


import hex.ModelMojoWriter;
import hex.glm.GLMModel;

import java.io.IOException;
import java.nio.ByteBuffer;

public class GAMMojoWriter extends ModelMojoWriter<GAMModel, GAMModel.GAMParameters, GAMModel.GAMModelOutput> {
  @Override
  public String mojoVersion() {
    return "1.00";
  }
  
  public GAMMojoWriter(GAMModel model) {
    super(model);
  }

  @Override
  protected void writeModelData() throws IOException {
    writekv("use_all_factor_levels", model._parms._use_all_factor_levels);
    writekv("cats", model._output._dinfo._cats);
    writekv("cat_offsets", model._output._dinfo._catOffsets);
    writekv("nums", model._output._dinfo._nums);

    boolean imputeMeans = model._parms.missingValuesHandling().equals(GLMModel.GLMParameters.MissingValuesHandling.MeanImputation);
    writekv("mean_imputation", imputeMeans);
    if (imputeMeans) {
      writekv("numNAFills", model._output._dinfo.numNAFill());
      writekv("catNAFills", model._output._dinfo.catNAFill());
    }

    writekv("beta", model._output._model_beta); // 
    writekv("family", model._parms._family);
    writekv("link", model._parms._link);

    if (model._parms._family.equals(GLMModel.GLMParameters.Family.tweedie))
      writekv("tweedie_link_power", model._parms._tweedie_link_power);
    // add GAM specific parameters
    writekv("num_knots", model._parms._num_knots); // an array
    writeStringArrays(model._parms._gam_columns, "gam_columns");
    for (int cInd=0; cInd < model._parms._gam_columns.length; cInd++)
      writeStringArrays(model._output._gamColNames[cInd], "gamColNames_"+model._parms._gam_columns[cInd]);
    writekv("bs", model._parms._bs);  // an array of choice of spline functions
    writeDoubleArray(model._output._knots, "knots");
    int countGamCols = 0;
    for (String gamCol : model._parms._gam_columns) {
      writeDoubleArray(model._output._zTranspose[countGamCols], gamCol+"_zTranspose");      // write zTranspose
      writeDoubleArray(model._output._binvD[countGamCols++], gamCol+"_binvD");      // write binvD
    }
    // store variable importance information
  }
  
  public void writeStringArrays(String[] sArrays, String title) throws IOException {
    startWritingTextFile(title);
    for (String sName : sArrays) {
      writeln(sName);
    }
    finishWritingTextFile();
  }

  public void writeDoubleArray(double[][] array, String title) throws IOException {
    int totArraySize = 0;
    for (double[] row : array) 
      totArraySize += row.length;
    
    ByteBuffer bb = ByteBuffer.wrap(new byte[totArraySize * 8]);
    for (double[] row : array)
      for (double val : row)
        bb.putDouble(val);
    writeblob(title, bb.array());
  }
}
