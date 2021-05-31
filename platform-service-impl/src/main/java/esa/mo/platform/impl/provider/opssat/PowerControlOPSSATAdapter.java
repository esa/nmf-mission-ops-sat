/* ----------------------------------------------------------------------------
 * Copyright (C) 2018      European Space Agency
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
package esa.mo.platform.impl.provider.opssat;

import esa.mo.nanomind.impl.util.NanomindServicesConsumer;
import esa.mo.platform.impl.provider.gen.PowerControlAdapterInterface;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.structures.BooleanList;
import org.ccsds.moims.mo.mal.structures.Identifier;
import esa.opssat.nanomind.opssat_pf.structures.PayloadDevice;
import esa.opssat.nanomind.opssat_pf.structures.PayloadDeviceList;
import org.ccsds.moims.mo.platform.powercontrol.structures.Device;
import org.ccsds.moims.mo.platform.powercontrol.structures.DeviceList;
import org.ccsds.moims.mo.platform.powercontrol.structures.DeviceType;

public class PowerControlOPSSATAdapter implements PowerControlAdapterInterface
{

  private final NanomindServicesConsumer obcServicesConsumer;
  private final List<Device> devices;
  private final Map<Identifier, Device> deviceByName;
  private final Map<Long, Device> deviceByObjInstId;
  private final Map<Long, PayloadDevice> payloadIdByObjInstId;
  private static final Logger LOGGER = Logger.getLogger(PowerControlOPSSATAdapter.class.getName());

  public PowerControlOPSSATAdapter(final NanomindServicesConsumer obcServicesConsumer)
  {
    this.obcServicesConsumer = obcServicesConsumer;
    LOGGER.log(Level.INFO, "Initialisation");
    devices = new ArrayList<>();
    deviceByName = new HashMap<>();
    deviceByObjInstId = new HashMap<>();
    payloadIdByObjInstId = new HashMap<>();
    initDevices();
  }

  private void initDevices()
  {
    addDevice(new Device(true, 0L, new Identifier(
        "Attitude Determination and Control System"), DeviceType.ADCS), PayloadDevice.FineADCS);
    addDevice(new Device(true, 10L, new Identifier(
        "Satellite Experimental Processing Platform 1"), DeviceType.OBC), PayloadDevice.SEPP1);
    addDevice(new Device(true, 11L, new Identifier(
        "Satellite Experimental Processing Platform 2"), DeviceType.OBC), PayloadDevice.SEPP2);
    addDevice(new Device(false, 2L, new Identifier("S-Band Transceiver"), DeviceType.SBAND),
        PayloadDevice.SBandTRX);
    addDevice(new Device(false, 3L, new Identifier("X-Band Transmitter"), DeviceType.XBAND),
        PayloadDevice.XBandTRX);
    addDevice(new Device(false, 4L, new Identifier("Software Defined Radio"),
        DeviceType.SDR), PayloadDevice.SDR);
    addDevice(new Device(false, 5L, new Identifier("Optical Receiver"), DeviceType.OPTRX),
        PayloadDevice.OpticalRX);
    addDevice(new Device(false, 6L, new Identifier("HD Camera"), DeviceType.CAMERA),
        PayloadDevice.HDCamera);
  }

  private void addDevice(final Device device, final PayloadDevice payloadId)
  {
    devices.add(device);
    deviceByName.put(device.getName(), device);
    deviceByObjInstId.put(device.getUnitObjInstId(), device);
    payloadIdByObjInstId.put(device.getUnitObjInstId(), payloadId);
  }

  @Override
  public Map<Identifier, Device> getDeviceMap()
  {
    return Collections.unmodifiableMap(deviceByName);
  }

  private Device findByType(final DeviceType type)
  {
    for (final Device device : devices) {
      if (device.getDeviceType() == type) {
        return device;
      }
    }
    return null;
  }

  @Override
  public void enableDevices(final DeviceList inputList) throws IOException
  {
    for (final Device device : inputList) {
      LOGGER.log(Level.INFO, "Looking up Device {0}", new Object[]{device});
      PayloadDevice payloadId = payloadIdByObjInstId.get(device.getUnitObjInstId());
      if (device.getUnitObjInstId() != null) {
        payloadId = payloadIdByObjInstId.get(device.getUnitObjInstId());
      } else {
        final Device found = findByType(device.getDeviceType());
        if (found != null) {
          payloadId = payloadIdByObjInstId.get(found.getUnitObjInstId());
        } else {
          throw new IOException("Cannot find the device by type " + device);
        }
      }
      if (payloadId != null) {
        switchDevice(payloadId, device.getEnabled());
      } else {
        throw new IOException("Cannot find the device by oId " + device);
      }
    }
  }

  private void switchDevice(final PayloadDevice device, final Boolean enabled) throws IOException
  {
    // TODO: Track status of the device in the device list
    final PayloadDeviceList deviceList = new PayloadDeviceList();
    final BooleanList powerStates = new BooleanList();
    deviceList.add(device);
    powerStates.add(enabled);
    LOGGER.log(Level.INFO, "Switching device {0} to enabled: {1}", new Object[]{device, enabled});
    try {
      obcServicesConsumer.getPowerNanomindService().getPowerNanomindStub().setPowerState(deviceList,
          powerStates);
    } catch (final MALInteractionException | MALException ex) {
      throw new IOException("Cannot switch device through OBC", ex);
    }
  }

}
