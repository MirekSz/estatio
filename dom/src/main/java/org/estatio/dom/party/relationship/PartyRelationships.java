package org.estatio.dom.party.relationship;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.ActionSemantics;
import org.apache.isis.applib.annotation.ActionSemantics.Of;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.NotInServiceMenu;
import org.apache.isis.applib.annotation.Optional;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Prototype;
import org.apache.isis.applib.annotation.RegEx;
import org.apache.isis.core.runtime.authentication.standard.RandomCodeGenerator10Chars;

import org.estatio.dom.EstatioDomainService;
import org.estatio.dom.RegexValidation;
import org.estatio.dom.communicationchannel.CommunicationChannelType;
import org.estatio.dom.communicationchannel.CommunicationChannels;
import org.estatio.dom.party.Party;
import org.estatio.dom.party.Person;
import org.estatio.dom.party.Persons;

@DomainService
public class PartyRelationships extends EstatioDomainService<PartyRelationship> {

    public PartyRelationships() {
        super(PartyRelationships.class, PartyRelationship.class);
    }

    @Prototype
    @ActionSemantics(Of.SAFE)
    public List<PartyRelationship> allRelationships() {
        return allInstances();
    }

    @Programmatic
    public List<PartyRelationship> findByParty(Party party) {
        return allMatches("findByParty", "party", party);
    }

    @ActionSemantics(Of.NON_IDEMPOTENT)
    @NotInServiceMenu
    public PartyRelationship newRelationship(
            final @Named("From party") Party from,
            final @Named("To party") Party to,
            final @Named("Relationship type") String relationshipType) {
        PartyRelationship relationship = getContainer().injectServicesInto(PartyRelationshipType.createWithToTitle(from, to, relationshipType));
        relationship.setFrom(from);
        relationship.setTo(to);
        persistIfNotAlready(relationship);
        return relationship;
    }

    public Set<String> choices2NewRelationship(
            final Party from,
            final Party to,
            final String type) {
        return PartyRelationshipType.toTitlesFor(
                from == null ? null : from.getClass(),
                to == null ? null : to.getClass());
    }

    @ActionSemantics(Of.NON_IDEMPOTENT)
    @NotInServiceMenu
    public PartyRelationship newRelatedPerson(
            final Party party,
            final @Named("reference") @Optional @RegEx(validation = RegexValidation.Person.REFERENCE) String reference,
            final @Named("initials") @Optional @RegEx(validation = RegexValidation.Person.INITIALS) String initials,
            final @Named("First name") @Optional String firstName,
            final @Named("Last name") String lastName,
            final @Named("Relationship type") String relationshipType,
            final @Named("Phone number") @Optional @RegEx(validation = RegexValidation.CommunicationChannel.PHONENUMBER) String phoneNumber,
            final @Named("Email address") @Optional @RegEx(validation = RegexValidation.CommunicationChannel.EMAIL) String emailAddress
            ) {

        RandomCodeGenerator10Chars generator = new RandomCodeGenerator10Chars();
        String newReference = reference == null ? generator.generateRandomCode().toUpperCase() : reference;
        Person person = persons.newPerson(newReference, initials, firstName, lastName);
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            communicationChannels.newPhoneOrFax(person, CommunicationChannelType.PHONE_NUMBER, phoneNumber);
        }
        if (emailAddress != null && !emailAddress.isEmpty()) {
            communicationChannels.newEmail(person, CommunicationChannelType.EMAIL_ADDRESS, emailAddress);
        }
        return newRelationship(party, person, relationshipType);
    }

    public Set<String> choices5NewRelatedPerson(
            final Party from,
            final String reference,
            final String initials,
            final String firstName,
            final String lastName,
            final String type,
            final String phoneNumber,
            final String emailAddress) {
        return PartyRelationshipType.toTitlesFor(from.getClass(), Person.class);
    }

    @Inject
    private Persons persons;

    @Inject
    private CommunicationChannels communicationChannels;
}