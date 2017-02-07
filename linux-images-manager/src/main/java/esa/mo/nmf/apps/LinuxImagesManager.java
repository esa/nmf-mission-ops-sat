/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
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
package esa.mo.nmf.apps;

import com.alexkasko.delta.DirDeltaCreator;
import com.github.stephenc.javaisotools.iso9660.ConfigException;
import com.github.stephenc.javaisotools.iso9660.ISO9660RootDirectory;
import com.github.stephenc.javaisotools.iso9660.impl.CreateISO;
import com.github.stephenc.javaisotools.iso9660.impl.ISO9660Config;
import com.github.stephenc.javaisotools.iso9660.impl.ISOImageFileHandler;
import com.github.stephenc.javaisotools.sabre.HandlerException;
import com.github.stephenc.javaisotools.sabre.StreamHandler;
import com.nothome.delta.Delta;
import com.nothome.delta.DiffWriter;
import com.nothome.delta.GDiffPatcher;
import com.nothome.delta.GDiffWriter;
import esa.mo.nmf.MCRegistration;
import esa.mo.nmf.MonitorAndControlNMFAdapter;
import esa.mo.nmf.NMFException;
import esa.mo.nmf.NanoSatMOFrameworkInterface;
import esa.mo.nmf.nanosatmoconnector.NanoSatMOConnectorImpl;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.ccsds.moims.mo.mal.provider.MALInteraction;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mc.structures.AttributeValueList;

/**
 * This class provides a simple Hello World demo cli provider
 *
 */
public class LinuxImagesManager {

    private final NanoSatMOFrameworkInterface nanoSatMOFramework = new NanoSatMOConnectorImpl(new MCAdapter());

    public LinuxImagesManager() {
        //        boolean valid = runCommand("bgfbf");
        //        boolean valid = runCommand("ping google.pt");
/*
        File oldDirectory = new File("/home/root/Software_Management/Mount_Points/Slot_A/home/");
        File newDirectory = new File("/home/root/Software_Management/Mount_Points/Slot_B/home/");
        File patchFile = new File("/home/root/Software_Management/Mount_Points/Repository/Patches/SlotB-SlotA.diff");

        dirDeltaCreator(oldDirectory, newDirectory, patchFile);
         */

    }

    private void create_iso_image() {
        try {
            // Output file
            File outfile = new File("/home/root/repository/Full_Images/lala.iso");

            // Directory hierarchy, starting from the root
            ISO9660RootDirectory.MOVED_DIRECTORIES_STORE_NAME = "/home/root/repository/";
            ISO9660RootDirectory root = new ISO9660RootDirectory();

            // Add directory
            File file = new File("here/");
            if (file.exists()) {
                if (file.isDirectory()) {
                    try {
                        root.addContentsRecursively(file);
                    } catch (HandlerException ex) {
                        Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            // ISO9660 support
            ISO9660Config iso9660Config = new ISO9660Config();
            iso9660Config.allowASCII(false);
            iso9660Config.setInterchangeLevel(1);
            iso9660Config.restrictDirDepthTo8(true);
            iso9660Config.setPublisher("ESA");
            iso9660Config.setVolumeID("ISO Test");
            iso9660Config.setDataPreparer("Jens Hatlak");
            iso9660Config.forceDotDelimiter(true);

            // Create ISO
            StreamHandler streamHandler = new ISOImageFileHandler(outfile);
            CreateISO iso = new CreateISO(streamHandler, root);
            iso.process(iso9660Config, null, null, null);
            System.out.println("Done. File is: " + outfile);

            Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.INFO, "Successfully created!");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (HandlerException ex) {
            Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConfigException ex) {
            Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
/*
    private void nothome_delta() {
        File test1File = new File("/home/root/repository/Full_Images/mmcblk05_clone.img");
        File test2File = new File("/home/root/repository/Full_Images/mmcblk05_clone3.img");

        try {
            this.doTest(test1File, test2File, 4 * 1024);
        } catch (IOException ex) {
            Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
*/
    /*
    private void jbsdiff_diff() {

        File oldFile = new File("/home/root/repository/Full_Images/mmcblk05_clone.img");
        File newFile = new File("/home/root/repository/Full_Images/mmcblk05_clone3.img");
        File patchFile = new File("/home/root/repository/Full_Images/jbsdiff.diff");

        String compression = System.getProperty("jbsdiff.compressor", "bzip2");
        compression = compression.toLowerCase();

        try {
            String command = "diff";

            if (command.equals("diff")) {
                FileUI.diff(oldFile, newFile, patchFile, compression);
            } else if (command.equals("patch")) {
                FileUI.patch(oldFile, newFile, patchFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
*/
    /**
     * Main command line entry point.
     *
     * @param args the command line arguments
     * @throws java.lang.Exception If there is an error
     */
    public static void main(final String args[]) throws Exception {
        LinuxImagesManager demo = new LinuxImagesManager();
    }

    public class MCAdapter extends MonitorAndControlNMFAdapter {

        @Override
        public void initialRegistrations(MCRegistration registrationObject) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Attribute onGetValue(Identifier identifier, Byte rawType) {
            return null;
        }

        @Override
        public Boolean onSetValue(Identifier identifier, Attribute value) {
            return false;  // to confirm that the variable was set
        }

        @Override
        public UInteger actionArrived(Identifier name, AttributeValueList attributeValues, Long actionInstanceObjId, boolean reportProgress, MALInteraction interaction) {

            String mount_point_dir = "/home/root/Software_Management/Mount_Points/";
                String old_dir = "/home/root/Software_Management/Mount_Points/Slot_A/";
                String new_dir = "/home/root/Software_Management/Mount_Points/Slot_B/";
                String patch_temp_folder = "/home/root/Software_Management/Mount_Points/Repository/Patches/temp/";
                String DirectCopyFolderName = "DirectCopyFolder_dfhsifuh34t8hg9pw4ghp49ghp49";


            if ("clonePartition".equals(name.getValue())) {
                String partition = attributeValues.get(0).getValue().toString();  // mmcblk0p5
                String filename = (String) attributeValues.get(1).getValue().toString();   // mmcblk05_clone.img

                String cmd = "dd if=/dev/" + partition + " of=/home/root/Software_Management/Mount_Points/Repository/Images/" + filename + " bs=4096 conv=noerror";
                boolean valid = runCommand(cmd);
                try {
                    nanoSatMOFramework.reportActionExecutionProgress(valid, 0, 1, 1, actionInstanceObjId);
                } catch (NMFException ex) {
                    Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if ("restorePartitonWithImage".equals(name.getValue())) {
                String partition = attributeValues.get(0).getValue().toString();  // mmcblk0p5
                String filename = attributeValues.get(1).getValue().toString();   // mmcblk05_clone.img

                String cmd = "dd if=/home/root/Software_Management/Mount_Points/Repository/Images/" + filename + " of=/dev/" + partition + " bs=4096 conv=noerror";
                boolean valid = runCommand(cmd);
                try {
                    nanoSatMOFramework.reportActionExecutionProgress(valid, 0, 1, 1, actionInstanceObjId);
                } catch (NMFException ex) {
                    Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if ("generateDiff".equals(name.getValue())) {
                
                // List all folders in the partition
                // get all the files from a directory
                File oldDirectory = new File(old_dir);  // Location of the folder
                File[] fList = oldDirectory.listFiles();

                if (fList == null) {
                    Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, "The directory could not be found: {0}", oldDirectory.toString());
                    return null;
                }

                String directCopyDirString = patch_temp_folder + DirectCopyFolderName;
                File directCopyDir = new File(directCopyDirString);
                directCopyDir.mkdir();
                // Cycle all the folders and content and check if there are new folders
                for (File new_folder : (new File(new_dir)).listFiles()) {
                    File old_folder = new File(new_dir + new_folder.getName());
                 
                    if (!old_folder.exists()){
                        try {
                            // If it does not exist, then copy the files/folders directly
                            FileUtils.copyDirectory(new_folder, new File(directCopyDirString + File.pathSeparator + new_folder.getName()) );
                        } catch (IOException ex) {
                            Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                
                // Cycle one by one and exclude some...
                for (File old_folder : fList) {
                    if (old_folder.isDirectory()) {
                        File new_folder = new File(new_dir + old_folder.getName());
                        // Store the diffs in a temporary folder
                        File patchFile = new File(patch_temp_folder + old_folder.getName());

                        if (    "var".equals(old_folder.getName()) || 
                                "lib".equals(old_folder.getName()) || 
                                "dev".equals(old_folder.getName())
                                ) {
                            Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.INFO, "(3) Skipping directory: " + old_folder.getName());
                            continue;
                        }

                        try {
                            Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.INFO, "(1) Diffing directory: " + old_folder.getName());
                            new DirDeltaCreator().create(old_folder, new_folder, patchFile);
                        } catch (IOException ex) {
                            Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.INFO, "(2) Couldn't diff directory: " + old_folder.getName());
                            continue;
                        }

                    }
                }
                
                Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.INFO, "(4) Diff completed!");
                try {
                    nanoSatMOFramework.reportActionExecutionProgress(true, 0, 1, 3, actionInstanceObjId);
                } catch (NMFException ex) {
                    Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
                }

                // Compress the temporary folder in a new file and delete the folder
                boolean valid = runCommand("tar -zcvf " + patch_temp_folder + "Slot_A-Slot_B.tar.gz " + patch_temp_folder.substring(0, patch_temp_folder.length()-1));
                try {
                    nanoSatMOFramework.reportActionExecutionProgress(valid, 0, 2, 3, actionInstanceObjId);
                } catch (NMFException ex) {
                    Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.INFO, "(5) Compression completed!");

                boolean valid2 = runCommand("rm -r " + patch_temp_folder);
                try {
                    nanoSatMOFramework.reportActionExecutionProgress(valid2, 0, 3, 3, actionInstanceObjId);
                } catch (NMFException ex) {
                    Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.INFO, "(6) Folder Deleted!");
                
            }

            if ("patchPartition".equals(name.getValue())) {

                // Extract the files into a temporary folder
                boolean valid = runCommand("tar -xzvf " + patch_temp_folder + "Slot_A-Slot_B.tar.gz" + " " + patch_temp_folder + DirectCopyFolderName + "a");
                try {
                    nanoSatMOFramework.reportActionExecutionProgress(valid, 0, 1, 3, actionInstanceObjId);
                } catch (NMFException ex) {
                    Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                Logger.getLogger(LinuxImagesManager.class.getName()).log(Level.INFO, "(5) Compression completed!");
                
                
                // Cycle them all and patch the folders
                // Delete the temporary folder
                boolean valid2 = runCommand("aaa");

            }

            if ("imageChecksum".equals(name.getValue())) {
                String cmd = "sha1sum mmcblk05_clone2.img";
//                boolean valid = runCommand(cmd);

            }

            return null;
        }

    }

    private String getPartitionName() {
        return "mmcblk0p5";
    }

    private boolean runCommand(String cmd) {

        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});
//            Process proc = Runtime.getRuntime().exec(new String[]{"cmd", "/c", cmd});
            StreamWrapper error = new StreamWrapper(proc.getErrorStream(), "ERROR");
            StreamWrapper output = new StreamWrapper(proc.getInputStream(), "OUTPUT");
            int exitVal = 0;

            error.start();
            output.start();
            error.join(3000);
            output.join(3000);
            exitVal = proc.waitFor();
            System.out.println("Output:\n" + output.getMessage() + "\nError:\n" + error.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private class StreamWrapper extends Thread {

        private InputStream is = null;
        private String type = null;
        private String message = null;

        public String getMessage() {
            return message;
        }

        StreamWrapper(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        private String getOutput() {
            return message;
        }

        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuffer buffer = new StringBuffer();
                String line = null;
                while ((line = br.readLine()) != null) {
                    buffer.append(line);
                    buffer.append("\n");
                }
                message = buffer.toString();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
/*
    private void doTest(File test1File, File test2File, int chunkSize) throws IOException {
        File patchedFile = new File("/home/root/repository/Full_Images/patchedFile.img");
        File delta = new File("/home/root/repository/Full_Images/delta");
        DiffWriter output = new GDiffWriter(new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(delta))));
        Delta d = new Delta();
        d.setChunkSize(chunkSize);
        d.compute(test1File, test2File, output);
        output.close();

//        assertTrue(delta.exists());
        System.out.println("delta length " + delta.length() + " for " + test1File + " " + test2File);
        System.out.println(toString(read(delta).toByteArray()));
        System.out.println("end patch");

        GDiffPatcher diffPatcher = new GDiffPatcher();
        diffPatcher.patch(test1File, delta, patchedFile);
//        assertTrue(patchedFile.exists());

//        assertEquals("file length", test2File.length(), patchedFile.length());
        byte[] buf = new byte[(int) test2File.length()];
        FileInputStream is = new FileInputStream(patchedFile);
        is.read(buf);
        is.close();
        patchedFile.delete();

//        assertEquals(new String(buf), read(test2File).toString());
    }
*/
    static ByteArrayOutputStream read(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            while (true) {
                int r = fis.read();
                if (r == -1) {
                    break;
                }
                os.write(r);
            }
            return os;
        } finally {
            fis.close();
        }
    }

    private static void append(StringBuffer sb, int value) {
        char b1 = (char) ((value >> 4) & 0x0F);
        char b2 = (char) ((value) & 0x0F);
        sb.append(Character.forDigit(b1, 16));
        sb.append(Character.forDigit(b2, 16));
    }

    /**
     * Return the data as a series of hex values.
     */
    public String toString(byte buffer[]) {
        int length = buffer.length;
        StringBuffer sb = new StringBuffer(length * 2);
        for (int i = 0; i < length; i++) {
            append(sb, buffer[i]);
        }
        return sb.toString();
    }

}
