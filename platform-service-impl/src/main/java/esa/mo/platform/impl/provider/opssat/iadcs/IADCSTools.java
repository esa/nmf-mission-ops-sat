/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
 * ----------------------------------------------------------------------------
 * Licensed under European Space Agency Public License (ESA-PL) Weak Copyleft – v2.4
 * You may not use this file except in compliance with the License.
 *
 * Except as expressly set forth in this License, the Software is provided to
 * You on an "as is" basis and without warranties of any kind, including without
 * limitation merchantability, fitness for a particular purpose, absence of
 * defects or errors, accuracy or non-infringement of intellectual property rights.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * ----------------------------------------------------------------------------
 */
package esa.mo.platform.impl.provider.opssat.iadcs;

import org.ccsds.moims.mo.mal.structures.FloatList;
import org.ccsds.moims.mo.platform.autonomousadcs.structures.MagnetorquersState;
import org.ccsds.moims.mo.platform.autonomousadcs.structures.Quaternion;
import org.ccsds.moims.mo.platform.autonomousadcs.structures.WheelsSpeed;
import org.ccsds.moims.mo.platform.structures.VectorF3D;

import at.tugraz.ihf.opssat.iadcs.*;

public class IADCSTools {

  public static SEPP_IADCS_API_MATRIX3_FLOAT convertToApiMatrix(final float M11, final float M12, final float M13,
      final float M21, final float M22, final float M23, final float M31, final float M32, final float M33) {
    final SEPP_IADCS_API_MATRIX3_FLOAT ret = new SEPP_IADCS_API_MATRIX3_FLOAT();
    ret.setM11(M11);
    ret.setM12(M12);
    ret.setM13(M13);
    ret.setM21(M21);
    ret.setM22(M22);
    ret.setM23(M23);
    ret.setM31(M31);
    ret.setM32(M32);
    ret.setM33(M33);
    return ret;
  }

  public static WheelsSpeed convertFromApiWheelSpeed(final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in1,
      final SEPP_IADCS_API_VECTOR3_UVW_FLOAT in2) {
    final FloatList list = new FloatList(6);
    list.add(in1.getX());
    list.add(in1.getY());
    list.add(in1.getZ());
    list.add(in2.getU());
    list.add(in2.getV());
    list.add(in2.getW());
    return new WheelsSpeed(list);
  }

  public static VectorF3D convertFromApiMagMoment(final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in) {
    // Moment is provided in A*m^2
    return new VectorF3D((float) in.getX(), (float) in.getY(), (float) in.getZ());
  }

  public static VectorF3D convertFromApiVector(final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT in) {
    return new VectorF3D(in.getX(), in.getY(), in.getZ());
  }

  public static SEPP_IADCS_API_VECTOR3_XYZ_FLOAT convertToApiVector(final VectorF3D vec) {
    final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT ret = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
    ret.setX(vec.getX());
    ret.setY(vec.getY());
    ret.setZ(vec.getZ());
    return ret;
  }

  public static SEPP_IADCS_API_VECTOR3_XYZ_FLOAT convertToApiVector(final float x, final float y, final float z) {
    final SEPP_IADCS_API_VECTOR3_XYZ_FLOAT ret = new SEPP_IADCS_API_VECTOR3_XYZ_FLOAT();
    ret.setX(x);
    ret.setY(y);
    ret.setZ(z);
    return ret;
  }

  public static SEPP_IADCS_API_QUATERNION_FLOAT convertToApiQuaternion(final float q, final float i, final float j,
      final float k) {
    final SEPP_IADCS_API_QUATERNION_FLOAT ret = new SEPP_IADCS_API_QUATERNION_FLOAT();
    ret.setQ(q);
    ret.setQ_I(i);
    ret.setQ_J(j);
    ret.setQ_K(k);
    return ret;
  }

  public static Quaternion convertFromApiQuaternion(final SEPP_IADCS_API_QUATERNION_FLOAT in) {
    return new Quaternion(in.getQ(), in.getQ_I(), in.getQ_J(), in.getQ_K());
  }

  public static MagnetorquersState convertApiMtqState(final long in) {
    final int mappedOrdinal;
    switch ((int) in) {
    case 1:
      mappedOrdinal = MagnetorquersState._ACTIVE_INDEX;
      break;
    case 2:
      mappedOrdinal = MagnetorquersState._SUSPEND_INDEX;
      break;
    case 0:
    default:
      mappedOrdinal = MagnetorquersState._INACTIVE_INDEX;
      break;
    }
    return MagnetorquersState.fromOrdinal(mappedOrdinal);
  }

}