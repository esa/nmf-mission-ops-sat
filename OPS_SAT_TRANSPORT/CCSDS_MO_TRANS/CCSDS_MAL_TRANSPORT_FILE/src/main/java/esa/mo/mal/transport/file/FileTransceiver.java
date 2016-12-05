/* ----------------------------------------------------------------------------
 * Copyright (C) 2014      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO File Transport Framework
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
package esa.mo.mal.transport.file;

import esa.mo.mal.transport.gen.sending.GENMessageSender;
import esa.mo.mal.transport.gen.sending.GENOutgoingMessageHolder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

/**
 * File transmitter and receiver. Used by the GEN message poller class.
 */
public class FileTransceiver implements esa.mo.mal.transport.gen.util.GENMessagePoller.GENMessageReceiver<InputStream>, GENMessageSender
{
  private final Path incomingDirectory;
  private final Path outgoingDirectory;
  private final WatchService watcher;
  private final String transportString;
  private final String filenameString;
  private final boolean deleteFiles;
  private long msgCount = 0;
  private WatchKey key = null;
  private Iterator<WatchEvent<?>> events = null;

  /**
   * Constructor.
   *
   * @param incomingDirectory The directory that incoming messages will appear in.
   * @param outgoingDirectory The directory that outgoing messages will be written into.
   * @param watcher The file watcher.
   * @param transportString The filename string to match for incoming messages
   * @param filenameString The file prefix for outgoing messages
   * @param deleteFiles True if files should be auto deleted after being read.
   */
  public FileTransceiver(Path incomingDirectory, Path outgoingDirectory, WatchService watcher, String transportString, String filenameString, boolean deleteFiles)
  {
    this.incomingDirectory = incomingDirectory;
    this.outgoingDirectory = outgoingDirectory;
    this.watcher = watcher;
    this.transportString = transportString;
    this.filenameString = filenameString;
    this.deleteFiles = deleteFiles;
  }

  @Override
  public void sendEncodedMessage(GENOutgoingMessageHolder packetData) throws IOException
  {
    // create tmp file name
    String tmpname = FileTransport.FILE_PREFIX
            + packetData.getDestinationURI().substring(7)
            + "-"
            + filenameString
            + "-"
            + String.format("%07d", ++msgCount);

    java.io.File tmpFile = new File(outgoingDirectory.toFile(), tmpname + ".tmp");

    java.io.FileOutputStream fos = new FileOutputStream(tmpFile);
    try
    {
      fos.write(packetData.getEncodedMessage());
      fos.flush();
    }
    finally
    {
      fos.close();
    }

    // rename file to correct file name
    tmpFile.renameTo(new File(outgoingDirectory.toFile(), tmpname + ".msg"));
  }

  @Override
  public InputStream readEncodedMessage() throws IOException, InterruptedException
  {
    if (null == key)
    {
      // wait for key to be signalled
      key = watcher.take();
      events = key.pollEvents().iterator();
    }

    if (events.hasNext())
    {
      WatchEvent event = events.next();
      WatchEvent.Kind kind = event.kind();

      // TBD - provide example of how OVERFLOW event is handled
      if (kind == java.nio.file.StandardWatchEventKinds.OVERFLOW)
      {
        return null;
      }

      // Context for directory entry event is the file name of entry
      WatchEvent<Path> ev = (WatchEvent<Path>) event;
      Path name = ev.context();
      Path child = incomingDirectory.resolve(name);

      if (Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS))
      {
        String subname = child.getFileName().toString();
        if (subname.startsWith(transportString) && subname.endsWith(".msg"))
        {
          System.out.println("Found file : " + child.getFileName());
          try
          {
            FileChannel fc;
            if (deleteFiles)
            {
              fc = FileChannel.open(child, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE);
            }
            else
            {
              fc = FileChannel.open(child, StandardOpenOption.READ);
            }

            return Channels.newInputStream(fc);
          }
          catch (IOException ex)
          {
            ex.printStackTrace();
          }
        }
      }
    }
    else
    {
      // reset the key
      boolean valid = key.reset();
      if (!valid)
      {
        // object no longer registered
        System.out.println("Not registered : " + incomingDirectory.toString());
      }

      events = null;
      key = null;
    }

    return null;
  }

  @Override
  public void close()
  {
    // nothing to do here
  }
}
