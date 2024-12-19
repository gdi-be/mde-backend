package de.terrestris.mde.mde_backend.utils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class NamespaceUtils {

  public static final String GMD = "http://www.isotc211.org/2005/gmd";

  public static final String GCO = "http://www.isotc211.org/2005/gco";

  public static final String GMX = "http://www.isotc211.org/2005/gmx";

  public static final String XLINK = "http://www.w3.org/1999/xlink";

  public static final String GML = "http://www.opengis.net/gml/3.2";

  public static void setNamespaceBindings(XMLStreamWriter writer) throws XMLStreamException {
    writer.setPrefix("gmd", GMD);
    writer.setPrefix("gco", GCO);
    writer.setPrefix("gmx", GMX);
    writer.setPrefix("xlink", XLINK);
    writer.setPrefix("gml", GML);
  }

  public static void writeNamespaceBindings(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeNamespace("gmd", GMD);
    writer.writeNamespace("gco", GCO);
    writer.writeNamespace("gmx", GMX);
    writer.writeNamespace("xlink", XLINK);
    writer.writeNamespace("gml", GML);
  }

}
