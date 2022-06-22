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

import esa.mo.helpertools.connections.ConnectionConsumer;
import esa.mo.mc.impl.provider.ParameterInstance;
import esa.mo.nanomind.impl.util.NanomindServicesConsumer;
import esa.mo.nmf.NMFException;
import esa.mo.nmf.commonmoadapter.CommonMOAdapterImpl;
import esa.mo.nmf.commonmoadapter.CompleteDataReceivedListener;
import esa.mo.platform.impl.provider.gen.PowerControlAdapterInterface;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.structures.*;
import esa.opssat.nanomind.opssat_pf.structures.OnBoardDevice;
import esa.opssat.nanomind.opssat_pf.structures.OnBoardDeviceList;
import org.ccsds.moims.mo.platform.powercontrol.structures.Device;
import org.ccsds.moims.mo.platform.powercontrol.structures.DeviceList;
import org.ccsds.moims.mo.platform.powercontrol.structures.DeviceType;

public class PowerControlOPSSATAdapter implements PowerControlAdapterInterface
{

  private static final long ADCS_ACTIVATION_DELAY_NS = 60L * 1000 * 1000 * 1000;
  private static final String PDU_CHANNEL_PARAM_NAME = "PDU1952";

  enum STATUS_MASK {
    DEVICE_STATUS_SEPP1_MASK(0x0004, OnBoardDevice.SEPP1),
    DEVICE_STATUS_SEPP2_MASK(0x0008, OnBoardDevice.SEPP2),
    DEVICE_STATUS_SBAND_MASK(0x0020, OnBoardDevice.SBandTRX),
    DEVICE_STATUS_XBAND_MASK(0x0080, OnBoardDevice.XBandTRX),
    DEVICE_STATUS_SDR_MASK(0x0100, OnBoardDevice.SDR),
    DEVICE_STATUS_IADCS_MASK(0x0200, OnBoardDevice.FineADCS),
    DEVICE_STATUS_OPT_MASK(0x0400, OnBoardDevice.OpticalRX),
    DEVICE_STATUS_CAM_MASK(0x1000, OnBoardDevice.HDCamera),
    DEVICE_STATUS_GPS_MASK(0x0800, OnBoardDevice.GPS);

    int value;
    OnBoardDevice payload;

    STATUS_MASK(int value, OnBoardDevice payload) {
      this.value = value;
      this.payload = payload;
    }
  }

  private final NanomindServicesConsumer obcServicesConsumer;
  private final Map<OnBoardDevice, Device> deviceByType;
  private final Map<Long, OnBoardDevice> payloadIdByObjInstId;
  private Long adcsChannelStartTime = null;
  private static final Logger LOGGER = Logger.getLogger(PowerControlOPSSATAdapter.class.getName());

  public PowerControlOPSSATAdapter()
  {
    this.obcServicesConsumer = NanomindServicesConsumer.getInstance();
    LOGGER.log(Level.INFO, "Initialisation");
    deviceByType = new ConcurrentHashMap<>();
    payloadIdByObjInstId = new HashMap<>();
    initDevices();
  }

  private void initDevices()
  {
    addDevice(new Device(false, 0L, new Identifier(
        "Attitude Determination and Control System"), DeviceType.ADCS), OnBoardDevice.FineADCS);
    addDevice(new Device(true, 10L, new Identifier(
        "Satellite Experimental Processing Platform 1"), DeviceType.OBC), OnBoardDevice.SEPP1);
    addDevice(new Device(true, 11L, new Identifier(
        "Satellite Experimental Processing Platform 2"), DeviceType.OBC), OnBoardDevice.SEPP2);
    addDevice(new Device(false, 2L, new Identifier("S-Band Transceiver"), DeviceType.SBAND),
        OnBoardDevice.SBandTRX);
    addDevice(new Device(false, 3L, new Identifier("X-Band Transmitter"), DeviceType.XBAND),
        OnBoardDevice.XBandTRX);
    addDevice(new Device(false, 4L, new Identifier("Software Defined Radio"),
        DeviceType.SDR), OnBoardDevice.SDR);
    addDevice(new Device(false, 5L, new Identifier("Optical Receiver"), DeviceType.OPTRX),
        OnBoardDevice.OpticalRX);
    addDevice(new Device(false, 6L, new Identifier("HD Camera"), DeviceType.CAMERA),
        OnBoardDevice.HDCamera);
    addDevice(new Device(false, 7L, new Identifier("GPS"), DeviceType.GNSS), OnBoardDevice.GPS);
  }

  private void addDevice(final Device device, final OnBoardDevice payloadId)
  {
    deviceByType.put(payloadId, device);
    payloadIdByObjInstId.put(device.getUnitObjInstId(), payloadId);
  }

  @Override
  public Map<Identifier, Device> getDeviceMap()
  {
    Map<Identifier, Device> map = new HashMap<>();
    deviceByType.forEach((k, device) -> { map.put(device.getName(), device); });
    return map;
  }

  private Device findByType(final DeviceType type)
  {
    for (Device device : deviceByType.values()) {
      if (device.getDeviceType() == type) {
        return device;
      }
    }
    return null;
  }

  @Override
  public void enableDevices(final DeviceList inputList) throws IOException
  {
    synchronized (this) {
      for (final Device device : inputList) {
        LOGGER.log(Level.INFO, "Looking up Device {0}", new Object[]{device});
        OnBoardDevice payloadId = payloadIdByObjInstId.get(device.getUnitObjInstId());
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
          // When device is set OFF, mark the status right away
          if(!device.getEnabled())
          {
            Device d = findByType(device.getDeviceType());
            d.setEnabled(false);
            if(d.getDeviceType() == DeviceType.ADCS){
              adcsChannelStartTime = null;
            }
          }
        } else {
          throw new IOException("Cannot find the device by oId " + device);
        }
      }
    }
  }

  @Override
  public boolean isDeviceEnabled(DeviceType deviceType) {
    synchronized (this) {
      boolean isEnabled = findByType(deviceType) == null ? false : findByType(deviceType).getEnabled();

      // In the ADCS case, check that the power channel has been running at least a minute to get an accurate reading
      if (isEnabled && deviceType.equals(DeviceType.ADCS)) {
        if (adcsChannelStartTime == null) {
          isEnabled = false;
        } else if ((System.nanoTime() - adcsChannelStartTime) < ADCS_ACTIVATION_DELAY_NS) {
          isEnabled = false;
        }
      }
      return isEnabled;
    }
  }

  @Override
  public void startStatusTracking(ConnectionConsumer connection) {

    CompleteDataReceivedListener listener = new CompleteDataReceivedListener() {
      @Override
      public void onDataReceived(ParameterInstance parameterInstance) {
        if (parameterInstance == null || false == PDU_CHANNEL_PARAM_NAME.equals(parameterInstance.getName().getValue())) {
           return;
        }
        synchronized (this) {
          Attribute rawValue = parameterInstance.getParameterValue().getRawValue();
          if (rawValue == null) {
            return;
          }
          long rawVal =  ((UShort)rawValue).getValue();
          for(STATUS_MASK mask : STATUS_MASK.values())
          {
              boolean enabled = (rawVal & mask.value) > 0;
              boolean oldEnabled = deviceByType.get(mask.payload).getEnabled();
              if (oldEnabled && !enabled) { 
                LOGGER.log(Level.INFO, "Device " + mask + " going offline");
              } else if (!oldEnabled && enabled) {
                LOGGER.log(Level.INFO, "Device " + mask + " coming online");
              }
              deviceByType.get(mask.payload).setEnabled(enabled);
              if (mask.payload == OnBoardDevice.FineADCS) {
                if (enabled && adcsChannelStartTime == null) {
                  adcsChannelStartTime = System.nanoTime();
                } else if (!enabled && adcsChannelStartTime != null) {
                  adcsChannelStartTime = null;
                }
              }
          }
        }
      }
    };

    CommonMOAdapterImpl commonMOAdapter = new CommonMOAdapterImpl(connection);
    try {
      commonMOAdapter.toggleParametersGeneration(Arrays.asList(PDU_CHANNEL_PARAM_NAME) , true);
    } catch (NMFException e) {
      LOGGER.log(Level.WARNING, "Error enabling PDU channel parameter");
    }
    commonMOAdapter.addDataReceivedListener(listener);
    LOGGER.log(Level.INFO, "Now listening to PDU channel info");
  }

  private void switchDevice(OnBoardDevice device, Boolean enabled) throws IOException
  {
    synchronized (this) {
      OnBoardDeviceList deviceList = new OnBoardDeviceList();
      BooleanList powerStates = new BooleanList();
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

}
