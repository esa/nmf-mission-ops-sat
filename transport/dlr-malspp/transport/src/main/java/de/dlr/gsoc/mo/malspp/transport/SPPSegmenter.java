/* 
 * MAL/SPP Binding for CCSDS Mission Operations Framework
 * Copyright (C) 2015 Deutsches Zentrum für Luft- und Raumfahrt e.V. (DLR).
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.dlr.gsoc.mo.malspp.transport;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacket;
import org.ccsds.moims.mo.testbed.util.spp.SpacePacketHeader;

public class SPPSegmenter implements Iterator {

	private static final String TOO_SMALL = "SPACE_PACKET_SIZE_LIMIT too small to accomodate secondary header and at least one octet of user data.";
	private static final int COUNTER_LENGTH = 4; // Number of bytes for the 'Segment Counter'.
	// Position of all the flag bits in the secondary header; if present 'Source' and 'Destination
	// Identifier' follow immediately, then 'Segment Counter':
	private static final int FLAG_IDX = 20;
	private final Queue<SpacePacket[]> readyMessages = new LinkedList<>();
	private final SortedMap<Long, SpacePacket> packetStore = new TreeMap<>();
	private final SortedSet<Long> startPacketCounters = new TreeSet<>();
	private final SortedSet<Long> endPacketCounters = new TreeSet<>();
	private Long unsegmentedPacketCounter;
	private final SortedMap<Long, Long> timeouts = new TreeMap<>();
	private final long timeout;

	public SPPSegmenter(final long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Gets the value of the 'Segment Counter' field in the Space Packet's secondary header.
	 *
	 * No error checking is performed. If the Space Packet is malformed due to a body that is too
	 * short an IndexOutOfBoundsException is thrown. If this method is called on a Space Packet not
	 * containing a segment counter (because it is unsegmented) the behavior is undefined.
	 *
	 * @param spacePacket Space Packet that contains a 'Segment Counter' field.
	 * @return Value of the 'Segment Counter' field.
	 */
	private static long getSegmentCounter(final SpacePacket spacePacket) {
		final byte[] body = spacePacket.getBody();
		final byte flags = body[FLAG_IDX];
		final int counter_pos = FLAG_IDX + 1 + ((flags & 0b10000000) >>> 7) + ((flags & 0b01000000) >>> 6);

		long counter = 0;
		for (int i = 0; i < COUNTER_LENGTH; i++) {
			counter <<= 8;
			counter |= body[counter_pos + i] & 0xFF;
		}
		return counter;
	}

	private SpacePacket[] getCompleteSequence() {
		// Check if an unsegmented packet has been received or if both start and end packet are
		// present and the correct number of packets in-between has been received. Returns all
		// packets belonging to a complete sequence, null otherwise.
		if (null != unsegmentedPacketCounter) {
			final SpacePacket[] ret = new SpacePacket[]{packetStore.remove(unsegmentedPacketCounter)};
			unsegmentedPacketCounter = null;
			return ret;
		}
		if (startPacketCounters.isEmpty() || endPacketCounters.isEmpty()) {
			return null;
		}
		final long startCounter = startPacketCounters.first();
		final long endCounter = endPacketCounters.first();
		final long counterDiff = endCounter - startCounter;
		if (counterDiff < 0) {
			return null;
		}
		final SortedMap<Long, SpacePacket> subMap = packetStore.subMap(startCounter, endCounter + 1);
		if (subMap.size() == counterDiff + 1) {
			startPacketCounters.remove(startCounter);
			endPacketCounters.remove(endCounter);
			final SpacePacket[] ret = subMap.values().toArray(new SpacePacket[1]);
			subMap.clear(); // writes through to packetStore
			return ret;
		}
		return null;
	}

	private void storePacket(final SpacePacket spacePacket) {
		long counter = -1;
		final int seq = spacePacket.getHeader().getSequenceFlags();
		if (seq != 0b11) { // packet is segmented and has a counter
			counter = getSegmentCounter(spacePacket);
		}
		packetStore.put(counter, spacePacket);
		timeouts.put(System.currentTimeMillis(), counter);
		switch (seq) {
			case 0b11:
				unsegmentedPacketCounter = counter;
				break;
			case 0b01:
				startPacketCounters.add(counter);
				break;
			case 0b10:
				endPacketCounters.add(counter);
				break;
		}
	}

	private void deleteTimedOutPackets() {
		final long now = System.currentTimeMillis();
		final SortedMap<Long, Long> timedOut = timeouts.headMap(now - timeout);
		final Collection<Long> timedOutCounters = timedOut.values();
		for (final long counter : timedOutCounters) {
			packetStore.remove(counter);
			startPacketCounters.remove(counter);
			endPacketCounters.remove(counter);
		}
		timedOut.clear(); // writes through to timeouts
	}

	public void process(final SpacePacket spacePacket) {
		// TODO: Timeout can be made nicer. Here checking for timed out packets only happens
		// when a new packet is processed, which can leave the last packet in memory although it
		// might have timed out.
		if (timeout != 0) {
			deleteTimedOutPackets();
		}
		storePacket(spacePacket);
		final SpacePacket[] ready = getCompleteSequence();
		if (null != ready) {
			readyMessages.add(ready);
		}
	}

	/**
	 * If necessary, split the data in body across several Space Packets. The Space Packets primary
	 * header that is passed into the method is used as template, necessary fields (sequenceCounter
	 * and sequenceFlags) are overwritten, using the passed in sequenceCounter as starting number.
	 *
	 * @param packetDataFieldSizeLimit
	 * @param primaryHeader
	 * @param primaryApidQualifier
	 * @param secondaryHeaderPart1
	 * @param secondaryHeaderPart2
	 * @param body
	 * @param sequenceCounter
	 * @param segmentCounter
	 * @return
	 * @throws org.ccsds.moims.mo.mal.MALException
	 */
	public static SpacePacket[] split(
			final int packetDataFieldSizeLimit,
			final int primaryApidQualifier,
			final SpacePacketHeader primaryHeader,
			final byte[] secondaryHeaderPart1,
			final byte[] secondaryHeaderPart2,
			final byte[] body,
			final SPPCounter sequenceCounter,
			final SPPCounter segmentCounter) throws MALException {
		final Queue<SpacePacket> spacePackets = new LinkedList<>();

		final int sndHdrLength = secondaryHeaderPart1.length + secondaryHeaderPart2.length;
		final int userDataFieldSizeLimit;
		if (!(sndHdrLength < packetDataFieldSizeLimit)) {
			// Strictly speaking all the other cases are sufficient for determining packet splitting
			// and error cases. The case that the secondary header fills out the complete packet
			// without leaving a single byte left for the message body is fine for messages without
			// body. However, the standard (4.4.10) requires an error to be generated in this case.
			throw new MALException(TOO_SMALL);
		} else if (sndHdrLength + body.length <= packetDataFieldSizeLimit) {
			// Message fits in one packet, no segment counter needed.
			userDataFieldSizeLimit = packetDataFieldSizeLimit - sndHdrLength;
		} else if (sndHdrLength + COUNTER_LENGTH >= packetDataFieldSizeLimit) {
			// Message does not fit in one packet, but counter in secondary header (size: 4 bytes)
			// makes secondary header too large, such that no user data can be fit into a packet.
			throw new MALException(TOO_SMALL);
		} else {
			// Message does not fit in one packet, but secondary header including the segment
			// counter is small enough such that user data can be fit into the packets.
			userDataFieldSizeLimit = packetDataFieldSizeLimit - sndHdrLength - COUNTER_LENGTH;
		}

		int remaining = body.length;
		final int numberOfPackets = java.lang.Math.max((remaining - 1) / userDataFieldSizeLimit + 1, 1);
		final Iterator<Long> sequenceCounterIter = sequenceCounter.increment(numberOfPackets);
		final Iterator<Long> segmentCounterIter = (numberOfPackets > 1) ? segmentCounter.increment(numberOfPackets) : null;

		int offset = 0;
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		do {
			// combine secondary header and MAL message body to space packet body
			os.write(secondaryHeaderPart1, 0, secondaryHeaderPart1.length);
			// only write segment counter if message does not fit in a single packet
			if (numberOfPackets > 1) {
				final long c = segmentCounterIter.next(); // segmentCounterIter cannot be null here
				for (int i = COUNTER_LENGTH - 1; i >= 0; i--) {
					os.write((byte) (c >>> (i * 8)));
				}
			}
			os.write(secondaryHeaderPart2, 0, secondaryHeaderPart2.length);
			final int segmentLength = java.lang.Math.min(userDataFieldSizeLimit, remaining);
			os.write(body, offset, segmentLength);
			remaining -= segmentLength;

			// find out correct sequence flags according to offset and remaining bytes
			int sequenceFlags = 0b00; // continuation segment
			if (offset == 0 && remaining == 0) {
				sequenceFlags = 0b11; // unsegmented
			} else if (offset == 0) {
				sequenceFlags = 0b01; // first segment
			} else if (remaining == 0) {
				sequenceFlags = 0b10; // last segment
			}
			offset += segmentLength;

			// clone template primary header and change relevant values
			final SpacePacketHeader spHeader = new SpacePacketHeader(
					primaryHeader.getPacketVersionNumber(),
					primaryHeader.getPacketType(),
					primaryHeader.getSecondaryHeaderFlag(),
					primaryHeader.getApid(),
					sequenceFlags,
					sequenceCounterIter.next().shortValue()
			);

			// create space packet
			final SpacePacket spacePacket = new SpacePacket(spHeader, primaryApidQualifier, os.toByteArray(), 0, os.size());
			spacePackets.add(spacePacket);
			os.reset();
		} while (remaining > 0);

		return spacePackets.toArray(new SpacePacket[1]);
	}

	@Override
	public boolean hasNext() {
		return !readyMessages.isEmpty();
	}

	@Override
	public SpacePacket[] next() {
		return readyMessages.remove();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Not supported.");
	}
}
