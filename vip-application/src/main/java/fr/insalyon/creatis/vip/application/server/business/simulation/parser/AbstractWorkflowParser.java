package fr.insalyon.creatis.vip.application.server.business.simulation.parser;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.insalyon.creatis.vip.application.models.Descriptor;
import fr.insalyon.creatis.vip.application.models.Source;

public abstract class AbstractWorkflowParser extends DefaultHandler {


    protected XMLReader reader;
    protected List<Source> sources;
    protected String description = "No description found for this application. Please contact the developer to know what it is about.";
    private static final Logger logger = LoggerFactory.getLogger(AbstractWorkflowParser.class);

    protected AbstractWorkflowParser() {
        sources = new ArrayList<Source>();
    }

    public Descriptor parse(String fileName) throws IOException, SAXException, ParserConfigurationException {
        return parse(new FileReader(fileName));
    }

    public Descriptor parseString(String workflowString) throws IOException, SAXException, ParserConfigurationException {
        return parse(new StringReader(workflowString));
    }

   private Descriptor parse(Reader workflowReader) throws IOException, SAXException, ParserConfigurationException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);

        try {
            // Disable external general entities to prevent local file disclosure 
            parserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            // Disable external parameter entities to prevent attacks via external DTD files
            parserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            // Prevent the parser from loading external DTDs (Document Type Definitions)
            parserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            logger.warn("The SAX parser does not support some XXE security features", e);
        }

        reader = parserFactory.newSAXParser().getXMLReader();
        reader.setContentHandler(this);
        reader.parse(new InputSource(workflowReader));

        return new Descriptor(sources, description);
    }
}
