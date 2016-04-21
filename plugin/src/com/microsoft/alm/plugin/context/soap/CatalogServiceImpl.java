// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.soap;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.TeamServicesException;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.vss.client.core.model.VssServiceException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public class CatalogServiceImpl implements CatalogService {
    private static final Logger logger = LoggerFactory.getLogger(CatalogServiceImpl.class);

    private final ServerContext context;
    private final URI endpointUri;

    private final static String SOAP = "http://www.w3.org/2003/05/soap-envelope"; //$NON-NLS-1$

    private static final String ENDPOINT_PATH = "/TeamFoundation/Administration/v3.0/CatalogService.asmx"; //$NON-NLS-1$

    private static final int QUERY_OPTIONS_NONE = 0;
    private static final int QUERY_OPTIONS_EXPAND_DEPENDENCIES = 1;

    private static final String SINGLE_RECURSE_STAR = "*";

    private static final String QUERY_NODE_RESPONSE = "QueryNodesResponse"; //$NON-NLS-1$

    private static final String ORGANIZATIONAL_ROOT = "69A51C5E-C093-447e-A177-A09E47A60974"; //$NON-NLS-1$
    private static final String TEAM_FOUNDATION_SERVER_INSTANCE = "b36f1bda-df2d-482b-993a-f194a31a1fa2"; //$NON-NLS-1$
    private static final String PROJECT_COLLECTION = "26338D9E-D437-44aa-91F2-55880A328B54"; //$NON-NLS-1$

    private static final XMLInputFactory XML_INPUT_FACTORY;

    static {
        XML_INPUT_FACTORY = XMLInputFactory.newInstance();
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    }

    public CatalogServiceImpl(final ServerContext context) {
        assert context != null;
        this.context = context;

        final URI baseURI = context.getServerUri();
        endpointUri = UrlHelper.resolveEndpointUri(baseURI, ENDPOINT_PATH);
    }

    public List<TeamProjectCollectionReference> getProjectCollections() {

        final QueryData queryForOrganizationRoot = new QueryData(SINGLE_RECURSE_STAR, QUERY_OPTIONS_NONE, ORGANIZATIONAL_ROOT);
        final CatalogData catalogDataOrganizationRoot = getCatalogDataFromServer(queryForOrganizationRoot);

        //If auth fails, you can get here and catalogDataOrganizationRoot is null
        if (catalogDataOrganizationRoot == null) {
            logger.warn("getProjectCollections catalogDataOrganizationRoot is null");
            throw new TeamServicesException(TeamServicesException.KEY_TFS_AUTH_FAILED);
        }

        final CatalogResource organizationRoot = catalogDataOrganizationRoot.catalogResources.get(0);

        final QueryData queryForFoundationServer = new QueryData(organizationRoot.nodeReferencePaths[0] + SINGLE_RECURSE_STAR, QUERY_OPTIONS_EXPAND_DEPENDENCIES, TEAM_FOUNDATION_SERVER_INSTANCE);
        final CatalogData catalogDataFoundationServer = getCatalogDataFromServer(queryForFoundationServer);
        final CatalogResource foundationServer = catalogDataFoundationServer.catalogResources.get(0);

        final QueryData queryForProjectCollections = new QueryData(foundationServer.nodeReferencePaths[0] + SINGLE_RECURSE_STAR, QUERY_OPTIONS_EXPAND_DEPENDENCIES, PROJECT_COLLECTION);
        final CatalogData catalogDataProjectCollections = getCatalogDataFromServer(queryForProjectCollections);

        final List<TeamProjectCollectionReference> projectCollections = new ArrayList<TeamProjectCollectionReference>(catalogDataProjectCollections.catalogResources.size());
        for (CatalogResource catalogResource : catalogDataProjectCollections.catalogResources) {
            final TeamProjectCollectionReference collectionReference = new TeamProjectCollectionReference();

            collectionReference.setId(UUID.fromString(catalogResource.instanceId));

            collectionReference.setName(catalogResource.displayName);

            final String collectionPath = "_apis/projectCollections/" + catalogResource.instanceId; //$NON-NLS-1$
            final URI collectionUri = UrlHelper.resolveEndpointUri(context.getUri(), collectionPath);
            collectionReference.setUrl(collectionUri.toString());

            projectCollections.add(collectionReference);
        }
        return projectCollections;
    }

    public TeamProjectCollectionReference getProjectCollection(final String collectionName) {
        final List<TeamProjectCollectionReference> collections = getProjectCollections();
        for(final TeamProjectCollectionReference collection : collections) {
            if(StringUtils.equalsIgnoreCase(collection.getName(), collectionName)) {
                return collection;
            }
        }
        throw new VssServiceException(TeamServicesException.KEY_OPERATION_ERRORS);
    }

    private class QueryData {
        final String pathSpecs;
        final int queryOptions;
        final String filterOnResourceType;

        QueryData(final String pathSpecs, final int queryOptions, final String filterOnResourceType) {
            this.pathSpecs = pathSpecs;
            this.queryOptions = queryOptions;
            this.filterOnResourceType = filterOnResourceType;
        }
    }

    private CatalogData getCatalogDataFromServer(final QueryData queryData) {
        final HttpPost httpPost = new HttpPost(endpointUri.toString());
        httpPost.setEntity(generateSoapQuery(queryData.pathSpecs, queryData.queryOptions));

        httpPost.addHeader(new BasicHeader("Accept-Encoding", "gzip")); //$NON-NLS-1$ //$NON-NLS-2$
        httpPost.addHeader(new BasicHeader("Accept-Language", localeToRFC5646LanguageTag(Locale.getDefault()))); //$NON-NLS-1$
        httpPost.addHeader(new BasicHeader("Content-Type", "application/soap+xml; charset=utf-8")); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            if (context.getHttpClient() == null) {
                logger.warn("getCatalogDataFromServer context.getHttpClient() is null");
                return null;
            }
            final HttpResponse httpResponse = context.getHttpClient().execute(httpPost);
            final int responseStatusCode = httpResponse.getStatusLine().getStatusCode();

            CatalogData catalogData;
            if (responseStatusCode == HttpStatus.SC_OK) {
                catalogData = new CatalogData(queryData.filterOnResourceType);
                readResponse(httpResponse, catalogData);
            } else {
                throw new HttpResponseException(responseStatusCode, httpResponse.getStatusLine().toString());
            }
            return catalogData;
        } catch (ClientProtocolException e) {
            logger.warn("getCatalogDataFromServer", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.warn("getCatalogDataFromServer", e);
            throw new RuntimeException(e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    private static String localeToRFC5646LanguageTag(final Locale locale) throws IllegalArgumentException {

        // language[-variant][-region]

        String result = locale.getLanguage();

        if (locale.getVariant().length() > 0) {
            result = result + "-" + locale.getVariant(); //$NON-NLS-1$
        }

        if (locale.getCountry().length() > 0) {
            result = result + "-" + locale.getCountry(); //$NON-NLS-1$
        }

        return result;
    }

    private void readResponse(final HttpResponse httpResponse, final ElementDeserializable readFromElement) {

        InputStream responseStream = null;
        try {

            final Header encoding = httpResponse.getFirstHeader("Content-Encoding"); //$NON-NLS-1$
            if (encoding != null && encoding.getValue().equalsIgnoreCase("gzip")) //$NON-NLS-1$
            {
                responseStream = new GZIPInputStream(httpResponse.getEntity().getContent());
            } else {
                responseStream = httpResponse.getEntity().getContent();
            }
            XMLStreamReader reader = null;
            try {

                reader = XML_INPUT_FACTORY.createXMLStreamReader(responseStream);

                final QName envelopeQName = new QName(SOAP, "Envelope", "soap"); //$NON-NLS-1$ //$NON-NLS-2$
                final QName headerQName = new QName(SOAP, "Header", "soap"); //$NON-NLS-1$ //$NON-NLS-2$
                final QName bodyQName = new QName(SOAP, "Body", "soap"); //$NON-NLS-1$ //$NON-NLS-2$

                // Read the envelope.
                if (reader.nextTag() == XMLStreamConstants.START_ELEMENT && reader.getName().equals(envelopeQName)) {
                    while (reader.nextTag() == XMLStreamConstants.START_ELEMENT) {
                        if (reader.getName().equals(headerQName)) {
                            // Ignore headers for now.
                            readUntilElementEnd(reader);
                        } else if (reader.getName().equals(bodyQName)) {
                            if (reader.nextTag() == XMLStreamConstants.START_ELEMENT && reader.getName().getLocalPart().equals(QUERY_NODE_RESPONSE)) {
                                readFromElement.readFromElement(reader);
                                return;
                            }
                        }
                    }
                }
            } catch (final XMLStreamException e) {
                logger.warn("readResponse", e);
                throw new RuntimeException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final XMLStreamException e) {
                        // Ignore and continue
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("readResponse", e);
            throw new RuntimeException(e);
        } finally {
            if (responseStream != null) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                    // Ignore and continue
                }
            }
        }
    }

    private interface ElementDeserializable {
        void readFromElement(final XMLStreamReader reader) throws XMLStreamException;
    }

    private class CatalogData implements ElementDeserializable {

        private final String filterOnResourceType;

        private final List<CatalogResource> catalogResources = new ArrayList<CatalogResource>();

        private CatalogData(final String filterOnResourceType) {
            this.filterOnResourceType = filterOnResourceType;
        }

        @Override
        public void readFromElement(final XMLStreamReader reader) throws XMLStreamException {
            String localName;
            int event;

            do {
                event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    localName = reader.getLocalName();

                    if (localName.equalsIgnoreCase("QueryNodesResult")) { //$NON-NLS-1$

                        String localName0;
                        int event0;

                        do {
                            event0 = reader.next();
                            if (event0 == XMLStreamConstants.START_ELEMENT) {
                                localName0 = reader.getLocalName();

                                if (localName0.equalsIgnoreCase("CatalogResources")) { //$NON-NLS-1$
                                    int event1;

                                    do {
                                        event1 = reader.nextTag();

                                        if (event1 == XMLStreamConstants.START_ELEMENT) {
                                            CatalogResource catalogResource = new CatalogResource();
                                            catalogResource.readFromElement(reader);
                                            if (filterOnResourceType.equalsIgnoreCase(catalogResource.resourceTypeIdentifier)) {
                                                catalogResources.add(catalogResource);
                                            }
                                        }
                                    }
                                    while (event1 != XMLStreamConstants.END_ELEMENT);
                                } else {
                                    // Read the unknown child element until its end
                                    readUntilElementEnd(reader);
                                }
                            }
                        }
                        while (event0 != XMLStreamConstants.END_ELEMENT);
                    } else {
                        // Read the unknown child element until its end
                        readUntilElementEnd(reader);
                    }
                }
            }
            while (event != XMLStreamConstants.END_ELEMENT);
        }
    }


    private class CatalogResource implements ElementDeserializable {
        String displayName;
        String resourceTypeIdentifier;
        boolean matchedQuery;

        String instanceId;

        String[] nodeReferencePaths;

        public void readFromElement(final XMLStreamReader reader)
                throws XMLStreamException {
            String localName;

            // Attributes
            final int attributeCount = reader.getAttributeCount();
            String attributeValue;

            for (int i = 0; i < attributeCount; i++) {
                localName = reader.getAttributeLocalName(i);
                attributeValue = reader.getAttributeValue(i);

                if (localName.equalsIgnoreCase("DisplayName")) { //$NON-NLS-1$
                    this.displayName = attributeValue;
                } else if (localName.equalsIgnoreCase("ResourceTypeIdentifier")) { //$NON-NLS-1$
                    this.resourceTypeIdentifier = attributeValue;
                } else if (localName.equalsIgnoreCase("MatchedQuery")) { //$NON-NLS-1$
                    this.matchedQuery = Boolean.valueOf(attributeValue);
                }
                // Ignore unknown attributes.
            }

            // Elements
            int event;

            do {
                event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    localName = reader.getLocalName();

                    if (localName.equalsIgnoreCase("Properties")) { //$NON-NLS-1$
                    /*
                     * The element type is an array.
                     */
                        int event0;

                        do {
                            event0 = reader.nextTag();

                            if (event0 == XMLStreamConstants.START_ELEMENT) {
                                KeyValueOfStringString keyValueOfStringString = new KeyValueOfStringString();
                                keyValueOfStringString.readFromElement(reader);
                                if (keyValueOfStringString.getKey().equalsIgnoreCase("InstanceId")) { //$NON-NLS-1$
                                    instanceId = keyValueOfStringString.getValue();
                                }
                            }
                        }
                        while (event0 != XMLStreamConstants.END_ELEMENT);

                    } else if (localName.equalsIgnoreCase("NodeReferencePaths")) { //$NON-NLS-1$
                    /*
                     * The element type is an array.
                     */
                        int event0;
                        final List<String> list0 = new ArrayList<String>();

                        do {
                            event0 = reader.nextTag();

                            if (event0 == XMLStreamConstants.START_ELEMENT) {
                                list0.add(reader.getElementText());
                            }
                        }
                        while (event0 != XMLStreamConstants.END_ELEMENT);

                        this.nodeReferencePaths = list0.toArray(new String[list0.size()]);
                    } else {
                        // Read the unknown child element until its end
                        readUntilElementEnd(reader);
                    }
                }
            }
            while (event != XMLStreamConstants.END_ELEMENT);
        }
    }

    private class KeyValueOfStringString implements ElementDeserializable {
        String key;
        String value;

        KeyValueOfStringString() {
            super();
        }

        String getKey() {
            return this.key;
        }

        String getValue() {
            return this.value;
        }

        public void readFromElement(final XMLStreamReader reader)
                throws XMLStreamException {
            String localName;

            // This object uses no attributes

            // Elements
            int event;

            do {
                event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    localName = reader.getLocalName();

                    if (localName.equalsIgnoreCase("Key")) { //$NON-NLS-1$
                        this.key = reader.getElementText();
                    } else if (localName.equalsIgnoreCase("Value")) { //$NON-NLS-1$
                        this.value = reader.getElementText();
                    } else {
                        // Read the unknown child element until its end
                        readUntilElementEnd(reader);
                    }
                }
            }
            while (event != XMLStreamConstants.END_ELEMENT);
        }
    }

    /**
     * Advances the {@link XMLStreamReader} until it has read the end of the
     * current element. Useful when an element is encountered while reading a
     * stream, and it should be skipped.
     *
     * @param reader the stream reader to read from (not null).
     */
    private static void readUntilElementEnd(final XMLStreamReader reader)
            throws XMLStreamException {
        int event = reader.getEventType();

        /*
         * Start element depth at 1, increment when an element is started (not
         * including the element that we start with), decrement when an element
         * is ended, and when it goes to 0 we've read the end of the original
         * reader's element.
         */
        int elementDepth = 1;

        boolean firstTime = true;
        while (true) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    /*
                     * Don't increment depth the first time through, because the
                     * caller opened the element.
                     */
                    if (firstTime) {
                        firstTime = false;
                    } else {
                        elementDepth++;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    elementDepth--;

                    if (elementDepth < 1) {
                        /*
                         * We just read the end element for the original
                         * element.
                         */
                        return;
                    }

                    break;
                default:
                    /*
                     * Things like characters, comments, attributes, etc. Ignore
                     * them all.
                     */
            }

            event = reader.next();
        }
    }


    private static StringEntity generateSoapQuery(final String pathSpecs, final int queryOptions) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version='1.0' encoding='UTF-8'?>"); //$NON-NLS-1$
        stringBuilder.append("<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">");//$NON-NLS-1$
        stringBuilder.append("<soap:Body xmlns=\"http://microsoft.com/webservices/\">");//$NON-NLS-1$
        stringBuilder.append("<QueryNodes>");//$NON-NLS-1$
        stringBuilder.append("<pathSpecs>");//$NON-NLS-1$
        stringBuilder.append("<string>" + pathSpecs + "</string>");//$NON-NLS-1$ //$NON-NLS-2$
        stringBuilder.append("</pathSpecs>");//$NON-NLS-1$
        stringBuilder.append("<queryOptions>" + queryOptions + "</queryOptions>");//$NON-NLS-1$
        stringBuilder.append("</QueryNodes>");//$NON-NLS-1$
        stringBuilder.append("</soap:Body>");//$NON-NLS-1$
        stringBuilder.append("</soap:Envelope>");//$NON-NLS-1$

        final StringEntity stringEntity = new StringEntity(stringBuilder.toString(), ContentType.create("application/soap+xml", "utf-8"));//$NON-NLS-1$ //$NON-NLS-2$
        return stringEntity;
    }

}
