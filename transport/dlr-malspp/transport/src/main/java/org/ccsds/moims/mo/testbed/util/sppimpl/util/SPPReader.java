/**
 * *****************************************************************************
 * Copyright or © or Copr. CNES
 *
 * This software is a computer program whose purpose is to provide a framework
 * for the CCSDS Mission Operations services.
 *
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 ******************************************************************************
 */
package org.ccsds.moims.mo.testbed.util.sppimpl.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacketHeader;

public class SPPReader {
    private static final Logger LOGGER = Logger.getLogger(SPPReader.class.getName());
    final protected static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private final String PROCESSED_FILENAME = "processed_apids.txt";

    private final byte[] apidQualifierBuffer;

    private final byte[] inHeaderBuffer;

    private final byte[] inCrcBuffer;
    private final InputStream is;
    private final boolean crcEnabled;
    private final APIDRangeList crcApids;
    private final APIDRangeList processedApids;
    private SpacePacket packet;
    private int errorCount;

    public SPPReader(final InputStream is) {
        errorCount = 0;
        this.is = is;
        apidQualifierBuffer = new byte[2];
        inHeaderBuffer = new byte[6];
        inCrcBuffer = new byte[2];
        crcEnabled = SPPHelper.getCrcEnabled();
        crcApids = SPPHelper.initWhitelist(new File(SPPHelper.CRC_FILENAME));
        processedApids = SPPHelper.initWhitelist(new File(PROCESSED_FILENAME));
    }

    private int read(final byte[] b, final int initialOffset, final int totalLength) throws IOException {
        int n;
        int len = 0;
        do {
            n = is.read(b, initialOffset + len, totalLength - len);
            if (n != -1) {
                len += n;
            } else {
                throw new IOException("End of input stream.");
            }
        } while (len < totalLength);
        return len;
    }

    public synchronized SpacePacket receive() throws IOException {
        final int apidQualifier;

        if (SPPHelper.isAPIDqualifierInMessage) {
            // 1- Read the APID qualifier
            read(apidQualifierBuffer, 0, 2);
            // Logger.getLogger(this.getClass().getName()).log(Level.INFO, "APID Qualifier: {0}",bytesToHex(apidQualifierBuffer));
            apidQualifier = (((apidQualifierBuffer[0] & 0xFF) << 8) | (apidQualifierBuffer[1] & 0xFF));
        } else {
            apidQualifier = SPPHelper.defaultAPIDqualifier;
        }

        final SpacePacketHeader header = new SpacePacketHeader();
        final SpacePacket outPacket = new SpacePacket(header, null, 0, 0);

        outPacket.setApidQualifier(apidQualifier);

        // 2- Read the Space Packet
        read(inHeaderBuffer, 0, 6);
        //        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Header: {0}",bytesToHex(inHeaderBuffer));
        int pk_ident = inHeaderBuffer[0] & 0xFF;
        pk_ident = (pk_ident << 8) | (inHeaderBuffer[1] & 0xFF);
        final int vers_nb = (pk_ident >> 13) & 0x0007;
        final int pkt_type = (pk_ident >> 12) & 0x0001;
        final int sec_head_flag = (pk_ident >> 11) & 0x0001;
        final int apid = pk_ident & 0x07FF;

        final boolean processCrc = crcEnabled && crcApids.inRange(apid);

        int pkt_seq_ctrl = inHeaderBuffer[2] & 0xFF;
        pkt_seq_ctrl = (pkt_seq_ctrl << 8) | (inHeaderBuffer[3] & 0xFF);
        final int segt_flag = (pkt_seq_ctrl >> 14) & 0x0003;
        final int seq_count = pkt_seq_ctrl & 0x3FFF;

        int pkt_length_value = inHeaderBuffer[4] & 0xFF;
        pkt_length_value = ((pkt_length_value << 8) | (inHeaderBuffer[5] & 0xFF)) + 1;

        if (processCrc) {
            pkt_length_value = pkt_length_value - 2;
        }
        final SpacePacketHeader sph = outPacket.getHeader();
        sph.setApid(apid);
        sph.setSecondaryHeaderFlag(sec_head_flag);
        sph.setPacketType(pkt_type);
        sph.setPacketVersionNumber(vers_nb);
        sph.setSequenceCount(seq_count);
        sph.setSequenceFlags(segt_flag);

        final int dataLength = pkt_length_value;

        if (dataLength > 65536) {
            throw new IOException("The data length cannot be bigger than 65536!");
        }

        outPacket.setLength(dataLength);
        final byte[] data = new byte[outPacket.getLength()];
        read(data, outPacket.getOffset(), dataLength);

        outPacket.setBody(data);

        if (!processedApids.inRange(apid)) {
            return null;
        }

        // Read CRC
        if (processCrc) {
            is.read(inCrcBuffer);
            int readCRC = inCrcBuffer[0] & 0xFF;
            readCRC = (readCRC << 8) | (inCrcBuffer[1] & 0xFF);
            this.packet = outPacket;
            final int CRC = SPPHelper.computeCRC(inHeaderBuffer, data, outPacket.getOffset(), dataLength);
            if (CRC != readCRC) {
                final String error = "CRC Error:" + " expected=" + CRC + ", read=" + readCRC + " for " + " APID(" +
                    apid + ")" + ", SSC=" + seq_count + "" + ", pkt_len=" + pkt_length_value + ", header=" + bytesToHex(
                        inHeaderBuffer) + ", body=" + bytesToHex(data);
                LOGGER.log(Level.WARNING, error);
                errorCount++;
                if (errorCount >= 3) {
                    throw new IOException(error);
                }
                // For singular errors - discard this packet and receive another one
                return receive();
            }
        }
        errorCount = 0;
        return outPacket;
    }

    public SpacePacket getPacket() {
        return packet;
    }

    public static String bytesToHex(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
