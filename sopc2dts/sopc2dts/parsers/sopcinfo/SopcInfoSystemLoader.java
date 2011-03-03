package sopc2dts.parsers.sopcinfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import sopc2dts.Logger;
import sopc2dts.lib.Connection;
import sopc2dts.lib.AvalonSystem;
import sopc2dts.lib.SopcComponentLib;
import sopc2dts.lib.components.BasicComponent;
import sopc2dts.lib.components.Interface;

public class SopcInfoSystemLoader implements ContentHandler {
	public static final float MIN_SUPPORTED_VERSION	= 8.1f;
	public static final float MAX_SUPPORTED_VERSION	= 10.0f;
	float version = MIN_SUPPORTED_VERSION;
	AvalonSystem currSystem;
	BasicComponent currComp;
	private File sourceFile;
	private XMLReader xmlReader;
	protected Vector<Connection> vConnections = new Vector<Connection>();
	SopcComponentLib lib = SopcComponentLib.getInstance();
	String uniqueID = "";
	String currTag = "";
	String versionStr = "";
	
	public synchronized AvalonSystem loadSystem(File source)
	{
		try {
			InputSource in = new InputSource(new FileReader(source));
			sourceFile = source;
			xmlReader = XMLReaderFactory.createXMLReader();
			xmlReader.setContentHandler(this);
			xmlReader.parse(in);
			if(currSystem!=null)
			{
				recheckComponents();
				connectComponents();
			}
		} catch (SAXException e) {
			e.printStackTrace();
			currSystem = null;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			currSystem = null;
		} catch (IOException e) {
			e.printStackTrace();
			currSystem = null;
		}
		return currSystem;
	}
	public void connectComponents()
	{
		Connection conn;
		while(vConnections.size()>0)
		{
			boolean foundDuplicate = false;
			conn = vConnections.firstElement();
			Interface intfM = conn.getMasterInterface();
			Interface intfS = conn.getSlaveInterface();
			//Check for duplicate
			if(intfM!=null)
			{
				for(Connection c : intfM.getConnections())
				{
					if((c.getMasterInterface() == intfM)&&(c.getSlaveInterface() == intfS))
					{
						foundDuplicate = true;
					}
				}
				if(!foundDuplicate)
				{
					intfM.getConnections().add(conn);
					if(intfS!=null)
					{
						intfS.getConnections().add(conn);
					}
				}
			}
			vConnections.remove(conn);
		}
		/*
		 * Now remove tristate (and other unneeded) bridges. If any.
		 * Also flatten hierarchical qsys designs.
		 */
		for(BasicComponent c : currSystem.getSystemComponents())
		{
			c.removeFromSystemIfPossible(currSystem);
		}
	}
	public void recheckComponents()
	{
		for(BasicComponent comp : currSystem.getSystemComponents())
		{
			lib.finalCheckOnComponent(comp);
		}
	}

	public void startElement(String uri, String localName, String qName, 
			Attributes atts) throws SAXException {
		if(localName.equalsIgnoreCase("module"))
		{
			currComp = lib.getComponentForClass(atts.getValue("kind"),
					atts.getValue("name"), atts.getValue("version"));
			currSystem.getSystemComponents().add(currComp);
			@SuppressWarnings("unused")
			SopcInfoComponent c = new SopcInfoComponent(this, xmlReader, currComp);
		} else if(localName.equalsIgnoreCase("connection")) {
			@SuppressWarnings("unused")
			SopcInfoConnection conn = new SopcInfoConnection(this, xmlReader, 
					atts.getValue("kind"), this);
		} else if(localName.equalsIgnoreCase("plugin") ||
				localName.equalsIgnoreCase("parameter")) {
			@SuppressWarnings("unused")
			SopcInfoElementIgnoreAll ignore = new SopcInfoElementIgnoreAll(this, 
					xmlReader, localName);
		} else if(localName.equalsIgnoreCase("reportVersion") ||
				localName.equalsIgnoreCase("uniqueIdentifier")) {
			currTag = localName;
		} else if(localName.equalsIgnoreCase("EnsembleReport"))
		{
			currSystem = new AvalonSystem(atts.getValue("name"), 
					atts.getValue("version"), sourceFile);
		} else {
			System.out.println("New element " + localName);
		}
	}
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(localName.equals(currTag)) {
			currTag = null;
		} else if(localName.equalsIgnoreCase("module")) {
			currComp = null;
		} else if(localName.equalsIgnoreCase("EnsembleReport")) {
			Logger.logln("sopcinfo: Loading done");
		} else {
			Logger.logln("sopcinfo: Unexpected endtag: " + localName);
		}
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		if(currTag!=null)
		{
			if(currTag.equalsIgnoreCase("uniqueIdentifier"))
			{
				uniqueID = String.copyValueOf(ch, start, length);
			} else if(currTag.equalsIgnoreCase("reportVersion"))
			{
				versionStr = String.copyValueOf(ch, start, length);
			}
		}
	}
	public void endDocument() throws SAXException {
	}
	public void endPrefixMapping(String arg0) throws SAXException {
	}
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2)
			throws SAXException {
	}
	public void processingInstruction(String arg0, String arg1)
			throws SAXException {
	}
	public void setDocumentLocator(Locator arg0) {
	}
	public void skippedEntity(String arg0) throws SAXException {
	}
	public void startDocument() throws SAXException {
	}
	public void startPrefixMapping(String arg0, String arg1)
			throws SAXException {
	}
}