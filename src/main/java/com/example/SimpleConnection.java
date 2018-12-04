/*
 * To the extent possible under law, the OME developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */
package com.example;

import static omero.rtypes.rlong;
import static omero.rtypes.rstring;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.apache.commons.lang.ArrayUtils;

import ome.util.Utils;
import ome.util.checksum.ChecksumProviderFactory;
import ome.util.checksum.ChecksumProviderFactoryImpl;
import ome.util.checksum.ChecksumType;
import omero.ClientError;
import omero.RLong;
import omero.RType;
import omero.ServerError;
import omero.api.IUpdatePrx;
import omero.api.RawFileStorePrx;
import omero.api.ServiceFactoryPrx;
import omero.api.ThumbnailStorePrx;
import omero.cmd.Delete2;
import omero.cmd.Delete2Response;
import omero.sys.Parameters;
import omero.sys.ParametersI;

import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ProjectData;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.log.SimpleLogger;
import omero.model.ChecksumAlgorithm;
import omero.model.ChecksumAlgorithmI;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;
import omero.model.enums.ChecksumAlgorithmSHA1160;


/**
 * A simple connection to an OMERO server using the Java gateway
 *
 * @author The OME Team
 */
public class SimpleConnection extends JFrame  implements ActionListener{
	
	//GUI
	private JTextPane output;
	private JTextField userName;
	private JTextField serverName;
	private JPasswordField passw;
	private JTextField filePath;
	private JButton browseBtn;
	private JButton chancelBtn;
	private JButton runBtn;
	private JButton closeBtn;
	
	

    /** Reference to the gateway.*/
    private Gateway gateway;
    
    private SimpleConnection client ;

    /** The security context.*/
    private SecurityContext ctx;
    
   
    
    /** 
     * Creates a connection, the gateway will take care of the services
     * life-cycle.
     *
     * @param hostname The name of the server.
     * @param port The port to use.
     * @param userName The name of the user.
     * @param password The user's password.
     */
    protected void connect(String hostname, int port, String userName, String password)
        throws Exception
    {
        LoginCredentials cred = new LoginCredentials();
        cred.getServer().setHostname(hostname);
        if (port > 0) {
            cred.getServer().setPort(port);
        }
        cred.getUser().setUsername(userName);
        cred.getUser().setPassword(password);
        ExperimenterData user = gateway.connect(cred);
        appendOutput("Connected as " + user.getUserName(),null);
        ctx = new SecurityContext(user.getGroupId());
    }

    /** 
     * Creates a connection, the gateway will take care of the services
     * life-cycle.
     *
     * @param args The arguments used to connect.
     */
    private void connect(String[] args)
        throws Exception
    {
        LoginCredentials cred = new LoginCredentials(args);
        ExperimenterData user = gateway.connect(cred);
        appendOutput("Connected as " + user.getUserName(),null);
        ctx = new SecurityContext(user.getGroupId());
    }
    
    /** Makes sure to disconnect to destroy sessions.*/
    private void disconnect()
    {
        gateway.disconnect();
    }

    /** Loads the projects owned by the user currently logged in.*/
    private void loadProjects()
        throws Exception
    {
        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        Collection<ProjectData> projects = browse.getProjects(ctx);
    }

    // transfer image as OriginalFile upload, measure transfer time
    private void transferImage() throws Exception
    {
    	String fName=getTestFilePath();
    	File file = new File(fName);
//    	String path = System.getProperty("user.dir");
//    	path =getProgrammDir(path);
//    	File file=new File(path+fName);
//    	File file=new File("/cellnanos/copytest/CK_134_20171214_Caco-LA-eGFP_NCTC12023_90min_15µL.czi");
    	
    	if(file !=null && file.exists())
    	{
    		appendOutput("ImportFile: "+file.getAbsolutePath(),null);
    		ServiceFactoryPrx sf =  gateway.getImportStore(ctx).getServiceFactory();
    		if(sf!=null) {

    			upload(file, null, 5000000,sf);
    		}
    	}else {
    		appendOutput("File not exists: "+file.getAbsolutePath(),null);
    	}
    }
    
    private String getProgrammDir(String path) {
		String parent = path;
		for(int i=0; i<5;i++)
			parent = new File(parent).getParent();
		return parent;
	}
    
    private void deleteOriginalFile(String id) {
    	// cli:  >>bin/omero delete OriginalFile:<id>
    }
    

	/**
     * Utility method to upload a file to the server
     *
     * @param file
     *            Cannot be null.
     * @param fileObject
     *            Can be null.
     * @param blockSize
     *            Can be null.
     */
    public OriginalFile upload(File file, OriginalFile fileObject, Integer blockSize,ServiceFactoryPrx sf) throws ServerError, IOException  {
    	
        if (file == null) {
        	appendOutput("Non-null file must be provided",null);
        }

        if (!file.exists() || ! file.canRead()) {
        	appendOutput("File does not exist or is not readable: " + file.getAbsolutePath(),null);
        }
    	
        if (blockSize == null) {
            blockSize = 262144;
        }

        long size = file.length();
        if (blockSize > size) {
            blockSize = (int) size;
        }

        if (fileObject == null) {
            fileObject = new OriginalFileI();
        }

        fileObject.setSize(rlong(size));

//        final ChecksumAlgorithm hasher = new ChecksumAlgorithmI();
//        hasher.setValue(rstring(ChecksumAlgorithmSHA1160.value));
//        fileObject.setHasher(hasher);
//        fileObject.setHash(rstring(sha1(file)));

        if (fileObject.getName() == null) {
            fileObject.setName(rstring(file.getName()));
        }

        if (fileObject.getPath() == null) {
            String path = file.getParent() == null ?
                    File.separator : (file.getParent() + File.separator);
            fileObject.setPath(rstring(path));
        }

        if (fileObject.getMimetype() == null) {
            String mimeType = null;
            try {
                mimeType = Files.probeContentType(file.toPath());
                appendOutput("Detected FileType: "+mimeType,null);
            } catch (IOException | SecurityException e) {
                /* can't guess */
            }
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            fileObject.setMimetype(rstring(mimeType));
        }

        IUpdatePrx up = sf.getUpdateService();
        fileObject = (OriginalFile) up.saveAndReturnObject(fileObject);

        byte[] buf = new byte[blockSize];
        RawFileStorePrx rfs = sf.createRawFileStore();
        
        FileInputStream stream = null;
        try {
        	appendOutput("Upload file...",null);
            rfs.setFileId(fileObject.getId().getValue());
            appendOutput("\t File id: "+fileObject.getId().getValue(),null);
            appendOutput("\t File size: "+size,null);
            stream = new FileInputStream(file);
            long offset = 0;
            int rlen;
            long startTime = System.currentTimeMillis();

            while(true) {
            	rlen=stream.read(buf);
            	if (rlen == -1) {
            		break;
            	}
            	final byte[] bufferToWrite;
            	if (rlen < buf.length) {
            		bufferToWrite = new byte[rlen];
            		System.arraycopy(buf, 0, bufferToWrite, 0, rlen);
            	} else {
            		bufferToWrite = buf;
            	}
            	rfs.write(bufferToWrite, offset, rlen);
            	offset += rlen;
            }
            long endTime = System.currentTimeMillis();
            long duration=endTime-startTime;
            appendOutput("Duration: "+duration,null);
            double traffic=(1000.0*size)/(duration*124955978.6);
            appendOutput("Traffic: " +traffic+" GBit/sec",null);
            appendOutput("... finished upload file",null);
            return rfs.save();
        } finally {
            Utils.closeQuietly(stream);
            if (rfs != null) {
                rfs.close();
            }
        }
    }
    
    /**
     * Calculates the local sha1 for a file.
     * @param file a local file
     * @return the file's SHA-1 hexadecimal digest
     */
    public String sha1(File file) {
        ChecksumProviderFactory cpf = new ChecksumProviderFactoryImpl();
        return cpf.getProvider(ChecksumType.SHA1).putFile(
                        file.getAbsolutePath()).checksumAsString();
    }

    
//    private void downloadImage()
//    {
//    	int INC = 262144;
//    	RawFileStorePrx store;
//    	File f;// The file on client's machine
//    	OriginalFile of; //object from the list returned in previous call.
//    	store.setFileId(of.getId().getValue());
//    	stream = new FileOutputStream(f);
//    	size = of.getSize().getValue();
//    	try {
//    	   for (offset = 0; (offset+INC) < size;) {
//    	       stream.write(store.read(offset, INC)); //read the data
//    	      offset += INC;
//    	    }               
//    	} finally {
//    	stream.write(store.read(offset, (int) (size-offset)));
//    	stream.close();
//    	}            
//    }
    
    
    
    /** Loads the image with the id 1.*/
    private void loadFirstImage()
        throws Exception
    {
        ParametersI params = new ParametersI();
        params.acquisitionData();
        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        ImageData image = browse.getImage(ctx, 1L, params);
        PixelsData pixels = image.getDefaultPixels();
        ThumbnailStorePrx store = gateway.getThumbnailService(ctx);
        store.setPixelsId(pixels.getId());
        System.out.println("Ready to get thumbnail");
    }

    /** Creates a new instance.*/
    SimpleConnection()
    {
    	gateway = new Gateway(new SimpleLogger());
//    	client = new SimpleConnection();
    	initGUIComponents();
    }
    
    private void initGUIComponents() {
    	JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
 
        cs.fill = GridBagConstraints.HORIZONTAL;
 
        JLabel lbserver = new JLabel("Server: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(lbserver, cs);
 
        serverName = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        panel.add(serverName, cs);
        
        JLabel lbUsername = new JLabel("Username: ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        panel.add(lbUsername, cs);
 
        userName = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        panel.add(userName, cs);
 
        JLabel lbPassword = new JLabel("Password: ");
        cs.gridx = 0;
        cs.gridy = 2;
        cs.gridwidth = 1;
        panel.add(lbPassword, cs);
 
        passw = new JPasswordField(20);
        cs.gridx = 1;
        cs.gridy = 2;
        cs.gridwidth = 2;
        panel.add(passw, cs);
        panel.setBorder(new LineBorder(Color.GRAY));
        
        JLabel lbtestFile = new JLabel("TestFile: ");
        cs.gridx = 0;
        cs.gridy = 3;
        cs.gridwidth = 1;
        panel.add(lbtestFile, cs);
        
        filePath = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 3;
        cs.gridwidth = 2;
        panel.add(filePath, cs);
        
        browseBtn = new JButton("Browse");
        browseBtn.addActionListener(this);
        cs.gridx = 3;
        cs.gridy = 3;
        cs.gridwidth = 1;
        panel.add(browseBtn, cs);
        
        runBtn = new JButton("Run");
        runBtn.addActionListener(this);
        chancelBtn = new JButton("Cancel");
        chancelBtn.addActionListener(this);
        closeBtn = new JButton("Close");
        closeBtn.addActionListener(this);
        
        JPanel bp = new JPanel();
        bp.add(chancelBtn);
        bp.add(runBtn);
        bp.add(closeBtn);
        
        output = new JTextPane();
        output.setEditable(false);
		DefaultCaret caret = (DefaultCaret) output.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		JScrollPane sp = new JScrollPane();
		sp.setViewportView(output);
        
        add(panel, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(bp, BorderLayout.PAGE_END);
 
        pack();
        this.setSize(600,400);
		
	}
	

    /**
     */
    public static void main(String[] args) throws Exception {
    	java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new SimpleConnection().setVisible(true);
			}
		});
    }
    
    
    private void appendOutput(String s,String style) {
		try {
			StyledDocument doc=output.getStyledDocument();
			addStylesToDocument(doc);
			Style myStyle=doc.getStyle("regular");
			if(style!=null)
				myStyle=doc.getStyle(style);
			doc.insertString(doc.getLength(), s+"\n", myStyle);
		} catch(BadLocationException exc) {
			exc.printStackTrace();
		}
	}

	private void addStylesToDocument(StyledDocument doc) {
		//Initialize some styles.
		Style def = StyleContext.getDefaultStyleContext().
				getStyle(StyleContext.DEFAULT_STYLE);

		Style regular = doc.addStyle("regular", def);
		StyleConstants.setFontFamily(def, "SansSerif");

		Style s = doc.addStyle("bold", regular);
		StyleConstants.setBold(s, true);

		s = doc.addStyle("large", regular);
		StyleConstants.setFontSize(s, 16);
	}
	
	public String getUsername() {
        return userName.getText().trim();
    }
 
    public String getPassword() {
        return new String(passw.getPassword());
    }
    
    public String getServer() {
    	return serverName.getText().trim();
    }
    public JTextPane getOutput() {
    	return output;
    }
    public String getTestFilePath()
    {
    	return filePath.getText();
    }

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == browseBtn) {
			JFileChooser fcOpen =new JFileChooser();
			File tempFile = new File(filePath.getText());
			if(tempFile!=null && tempFile.exists()) {
				fcOpen.setCurrentDirectory(new File(tempFile.getParent()));
			}
        	int returnValOpen=fcOpen.showOpenDialog(this);
        	if(returnValOpen == JFileChooser.APPROVE_OPTION) {
        		tempFile = fcOpen.getSelectedFile();
        		filePath.setText(tempFile.getAbsolutePath());
        	}
		}else if(e.getSource()==runBtn) {
			try {
				connect(getServer(), 4064, getUsername(), getPassword());
				transferImage();
				appendOutput("Delete file on server with >>bin/omero delete OriginalFile:<id>",null);
            } catch (Exception e1) {
				// TODO Auto-generated catch block
				appendOutput(e1.getMessage(),null);
			} finally {
                disconnect();
            }
		}else if(e.getSource()==chancelBtn) {
			dispose();
		}else if(e.getSource()==closeBtn) {
			disconnect();
            dispose();
		}
	}

    
    
}
