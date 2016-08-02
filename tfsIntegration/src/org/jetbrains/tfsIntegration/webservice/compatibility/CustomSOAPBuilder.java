/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jetbrains.tfsIntegration.webservice.compatibility;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXBuilder;
import org.apache.axiom.om.util.DetachableInputStream;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.MessageContext;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

// [IntelliJ] a copy of org.apache.axis2.builder.SOAPBuilder class that uses CustomStAXSOAPModelBuilder

public class CustomSOAPBuilder implements Builder {

    public OMElement processDocument(InputStream inputStream, String contentType,
                                     MessageContext messageContext) throws AxisFault {
        XMLStreamReader streamReader;
        try {
            String charSetEncoding = (String) messageContext
                    .getProperty(Constants.Configuration.CHARACTER_SET_ENCODING);

            // Apply a detachable inputstream.  This can be used later
            // to (a) get the length of the incoming message or (b)
            // free transport resources.
            DetachableInputStream is = new DetachableInputStream(inputStream);
            messageContext.setProperty(Constants.DETACHABLE_INPUT_STREAM, is);

            // Get the actual encoding by looking at the BOM of the InputStream
            PushbackInputStream pis = BuilderUtil.getPushbackInputStream(is);
            String actualCharSetEncoding = BuilderUtil.getCharSetEncoding(pis, charSetEncoding);

            // Get the XMLStreamReader for this input stream
            streamReader = StAXUtils.createXMLStreamReader(pis, actualCharSetEncoding);

            // [IntelliJ ++++++++++++]
            StAXBuilder builder = new CustomStAXSOAPModelBuilder(streamReader);
            // [IntelliJ ------------]
            SOAPEnvelope envelope = (SOAPEnvelope) builder.getDocumentElement();
            BuilderUtil
                    .validateSOAPVersion(BuilderUtil.getEnvelopeNamespace(contentType), envelope);
            BuilderUtil.validateCharSetEncoding(charSetEncoding, builder.getDocument()
                    .getCharsetEncoding(), envelope.getNamespace().getNamespaceURI());
            return envelope;
        } catch (IOException e) {
            throw AxisFault.makeFault(e);
        } catch (XMLStreamException e) {
            throw AxisFault.makeFault(e);
        }
    }
}
