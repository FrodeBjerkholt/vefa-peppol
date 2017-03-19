package no.difi.vefa.peppol.publisher.builder;

import no.difi.vefa.peppol.common.model.DocumentTypeIdentifier;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;
import no.difi.vefa.peppol.common.model.ProcessMetadata;
import no.difi.vefa.peppol.publisher.model.PublisherEndpoint;
import no.difi.vefa.peppol.publisher.model.PublisherServiceMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * @author erlend
 */
public class ServiceMetadataBuilder {

    private ParticipantIdentifier participantIdentifier;

    private DocumentTypeIdentifier documentTypeIdentifier;

    private List<ProcessMetadata<PublisherEndpoint>> processes = new ArrayList<>();

    public static ServiceMetadataBuilder newInstance() {
        return new ServiceMetadataBuilder();
    }

    private ServiceMetadataBuilder() {

    }

    public ServiceMetadataBuilder participant(ParticipantIdentifier participantIdentifier) {
        this.participantIdentifier = participantIdentifier;
        return this;
    }

    public ServiceMetadataBuilder documentTypeIdentifier(DocumentTypeIdentifier documentTypeIdentifier) {
        this.documentTypeIdentifier = documentTypeIdentifier;
        return this;
    }

    public ServiceMetadataBuilder add(ProcessIdentifier processIdentifier, PublisherEndpoint... endpoints) {
        this.processes.add(ProcessMetadata.of(processIdentifier, endpoints));
        return this;
    }

    public PublisherServiceMetadata build() {
        return new PublisherServiceMetadata(participantIdentifier, documentTypeIdentifier, processes);
    }
}
