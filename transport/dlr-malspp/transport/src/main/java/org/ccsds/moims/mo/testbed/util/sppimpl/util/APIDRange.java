/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ccsds.moims.mo.testbed.util.sppimpl.util;

/**
 * This class models a range of APIDs as a closed interval [minAPID, maxAPID].
 * @author yannick
 */
public class APIDRange
{
  private final int min;
  private final int max;

  public APIDRange(final int min, final int max){
    this.min = min;
    this.max = max;
  }

  /**
   * Checks if an APID is inside the range of this object.
   * @param apid The APID to check.
   * @return True iff apid in [min, max].
   */
  public boolean inRange(final int apid){
    return apid >= min && apid <= max;
  }
}
