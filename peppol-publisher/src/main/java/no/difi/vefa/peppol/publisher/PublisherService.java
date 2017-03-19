package no.difi.vefa.peppol.publisher;

import no.difi.vefa.peppol.common.model.DocumentTypeIdentifier;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.publisher.api.PublisherSyntax;
import no.difi.vefa.peppol.publisher.api.ServiceGroupProvider;
import no.difi.vefa.peppol.publisher.api.ServiceMetadataProvider;
import no.difi.vefa.peppol.publisher.lang.PublisherException;
import no.difi.vefa.peppol.publisher.model.PublisherServiceMetadata;
import no.difi.vefa.peppol.publisher.model.ServiceGroup;
import no.difi.vefa.peppol.security.xmldsig.DomUtils;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServlet;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * @author erlend
 */
public class PublisherService extends HttpServlet {

    private ServiceGroupProvider serviceGroupProvider;

    private ServiceMetadataProvider serviceMetadataProvider;

    private PublisherSyntaxProvider publisherSyntaxProvider;

    private Signer signer;

    public PublisherService(ServiceGroupProvider serviceGroupProvider,
                            ServiceMetadataProvider serviceMetadataProvider,
                            PublisherSyntaxProvider publisherSyntaxProvider,
                            Signer signer) {
        this.serviceGroupProvider = serviceGroupProvider;
        this.serviceMetadataProvider = serviceMetadataProvider;
        this.publisherSyntaxProvider = publisherSyntaxProvider;
        this.signer = signer;
    }

    public void serviceGroup(OutputStream outputStream, String syntax, URI rootUri,
                             ParticipantIdentifier participantIdentifier)
            throws IOException, JAXBException, PublisherException {
        ServiceGroup serviceGroup = serviceGroupProvider.get(participantIdentifier);

        PublisherSyntax publisherSyntax = publisherSyntaxProvider.getSyntax(syntax);
        Marshaller marshaller = publisherSyntax.getMarshaller();
        marshaller.marshal(publisherSyntax.of(serviceGroup, rootUri), outputStream);
    }

    public void metadataProvider(OutputStream outputStream, String syntax, ParticipantIdentifier participantIdentifier,
                                 DocumentTypeIdentifier documentTypeIdentifier)
            throws IOException, JAXBException, PublisherException {
        PublisherServiceMetadata serviceMetadata =
                serviceMetadataProvider.get(participantIdentifier, documentTypeIdentifier);

        PublisherSyntax publisherSyntax = publisherSyntaxProvider.getSyntax(syntax);
        Marshaller marshaller = publisherSyntax.getMarshaller();

        if (signer == null) {
            marshaller.marshal(publisherSyntax.of(serviceMetadata, false), outputStream);
        } else {
            Document document = DomUtils.newDocumentBuilder().newDocument();
            marshaller.marshal(publisherSyntax.of(serviceMetadata, true), document);
            signer.sign(document, outputStream);
        }
    }
}
