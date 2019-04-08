/* ----------------------------------------------------------------------------
 * Copyright (C) 2018      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
 * ----------------------------------------------------------------------------
 * Licensed under the European Space Agency Public License, Version 2.0
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

import esa.mo.com.impl.util.GMVServicesConsumer;
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

  private final GMVServicesConsumer gmvServicesConsumer;
  private final List<Device> devices;
  private final Map<Identifier, Device> deviceByName;
  private final Map<Long, Device> deviceByObjInstId;
  private final Map<Long, PayloadDevice> payloadIdByObjInstId;

  public PowerControlOPSSATAdapter(GMVServicesConsumer gmvServicesConsumer)
  {
    this.gmvServicesConsumer = gmvServicesConsumer;
    Logger.getLogger(PowerControlOPSSATAdapter.class.getName()).log(Level.INFO, "Initialisation");
    devices = new ArrayList<>();
    deviceByName = new HashMap<>();
    deviceByObjInstId = new HashMap<>();
    payloadIdByObjInstId = new HashMap<>();
    initDevices();
  }

  private void initDevices()
  {
    addDevice(new Device(true, new Long(0), new Identifier(
        "Attitude Determination and Control System"), DeviceType.ADCS), PayloadDevice.FineADCS);
    addDevice(new Device(true, new Long(10), new Identifier(
        "Satellite Experimental Processing Platform 1"), DeviceType.OBC), PayloadDevice.SEPP1);
    addDevice(new Device(true, new Long(11), new Identifier(
        "Satellite Experimental Processing Platform 2"), DeviceType.OBC), PayloadDevice.SEPP2);
    addDevice(new Device(false, new Long(2), new Identifier("S-Band Transceiver"), DeviceType.SBAND),
        PayloadDevice.SBandTRX);
    addDevice(new Device(false, new Long(3), new Identifier("X-Band Transmitter"), DeviceType.XBAND),
        PayloadDevice.XBandTRX);
    addDevice(new Device(false, new Long(4), new Identifier("Software Defined Radio"),
        DeviceType.SDR), PayloadDevice.SDR);
    addDevice(new Device(false, new Long(5), new Identifier("Optical Receiver"), DeviceType.OPTRX),
        PayloadDevice.OpticalRX);
    addDevice(new Device(false, new Long(6), new Identifier("HD Camera"), DeviceType.CAMERA),
        PayloadDevice.HDCamera);
  }

  private void addDevice(Device device, PayloadDevice payloadId)
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

  private Device findByType(DeviceType type)
  {
    for (Device device : devices) {
      if (device.getDeviceType() == type) {
        return device;
      }
    }
    return null;
  }

  @Override
  public void enableDevices(DeviceList inputList) throws IOException
  {
    for (Device device : inputList) {
      PayloadDevice payloadId = payloadIdByObjInstId.get(device.getUnitObjInstId());
      if (device.getUnitObjInstId() != null) {
        payloadId = payloadIdByObjInstId.get(device.getUnitObjInstId());
      } else {
        Device found = findByType(device.getDeviceType());
        if (found != null) {
          payloadId = payloadIdByObjInstId.get(found.getUnitObjInstId());
        } else {
          throw new IOException("Cannot find the device.");
        }
      }
      if (payloadId != null) {
        switchDevice(payloadId, device.getEnabled());
      } else {
        throw new IOException("Cannot find the payload id.");
      }
    }
  }

  private void switchDevice(PayloadDevice device, Boolean enabled) throws IOException
  {
    PayloadDeviceList deviceList = new PayloadDeviceList();
    BooleanList powerStates = new BooleanList();
    deviceList.add(device);
    powerStates.add(enabled);
    try {
      gmvServicesConsumer.getPowerNanomindService().getPowerNanomindStub().setPowerState(deviceList,
          powerStates);
    } catch (MALInteractionException | MALException ex) {
      Logger.getLogger(PowerControlOPSSATAdapter.class.getName()).log(Level.SEVERE, null, ex);
      throw new IOException("Cannot switch device through OBSW", ex);
    }
  }

}
