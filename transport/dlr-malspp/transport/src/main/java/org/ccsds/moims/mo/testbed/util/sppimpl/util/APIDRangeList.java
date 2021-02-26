/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ccsds.moims.mo.testbed.util.sppimpl.util;

import java.util.ArrayList;

/**
 * List of APID ranges used to form a composite range of possible APIDs consisting of several intervals.
 * @author yannick
 */
public class APIDRangeList extends ArrayList<APIDRange>
{
  /**
   * Checks if a specified APID is inside the range of this list.
   * @param apid The APID to check.
   * @return True iff there exists a <i>range</i> in <b>this</b> with range.inRange(apid).
   */
  public boolean inRange(final int apid){
    for(final APIDRange r : this){
      if(r.inRange(apid)){
        return true;
      }
    }
    return false;
  }
}
