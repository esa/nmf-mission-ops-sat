/** *****************************************************************************
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
 ****************************************************************************** */
package org.ccsds.moims.mo.testbed.util.sppimpl.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacketHeader;

public class SPPWriter
{

  private final byte[] apidQualifierBuffer;

  private final byte[] outHeaderBuffer;

  private final byte[] outCrcBuffer;
  private final boolean crcEnabled;
  private final APIDRangeList crcApids;

  //private Hashtable sequenceCounters;
  private final OutputStream os;

  public SPPWriter(OutputStream os)
  {
    this.os = os;
    apidQualifierBuffer = new byte[2];
    outHeaderBuffer = new byte[6];
    outCrcBuffer = new byte[2];
    crcEnabled = SPPHelper.getCrcEnabled();
    crcApids = SPPHelper.initWhitelist(new File(SPPHelper.CRC_FILENAME));
    //sequenceCounters = new Hashtable();
  }

  public synchronized void send(SpacePacket packet) throws IOException
  {

    if (SPPHelper.isAPIDqualifierInMessage) {
      // 1- Write the APID qualifier
      int apidQualifier = packet.getApidQualifier();
      apidQualifierBuffer[0] = (byte) (apidQualifier >>> 8);
      apidQualifierBuffer[1] = (byte) (apidQualifier >>> 0);
      os.write(apidQualifierBuffer);
    }

    // 2- Write the Space Packet
    SpacePacketHeader sph = packet.getHeader();
    byte[] data = packet.getBody();
    int vers_nb = sph.getPacketVersionNumber();
    int pkt_type = sph.getPacketType();
    int sec_head_flag = sph.getSecondaryHeaderFlag();
    int TCPacket_apid = sph.getApid();
    int segt_flag = sph.getSequenceFlags();
    int pkt_ident = (vers_nb << 13) | (pkt_type << 12) | (sec_head_flag << 11) | (TCPacket_apid);

    Integer apid = new Integer(TCPacket_apid);

    /* The sequence counter should be assigned by the upper layer
    Integer counter = (Integer) sequenceCounters.get(apid);
    if (counter == null) {
      counter = new Integer(0);
      sequenceCounters.put(apid, counter);
    }*/
    int pkt_seq_ctrl = (segt_flag << 14)
        | (packet.getHeader().getSequenceCount());
    boolean processCrc = crcEnabled && crcApids.inRange(apid);
    // Remove 1 byte as specified by the specification.
//    int pkt_length_value = packet.getLength() - 1;
    int pkt_length_value = (processCrc) ? packet.getLength() - 1 + 2
        : packet.getLength() - 1;  // + 2 because of the appended CRC

    outHeaderBuffer[0] = (byte) (pkt_ident >> 8);
    outHeaderBuffer[1] = (byte) (pkt_ident & 0xFF);
    outHeaderBuffer[2] = (byte) (pkt_seq_ctrl >> 8);
    outHeaderBuffer[3] = (byte) (pkt_seq_ctrl & 0xFF);
    outHeaderBuffer[4] = (byte) (pkt_length_value >> 8);
    outHeaderBuffer[5] = (byte) (pkt_length_value & 0xFF);

    os.write(outHeaderBuffer);
    os.write(data, packet.getOffset(), packet.getLength());

    // There is no CRC in the SPP specification
    if (processCrc) {
      int CRC = SPPHelper.computeCRC(outHeaderBuffer, data,
          packet.getOffset(), packet.getLength());

      outCrcBuffer[0] = (byte) (CRC >> 8);
      outCrcBuffer[1] = (byte) (CRC & 0xFF);
      os.write(outCrcBuffer);
    }

    os.flush();

    //counter = new Integer(counter.intValue() + 1);
  }
}
