/*******************************************************************************
 * Copyright or � or Copr. CNES
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

import java.util.Map;

public class SpacePacket {
  
  private SpacePacketHeader header;
  
  private int apidQualifier;
  
  private byte[] body;
  
  private int offset;
  
  private int length;
  
  private Map qosProperties;
  
  public SpacePacket(SpacePacketHeader header, byte[] body, int offset,
      int length) {
    this(header, -1, body, offset, length);
  }
  
  public SpacePacket(SpacePacketHeader header, int apidQualifier, byte[] body, int offset,
      int length) {
    super();
    this.header = header;
    this.apidQualifier = apidQualifier;
    this.body = body;
    this.offset = offset;
    this.length = length;
  }

  public void setQosProperties(Map qosProperties) {
    this.qosProperties = qosProperties;
  }

  public Map getQosProperties() {
    return qosProperties;
  }

  /**
   * @return the header
   */
  public SpacePacketHeader getHeader() {
    return header;
  }

  /**
   * @param header the header to set
   */
  public void setHeader(SpacePacketHeader header) {
    this.header = header;
  }

  public int getApidQualifier() {
  	return apidQualifier;
  }

	public void setApidQualifier(int apidQualifier) {
  	this.apidQualifier = apidQualifier;
  }

	/**
   * @return the body
   */
  public byte[] getBody() {
    return body;
  }

  /**
   * @param body the body to set
   */
  public void setBody(byte[] body) {
    this.body = body;
  }

  /**
   * @return the offset
   */
  public int getOffset() {
    return offset;
  }

  /**
   * @param offset the offset to set
   */
  public void setOffset(int offset) {
    this.offset = offset;
  }

  /**
   * @return the length
   */
  public int getLength() {
    return length;
  }

  /**
   * @param length the length to set
   */
  public void setLength(int length) {
    this.length = length;
  }
  
  public String toString() {
    return '(' + super.toString() + ",apidQualifier=" + apidQualifier
        + ",header=" + header + ",offset=" + offset + ",length=" + length + ')';
  }

}
