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

import javax.xml.stream.XMLStreamReader;
import java.util.Vector;

// [IntelliJ] copy of class org.apache.axiom.soap.impl.builder.SOAP12BuilderHelper

public class CustomSOAP12BuilderHelper extends CustomSOAPBuilderHelper {

    private SOAPFactory factory;
    private boolean codePresent = false;
    private boolean reasonPresent = false;
    private boolean nodePresent = false;
    private boolean rolePresent = false;
    private boolean detailPresent = false;
    private boolean subcodeValuePresent = false;
    private boolean subSubcodePresent = false;
    private boolean valuePresent = false;
    private boolean subcodePresent = false;
    private boolean codeprocessing = false;
    private boolean subCodeProcessing = false;
    private boolean reasonProcessing = false;
    private Vector detailElementNames;

    public CustomSOAP12BuilderHelper(CustomStAXSOAPModelBuilder builder) {
        super(builder);
        factory = builder.getSoapFactory();
    }

    public OMElement handleEvent(XMLStreamReader parser,
                                 OMElement parent,
                                 int elementLevel) throws SOAPProcessingException {

        this.parser = parser;
        OMElement element = null;

        if (elementLevel == 4) {
            if (parser.getLocalName().equals(
                    SOAP12Constants.SOAP_FAULT_CODE_LOCAL_NAME)) {
                if (codePresent) {
                    throw new OMBuilderException(
                            "Multiple Code element encountered");
                } else {
                    element =
                            factory.createSOAPFaultCode((SOAPFault) parent,
                                                        builder);
                    codePresent = true;
                    codeprocessing = true;
                }
            } else if (parser.getLocalName().equals(
                    SOAP12Constants.SOAP_FAULT_REASON_LOCAL_NAME)) {
                if (!codeprocessing && !subCodeProcessing) {
                    if (codePresent) {
                        if (reasonPresent) {
                            throw new OMBuilderException(
                                    "Multiple Reason Element encountered");
                        } else {
                            element =
                                    factory.createSOAPFaultReason(
                                            (SOAPFault) parent, builder);
                            reasonPresent = true;
                            reasonProcessing = true;
                        }
                    } else {
                        throw new OMBuilderException(
                                "Wrong element order encountred at " +
                                        parser.getLocalName());
                    }
                } else {
                    if (codeprocessing) {
                        throw new OMBuilderException(
                                "Code doesn't have a value");
                    } else {
                        throw new OMBuilderException(
                                "A subcode doesn't have a Value");
                    }
                }

            } else if (parser.getLocalName().equals(
                    SOAP12Constants.SOAP_FAULT_NODE_LOCAL_NAME)) {
                if (!reasonProcessing) {
                    if (reasonPresent && !rolePresent && !detailPresent) {
                        if (nodePresent) {
                            throw new OMBuilderException(
                                    "Multiple Node element encountered");
                        } else {
                            element =
                                    factory.createSOAPFaultNode(
                                            (SOAPFault) parent, builder);
                            nodePresent = true;
                        }
                    } else {
                        throw new OMBuilderException(
                                "wrong element order encountered at " +
                                        parser.getLocalName());
                    }
                } else {
                    throw new OMBuilderException(
                            "Reason element Should have a text");
                }
            } else if (parser.getLocalName().equals(
                    SOAP12Constants.SOAP_FAULT_ROLE_LOCAL_NAME)) {
                if (!reasonProcessing) {
                    if (reasonPresent && !detailPresent) {
                        if (rolePresent) {
                            throw new OMBuilderException(
                                    "Multiple Role element encountered");
                        } else {
                            element =
                                    factory.createSOAPFaultRole(
                                            (SOAPFault) parent, builder);
                            rolePresent = true;
                        }
                    } else {
                        throw new OMBuilderException(
                                "Wrong element order encountered at " +
                                        parser.getLocalName());
                    }
                } else {
                    throw new OMBuilderException(
                            "Reason element should have a text");
                }
              // [IntelliJ] ++++++++++++++
            } else if (parser.getLocalName().equalsIgnoreCase(
              // [IntelliJ] -------------
                    SOAP12Constants.SOAP_FAULT_DETAIL_LOCAL_NAME)) {
                if (!reasonProcessing) {
                    if (reasonPresent) {
                        if (detailPresent) {
                            throw new OMBuilderException(
                                    "Multiple detail element encountered");
                        } else {
                            element =
                                    factory.createSOAPFaultDetail(
                                            (SOAPFault) parent, builder);
                            detailPresent = true;
                        }
                    } else {
                        throw new OMBuilderException(
                                "wrong element order encountered at " +
                                        parser.getLocalName());
                    }
                } else {
                    throw new OMBuilderException(
                            "Reason element should have a text");
                }
            } else {
                throw new OMBuilderException(
                        parser.getLocalName() +
                                " unsupported element in SOAPFault element");
            }

        } else if (elementLevel == 5) {
            if (parent.getLocalName().equals(
                    SOAP12Constants.SOAP_FAULT_CODE_LOCAL_NAME)) {
                if (parser.getLocalName().equals(
                        SOAP12Constants.SOAP_FAULT_VALUE_LOCAL_NAME)) {
                    if (!valuePresent) {
                        element =
                                factory.createSOAPFaultValue(
                                        (SOAPFaultCode) parent, builder);
                        valuePresent = true;
                        codeprocessing = false;
                    } else {
                        throw new OMBuilderException(
                                "Multiple value Encountered in code element");
                    }

                } else if (parser.getLocalName().equals(
                        SOAP12Constants.SOAP_FAULT_SUB_CODE_LOCAL_NAME)) {
                    if (!subcodePresent) {
                        if (valuePresent) {
                            element =
                                    factory.createSOAPFaultSubCode(
                                            (SOAPFaultCode) parent, builder);
                            subcodePresent = true;
                            subCodeProcessing = true;
                        } else {
                            throw new OMBuilderException(
                                    "Value should present before the subcode");
                        }

                    } else {
                        throw new OMBuilderException(
                                "multiple subcode Encountered in code element");
                    }
                } else {
                    throw new OMBuilderException(
                            parser.getLocalName() +
                                    " is not supported inside the code element");
                }

            } else if (parent.getLocalName().equals(
                    SOAP12Constants.SOAP_FAULT_REASON_LOCAL_NAME)) {
                if (parser.getLocalName().equals(
                        SOAP12Constants.SOAP_FAULT_TEXT_LOCAL_NAME)) {
                    element =
                            factory.createSOAPFaultText(
                                    (SOAPFaultReason) parent, builder);
                    ((OMNodeEx) element).setComplete(false);
                    reasonProcessing = false;
                } else {
                    throw new OMBuilderException(
                            parser.getLocalName() +
                                    " is not supported inside the reason");
                }
              // [IntelliJ] ++++++++++++++
            } else if (parent.getLocalName().equalsIgnoreCase(
              // [IntelliJ] --------------
                    SOAP12Constants.SOAP_FAULT_DETAIL_LOCAL_NAME)) {
                element =
                        this.factory.createOMElement(
                                parser.getLocalName(), null, parent, builder);
                builder.setProcessingDetailElements(true);
                detailElementNames = new Vector();
                detailElementNames.add(parser.getLocalName());

            } else {
                throw new OMBuilderException(
                        parent.getLocalName() +
                                " should not have child element");
            }


        } else if (elementLevel > 5) {
            if (parent.getLocalName().equals(
                    SOAP12Constants.SOAP_FAULT_SUB_CODE_LOCAL_NAME)) {
                if (parser.getLocalName().equals(
                        SOAP12Constants.SOAP_FAULT_VALUE_LOCAL_NAME)) {
                    if (subcodeValuePresent) {
                        throw new OMBuilderException(
                                "multiple subCode value encountered");
                    } else {
                        element =
                                factory.createSOAPFaultValue(
                                        (SOAPFaultSubCode) parent, builder);
                        subcodeValuePresent = true;
                        subSubcodePresent = false;
                        subCodeProcessing = false;
                    }
                } else if (parser.getLocalName().equals(
                        SOAP12Constants.SOAP_FAULT_SUB_CODE_LOCAL_NAME)) {
                    if (subcodeValuePresent) {
                        if (!subSubcodePresent) {
                            element =
                                    factory.createSOAPFaultSubCode(
                                            (SOAPFaultSubCode) parent,
                                            builder);
                            subcodeValuePresent = false;
                            subSubcodePresent = true;
                            subCodeProcessing = true;
                        } else {
                            throw new OMBuilderException(
                                    "multiple subcode encountered");
                        }
                    } else {
                        throw new OMBuilderException(
                                "Value should present before the subcode");
                    }
                } else {
                    throw new OMBuilderException(
                            parser.getLocalName() +
                                    " is not supported inside the subCode element");
                }
            } else if (builder.isProcessingDetailElements()) {
                int detailElementLevel = 0;
                boolean localNameExist = false;
                for (int i = 0; i < detailElementNames.size(); i++) {
                    if (parent.getLocalName().equals(
                            detailElementNames.get(i))) {
                        localNameExist = true;
                        detailElementLevel = i + 1;
                    }
                }
                if (localNameExist) {
                    detailElementNames.setSize(detailElementLevel);
                    element =
                            this.factory.createOMElement(
                                    parser.getLocalName(),
                                    null,
                                    parent,
                                    builder);
                    detailElementNames.add(parser.getLocalName());
                }

            } else {
                throw new OMBuilderException(
                        parent.getLocalName() +
                                " should not have child at element level " +
                                elementLevel);
            }
        }

        processNamespaceData(element, false);
        processAttributes(element);
        return element;
    }
}
