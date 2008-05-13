package org.jetbrains.tfsIntegration.core.tfs.version;

import org.apache.axiom.om.OMFactory;
import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Date;
import java.text.SimpleDateFormat;

public class DateVersionSpec extends VersionSpecBase {
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    public static SimpleDateFormat otextFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); // TODO: use SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)?
    private Date myDate;

    public DateVersionSpec(Date date){
        myDate = date;
    }

    public Date getMyDate() {
        return myDate;
    }

    protected void writeAttributes(QName parentQName, OMFactory factory, MTOMAwareXMLStreamWriter xmlWriter) throws XMLStreamException {
        writeVersionAttribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", xmlWriter);
        writeVersionAttribute("", "xsi:type", "DateVersionSpec", xmlWriter);
        writeVersionAttribute("", "date", getDateString(), xmlWriter);
        writeVersionAttribute("", "otext", getOTextString(), xmlWriter);
    }

    public String getOTextString() {
        return otextFormat.format(myDate);
    }

    public String getDateString() {
        String dateString = dateFormat.format(myDate);
        // FIXME: better way to get date in w3c format?
        // Complete date plus hours, minutes and seconds:
        // YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
        dateString = dateString.substring(0, 22) + ":" + dateString.substring(22);
        return dateString;
    }

    public Date getDate() {
        return myDate;
    }
}
