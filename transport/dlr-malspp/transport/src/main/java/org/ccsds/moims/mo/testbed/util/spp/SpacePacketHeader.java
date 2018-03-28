/*******************************************************************************
 * Copyright or © or Copr. CNES
 *
 * This software is a computer program whose purpose is to provide a 
 * framework for the CCSDS Mission Operations services.
 *
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 *******************************************************************************/
package org.ccsds.moims.mo.testbed.util.spp;

public class SpacePacketHeader {
  
  private int packetVersionNumber;
  
  private int packetType;
  
  private int secondaryHeaderFlag;
  
  private int apid;
  
  private int sequenceFlags;
  
  private int sequenceCount;
  
  public SpacePacketHeader() {}

  public SpacePacketHeader(int packetVersionNumber, int packetType,
      int secondaryHeaderFlag, int apid, int sequenceFlags, int sequenceCount) {
    super();
    this.packetVersionNumber = packetVersionNumber;
    this.packetType = packetType;
    this.secondaryHeaderFlag = secondaryHeaderFlag;
    this.apid = apid;
    this.sequenceFlags = sequenceFlags;
    this.sequenceCount = sequenceCount;
  }

  /**
   * @return the packetVersionNumber
   */
  public int getPacketVersionNumber() {
    return packetVersionNumber;
  }

  /**
   * @param packetVersionNumber the packetVersionNumber to set
   */
  public void setPacketVersionNumber(int packetVersionNumber) {
    this.packetVersionNumber = packetVersionNumber;
  }

  /**
   * @return the secondaryHeaderFlag
   */
  public int getSecondaryHeaderFlag() {
    return secondaryHeaderFlag;
  }

  /**
   * @param secondaryHeaderFlag the secondaryHeaderFlag to set
   */
  public void setSecondaryHeaderFlag(int secondaryHeaderFlag) {
    this.secondaryHeaderFlag = secondaryHeaderFlag;
  }

  /**
   * @return the apid
   */
  public int getApid() {
    return apid;
  }

  /**
   * @param apid the apid to set
   */
  public void setApid(int apid) {
    this.apid = apid;
  }

  /**
   * @return the spType
   */
  public int getPacketType() {
    return packetType;
  }

  /**
   * @param spType the spType to set
   */
  public void setPacketType(int packetType) {
    this.packetType = packetType;
  }
  
  /**
   * @return the sequenceFlag
   */
  public int getSequenceFlags() {
    return sequenceFlags;
  }

  /**
   * @param sequenceFlag the sequenceFlag to set
   */
  public void setSequenceFlags(int sequenceFlags) {
    this.sequenceFlags = sequenceFlags;
  }

  /**
   * @return the sequenceCount
   */
  public int getSequenceCount() {
    return sequenceCount;
  }

  /**
   * @param sequenceCount the sequenceCount to set
   */
  public void setSequenceCount(int sequenceCount) {
    this.sequenceCount = sequenceCount;
  }
  /*
  public SpacePacketHeader clone() {
  	return new SpacePacketHeader(packetVersionNumber, packetType,
        secondaryHeaderFlag, apid, sequenceFlags, sequenceCount);
  }*/
 
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(super.toString());
    buf.append(",packetVersionNumber=" + packetVersionNumber);
    buf.append(",packetType=" + packetType);
    buf.append(",secondaryHeaderFlag=" + secondaryHeaderFlag);
    buf.append(",apid=" + apid);
    buf.append(",sequenceFlags=" + sequenceFlags);
    buf.append(",sequenceCount=" + sequenceCount);
    return buf.toString();
  }

}
