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
import org.apache.axiom.om.impl.OMNodeEx;
import org.apache.axiom.om.impl.exception.OMBuilderException;
import org.apache.axiom.soap.*;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

// [IntelliJ] copy of class org.apache.axiom.soap.impl.builder.SOAP11BuilderHelper 

public class CustomSOAP11BuilderHelper extends CustomSOAPBuilderHelper implements SOAP11Constants {
    private SOAPFactory factory;
    private boolean faultcodePresent = false;
    private boolean faultstringPresent = false;

    public CustomSOAP11BuilderHelper(CustomStAXSOAPModelBuilder builder) {
        super(builder);
        factory = builder.getSoapFactory();
    }

    public OMElement handleEvent(XMLStreamReader parser,
                                 OMElement parent,
                                 int elementLevel) throws SOAPProcessingException {
        this.parser = parser;

        OMElement element = null;
        String localName = parser.getLocalName();

        if (elementLevel == 4) {

            if (SOAP_FAULT_CODE_LOCAL_NAME.equals(localName)) {

                SOAPFaultCode code = factory.createSOAPFaultCode(
                        (SOAPFault) parent, builder);
                processNamespaceData(code, false);
                processAttributes(code);

                processText(parser, code);
                ((OMNodeEx) code).setComplete(true);
                element = code;

                builder.adjustElementLevel(-1);

                faultcodePresent = true;
            } else if (SOAP_FAULT_STRING_LOCAL_NAME.equals(localName)) {

                SOAPFaultReason reason = factory.createSOAPFaultReason(
                        (SOAPFault) parent, builder);
                processNamespaceData(reason, false);
                processAttributes(reason);

                processText(parser, reason);
                ((OMNodeEx) reason).setComplete(true);
                element = reason;
                builder.adjustElementLevel(-1);


                faultstringPresent = true;
            } else if (SOAP_FAULT_ACTOR_LOCAL_NAME.equals(localName)) {
                element =
                        factory.createSOAPFaultRole((SOAPFault) parent,
                                                    builder);
                processNamespaceData(element, false);
                processAttributes(element);
            } else if (SOAP_FAULT_DETAIL_LOCAL_NAME.equals(localName)) {
                element =
                        factory.createSOAPFaultDetail((SOAPFault) parent,
                                                      builder);
                processNamespaceData(element, false);
                processAttributes(element);
            } else {
                element =
                        factory.createOMElement(
                                localName, null, parent, builder);
                processNamespaceData(element, false);
                processAttributes(element);
            }

        } else if (elementLevel == 5) {

            String parentTagName = "";
            if (parent instanceof Element) {
                parentTagName = ((Element) parent).getTagName();
            } else {
                parentTagName = parent.getLocalName();
            }

            if (parentTagName.equals(SOAP_FAULT_CODE_LOCAL_NAME)) {
                throw new OMBuilderException(
                        "faultcode element should not have children");
            } else if (parentTagName.equals(
                    SOAP_FAULT_STRING_LOCAL_NAME)) {
                throw new OMBuilderException(
                        "faultstring element should not have children");
            } else if (parentTagName.equals(
                    SOAP_FAULT_ACTOR_LOCAL_NAME)) {
                throw new OMBuilderException(
                        "faultactor element should not have children");
            } else {
                element =
                        this.factory.createOMElement(
                                localName, null, parent, builder);
                processNamespaceData(element, false);
                processAttributes(element);
            }

        } else if (elementLevel > 5) {
            element =
                    this.factory.createOMElement(localName,
                                                 null,
                                                 parent,
                                                 builder);
            processNamespaceData(element, false);
            processAttributes(element);
        }

        return element;
    }

    private void processText(XMLStreamReader parser, OMElement value) {
        try {
            int token = parser.next();
            while (token != XMLStreamReader.END_ELEMENT) {
                if (token == XMLStreamReader.CHARACTERS) {
                    factory.createOMText(value, parser.getText());
                } else if (token == XMLStreamReader.CDATA) {
                    factory.createOMText(value, parser.getText());
                } else {
                    throw new SOAPProcessingException(
                    "Only Characters are allowed here");
                }
                token = parser.next();
            }


        } catch (XMLStreamException e) {
            throw new SOAPProcessingException(e);
        }
    }

}
