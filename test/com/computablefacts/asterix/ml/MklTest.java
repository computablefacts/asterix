package com.computablefacts.asterix.ml;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import smile.math.blas.BLAS;
import smile.math.blas.EVDJob;
import smile.math.blas.LAPACK;
import smile.math.blas.Layout;

@Ignore("Disable because TravisCI randomly crashes when running these tests.")
public class MklTest {

  @Test
  public void testBlasIsSetup() {
    Assert.assertEquals("smile.math.blas.mkl.MKL", BLAS.getInstance().getClass().getName());
    Assert.assertEquals(2.0, BLAS.getInstance().asum(new double[]{1.0, 1.0}), 0.0);
  }

  @Test
  public void testLapackIsSetup() {

    Assert.assertEquals("smile.math.blas.mkl.MKL", LAPACK.getInstance().getClass().getName());

    double[] A = new double[]{3.0, -3.0, 1.0, 1.0};
    double[] WR = new double[2];
    double[] WI = new double[2];
    double[] VR = new double[2 * 2];
    double[] VL = new double[2 * 2];

    int info = LAPACK.getInstance()
        .geev(Layout.ROW_MAJOR, EVDJob.VECTORS, EVDJob.NO_VECTORS, 2, A, 2, WR, WI, VL, 2, VR, 2);

    Assert.assertEquals(0, info);
    Assert.assertArrayEquals(new double[]{2.0, 2.0}, WR, 0.000001);
    Assert.assertArrayEquals(new double[]{Math.sqrt(2.0), -Math.sqrt(2.0)}, WI, 0.000001);
  }
}
