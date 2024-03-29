package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessagePhysicalAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

class AddressMapperTest {

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<AddressMapper> constructor = AddressMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        Assertions.assertEquals(null, exception.getMessage());
    }

    @Test
    void addressMapperFromNationalRegistryTest() {
        Address address=AddressMapper.fromNationalRegistry(getpysicalAddress());
        Assertions.assertNotNull(address);
    }
    @Test
    void addressMapperToEntityTest() {
        String requestId= "12345abcde";
        PnAddress addressValue=AddressMapper.toEntity(getAddress(),requestId, AddressTypeEnum.AR_ADDRESS, new PnPaperChannelConfig());
        Assertions.assertNotNull(addressValue);
    }
    @Test
    void addressMapperFromAnalogToAddressTest() {
        Address address=AddressMapper.fromAnalogToAddress(getAnalogAddress(), "AR", "PREPARE");
        Assertions.assertNotNull(address);
    }
    @Test
    void addressMapperFromAnalogToAddressNULLTest() {
        Assertions.assertNull(AddressMapper.fromAnalogToAddress(null, null,null));
    }
    @Test
    void addressMapperToDTOTest() {
     PnAddress pnAddress = new PnAddress();
        pnAddress.setAddress("via roma");
        pnAddress.setAddressRow2("via lazio");
        pnAddress.setCap("00061");
        pnAddress.setCity("roma");
        pnAddress.setCity2("viterbo");
        pnAddress.setCountry("italia");
        pnAddress.setPr("PR");
        pnAddress.setFullName("Ettore Fieramosca");
        pnAddress.setNameRow2("Ettore");
        Address address=AddressMapper.toDTO(pnAddress);
        Assertions.assertNotNull(address);
    }
    @Test
    void addressMapperToPojoTest() {
        DiscoveredAddressDto discoveredAddressDto = new DiscoveredAddressDto();
        AnalogAddress address=AddressMapper.toPojo(discoveredAddressDto);
        Assertions.assertNotNull(address);
    }
    @Test
    void addressMapperToPojoTest2() {
        Address address = new Address();
        AnalogAddress addresss=AddressMapper.toPojo(address);
        Assertions.assertNotNull(addresss);
    }

    public Address getAddress(){
        Address address= new Address();
        address.setAddress("via roma");
        address.setAddressRow2("via lazio");
        address.setCap("00061");
        address.setCity("roma");
        address.setCity2("viterbo");
        address.setCountry("italia");
        address.setPr("PR");
        address.setFullName("Ettore Fieramosca");
        address.setNameRow2("Ettore");
        return address;
    }
    public AddressSQSMessagePhysicalAddressDto getpysicalAddress(){
        AddressSQSMessagePhysicalAddressDto pysicalAddress = new AddressSQSMessagePhysicalAddressDto();
        pysicalAddress.setAt("at");
        pysicalAddress.setAddress("address");
        pysicalAddress.setAddressDetails("addressDetails");
        pysicalAddress.setZip("zip");
        pysicalAddress.setMunicipality("municipality");
        pysicalAddress.setMunicipalityDetails("municipalityDetails");
        pysicalAddress.setProvince("province");
        pysicalAddress.setForeignState("foreignState");
        return pysicalAddress;
    }
    public AnalogAddress getAnalogAddress(){
        AnalogAddress analogAddress= new AnalogAddress();
        analogAddress.setAddress("via roma");
        analogAddress.setAddressRow2("via lazio");
        analogAddress.setCap("00061");
        analogAddress.setCity("roma");
        analogAddress.setCity2("viterbo");
        analogAddress.setCountry("italia");
        analogAddress.setPr("PR");
        analogAddress.setFullname("Ettore Fieramosca");
        analogAddress.setNameRow2("Ettore");
        return analogAddress;
    }

}
