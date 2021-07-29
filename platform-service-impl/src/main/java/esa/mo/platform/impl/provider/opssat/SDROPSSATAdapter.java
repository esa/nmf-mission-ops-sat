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

import at.tugraz.ihf.opssat.sdr.SEPP_SDR_API;
import at.tugraz.ihf.opssat.sdr.eSDR_RFFE_INPUT;
import at.tugraz.ihf.opssat.sdr.eSDR_RFFE_RX_LPF_BW;
import at.tugraz.ihf.opssat.sdr.eSDR_RFFE_RX_SAMPLING_FREQ;
import esa.mo.platform.impl.provider.gen.SoftwareDefinedRadioAdapterInterface;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.structures.FloatList;
import org.ccsds.moims.mo.platform.softwaredefinedradio.structures.IQComponents;
import org.ccsds.moims.mo.platform.softwaredefinedradio.structures.SDRConfiguration;

public class SDROPSSATAdapter implements SoftwareDefinedRadioAdapterInterface
{

  private static final Logger LOGGER = Logger.getLogger(SDROPSSATAdapter.class.getName());
  private static final float FREQ_MATCH_EPSILON = (float) 0.01; // 0.01 MHz = 10 kHz
  private final Map<Float, eSDR_RFFE_RX_SAMPLING_FREQ> samplingFreqsMap;
  private final Map<Float, eSDR_RFFE_RX_LPF_BW> lpfFreqsMap;
  private SEPP_SDR_API sdrApi;
  private ByteBuffer sampleBuffer;
  private int bufferSize, bufferLength;

  private final boolean initialized;
  private boolean configured;

  public SDROPSSATAdapter()
  {
    LOGGER.log(Level.INFO, "Initialisation");
    samplingFreqsMap = new TreeMap<>();
    samplingFreqsMap.put((float) 1.50, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_1M5);
    samplingFreqsMap.put((float) 1.75, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_1M75);
    samplingFreqsMap.put((float) 2.50, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_2M5);
    samplingFreqsMap.put((float) 3.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_3M);
    samplingFreqsMap.put((float) 3.84, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_3M84);
    samplingFreqsMap.put((float) 5.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_5M);
    samplingFreqsMap.put((float) 5.50, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_5M5);
    samplingFreqsMap.put((float) 6.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_6M);
    samplingFreqsMap.put((float) 7.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_7M);
    samplingFreqsMap.put((float) 8.75, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_8M75);
    samplingFreqsMap.put((float) 10.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_10M);
    samplingFreqsMap.put((float) 12.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_12M);
    samplingFreqsMap.put((float) 14.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_14M);
    samplingFreqsMap.put((float) 20.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_20M);
    samplingFreqsMap.put((float) 24.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_24M);
    samplingFreqsMap.put((float) 28.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_28M);
    samplingFreqsMap.put((float) 32.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_32M);
    samplingFreqsMap.put((float) 36.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_36M);
    samplingFreqsMap.put((float) 40.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_40M);
    samplingFreqsMap.put((float) 60.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_60M);
    samplingFreqsMap.put((float) 76.80, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_76M8);
    samplingFreqsMap.put((float) 80.00, eSDR_RFFE_RX_SAMPLING_FREQ.RFFE_RX_SAMPLING_80M);
    lpfFreqsMap = new TreeMap<>();
    lpfFreqsMap.put((float) 14.000, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_14M);
    lpfFreqsMap.put((float) 10.000, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_10M);
    lpfFreqsMap.put((float) 7.000, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_7M);
    lpfFreqsMap.put((float) 6.000, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_6M);
    lpfFreqsMap.put((float) 5.000, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_5M);
    lpfFreqsMap.put((float) 4.375, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_4M375);
    lpfFreqsMap.put((float) 3.500, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_3M5);
    lpfFreqsMap.put((float) 3.000, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_3M);
    lpfFreqsMap.put((float) 2.750, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_2M75);
    lpfFreqsMap.put((float) 2.500, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_2M5);
    lpfFreqsMap.put((float) 1.920, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_1M92);
    lpfFreqsMap.put((float) 1.500, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_1M5);
    lpfFreqsMap.put((float) 1.375, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_1M375);
    lpfFreqsMap.put((float) 1.250, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_1M25);
    lpfFreqsMap.put((float) 0.875, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_0M875);
    lpfFreqsMap.put((float) 0.750, eSDR_RFFE_RX_LPF_BW.RFFE_RX_LPFBW_0M75);
    try {
      System.loadLibrary("sdr_api_jni");
      synchronized(this) {
        sdrApi = new SEPP_SDR_API();
        sdrApi.Print_Info();
        sdrApi.Set_RF_Frontend_Input(eSDR_RFFE_INPUT.RFFE_INPUT_1);
        sdrApi.Enable_Receiver();
      }
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, "SDR API could not be initialized!", ex);
      this.initialized = false;
      return;
    }
    this.initialized = true;
  }

  @Override
  public boolean isUnitAvailable()
  {
    return initialized;
  }

  private eSDR_RFFE_RX_SAMPLING_FREQ getSamplingFreqFromFloat(final float input)
  {
    final Iterator it = samplingFreqsMap.entrySet().iterator();
    while (it.hasNext()) {
      final Map.Entry<Float, eSDR_RFFE_RX_SAMPLING_FREQ> pair = (Map.Entry) it.next();
      if (Math.abs(pair.getKey() - input) < FREQ_MATCH_EPSILON) {
        return pair.getValue();
      }
    }
    return null;
  }

  private eSDR_RFFE_RX_LPF_BW getLPFFreqFromFloat(final float input)
  {
    final Iterator it = lpfFreqsMap.entrySet().iterator();
    while (it.hasNext()) {
      final Map.Entry<Float, eSDR_RFFE_RX_LPF_BW> pair = (Map.Entry) it.next();
      if (Math.abs(pair.getKey() - input) < FREQ_MATCH_EPSILON) {
        return pair.getValue();
      }
    }
    return null;
  }

  @Override
  public boolean setConfiguration(final SDRConfiguration configuration)
  {
    synchronized(this) {
      LOGGER.log(Level.INFO, "Setting SDR configuration: {0}", configuration);
      final eSDR_RFFE_RX_SAMPLING_FREQ samplingFreq = getSamplingFreqFromFloat(
          configuration.getRxSamplingFrequency());
      final eSDR_RFFE_RX_LPF_BW lpfBw = getLPFFreqFromFloat(configuration.getRxLowPassBW());
      if (samplingFreq == null) {
        LOGGER.log(Level.WARNING, "Unsupported sampling frequency provided: {0} MHz",
            configuration.getRxSamplingFrequency());
        return false;
      }
      try {
        sdrApi.Set_RX_Gain_in_dB(configuration.getRxGain());
        sdrApi.Set_RX_Carrier_Frequency_in_GHz(configuration.getRxCarrierFrequency() / 1000);
        sdrApi.Set_RX_Sampling_Frequency(samplingFreq);
        sdrApi.Set_RXLPF_Bandwidth(lpfBw);
        sdrApi.Calibrate_RF_Frontend();
        // This will allocate plenty of memory, allowing to store samples from 0.1 second. Each sample pair is 2 x uint32
        bufferLength = (int) (configuration.getRxSamplingFrequency() * 100000);
        bufferSize = bufferLength * 8;
        sampleBuffer = ByteBuffer.allocateDirect(bufferSize);
      } catch (final Exception ex) {
        LOGGER.log(Level.WARNING, "Setting SDR configuration " + configuration + " failed", ex);
        return false;
      }
      configured = true;
      return true;
    }
  }

  @Override
  public boolean enableSDR(final Boolean enable)
  {
    synchronized(this) {
      LOGGER.log(Level.INFO, "EnableSDR: {0}", enable);
      if (!configured) {
        return false;
      }
      try {
        sdrApi.Enable_RX_Sampling_Clock();
      } catch (final Exception ex) {
        LOGGER.log(Level.SEVERE, "Enabling the SDR failed", ex);
        return false;
      }
      return true;
    }
  }

  @Override
  public IQComponents getIQComponents()
  {
    synchronized(this) {
      if (!configured) {
        return null;
      }
      final FloatList iList = new FloatList(bufferLength);
      final FloatList qList = new FloatList(bufferLength);

      final int[] ints = new int[bufferLength];
      sdrApi.Receive_IQ_Samples(ints, bufferSize);
      final IntBuffer tempBuffer = sampleBuffer.asIntBuffer();
      tempBuffer.put(ints);
      for (int i = 0; i < bufferLength; ++i) {
        iList.add((float) (sampleBuffer.getInt() & 0xFFFFFFFFL)); // remove sign extension
        qList.add((float) (sampleBuffer.getInt() & 0xFFFFFFFFL));
      }
      return new IQComponents(iList, qList);
    }
  }
}
