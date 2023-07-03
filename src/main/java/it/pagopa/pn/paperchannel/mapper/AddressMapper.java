package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.AnalogAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessagePhysicalAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.analysis.function.Add;

import java.time.LocalDateTime;

public class AddressMapper {
    private static final BaseMapper<Address, AnalogAddress> mapperAnalog = new BaseMapperImpl<>(Address.class, AnalogAddress.class);

    private static final BaseMapper<PnAddress, Address> mapperToAddressEntity = new BaseMapperImpl<>(PnAddress.class, Address.class);

    private static final BaseMapper<AnalogAddress, DiscoveredAddressDto> mapperToAnalog = new BaseMapperImpl<>(AnalogAddress.class, DiscoveredAddressDto.class);

    private AddressMapper(){
        throw new IllegalCallerException("the constructor must not called");
    }

    public static Address fromAnalogToAddress(AnalogAddress analogAddress, String productType, String flow){
        if (analogAddress == null) return null;
        Address address = fromAnalogToAddress(analogAddress);
        address.setFlowType(flow);
        address.setProductType(productType);
        return address;
    }

    public static Address fromAnalogToAddress(AnalogAddress analogAddress){
        if (analogAddress == null) return null;
        Address address = mapperAnalog.toEntity(analogAddress);
        return address;
    }


    public static Address fromNationalRegistry(AddressSQSMessagePhysicalAddressDto pysicalAddress){
        Address address = new Address();
        address.setNameRow2(pysicalAddress.getAt());
        address.setAddress(pysicalAddress.getAddress());
        address.setAddressRow2(pysicalAddress.getAddressDetails());
        address.setCap(pysicalAddress.getZip());
        address.setCity(pysicalAddress.getMunicipality());
        address.setCity2(pysicalAddress.getMunicipalityDetails());
        address.setPr(pysicalAddress.getProvince());
        address.setCountry(pysicalAddress.getForeignState());
        address.setFromNationalRegistry(true);
        return address;
    }

    public static PnAddress toEntity(Address address, String requestId, PnPaperChannelConfig paperChannelConfig){
        return toEntity(address, requestId, AddressTypeEnum.RECEIVER_ADDRESS, paperChannelConfig);
    }

    public static PnAddress toEntity(Address address, String requestId, AddressTypeEnum addressTypeEnum, PnPaperChannelConfig paperChannelConfig){
        PnAddress pnAddress = mapperToAddressEntity.toEntity(address);
        pnAddress.setRequestId(requestId);
        pnAddress.setTypology(addressTypeEnum.toString());

        if (paperChannelConfig != null) {
            if (StringUtils.equals(address.getFlowType(), Const.PREPARE)){
                //caso PREPARE set diretto
                pnAddress.setTtl(DateUtils.getTimeStampOfMills(LocalDateTime.now().plusDays(paperChannelConfig.getTtlPrepare())));
            }  else{
                //caso EXECUTION set con productType
                if (StringUtils.equals(address.getProductType(), ProductTypeEnum._890.getValue())) {
                    pnAddress.setTtl(DateUtils.getTimeStampOfMills(LocalDateTime.now().plusDays(paperChannelConfig.getTtlExecutionN_890())));
                }
                if (StringUtils.equals(address.getProductType(), ProductTypeEnum.RS.getValue())) {
                    pnAddress.setTtl(DateUtils.getTimeStampOfMills(LocalDateTime.now().plusDays(paperChannelConfig.getTtlExecutionN_RS())));
                }
                if (StringUtils.equals(address.getProductType(), ProductTypeEnum.AR.getValue())) {
                    pnAddress.setTtl(DateUtils.getTimeStampOfMills(LocalDateTime.now().plusDays(paperChannelConfig.getTtlExecutionN_AR())));
                }
                if (StringUtils.equals(address.getProductType(), ProductTypeEnum.RIR.getValue())) {
                    pnAddress.setTtl(DateUtils.getTimeStampOfMills(LocalDateTime.now().plusDays(paperChannelConfig.getTtlExecutionI_AR())));
                }
                if (StringUtils.equals(address.getProductType(), ProductTypeEnum.RIS.getValue())) {
                    pnAddress.setTtl(DateUtils.getTimeStampOfMills(LocalDateTime.now().plusDays(paperChannelConfig.getTtlExecutionI_RS())));
                }
            }
        }
        return pnAddress;
    }

    public static Address toDTO(PnAddress address){
        return mapperToAddressEntity.toDTO(address);
    }

    public static AnalogAddress toPojo(DiscoveredAddressDto discoveredAddressDto){
        if (discoveredAddressDto == null) return null;

        AnalogAddress address = mapperToAnalog.toEntity(discoveredAddressDto);
        address.setFullname(discoveredAddressDto.getName());
        return address;
    }

    public static AnalogAddress toPojo(Address address){
        if (address == null) return null;
        return mapperAnalog.toDTO(address);
    }

    public static AnalogAddress fromEntity(PnAddress address){
        if (address == null) return null;
        return mapperAnalog.toDTO(toDTO(address));
    }

    public static AnalogAddressDto toAnalogAddressManager(Address address){
        AnalogAddressDto analogAddress = new AnalogAddressDto();
        analogAddress.setAddressRow(address.getAddress());
        analogAddress.setAddressRow2(address.getAddressRow2());
        analogAddress.setCap(address.getCap());
        analogAddress.setCity(address.getCity());
        analogAddress.setCity2(address.getCity2());
        analogAddress.setPr(address.getPr());
        analogAddress.setCountry(address.getCountry());
        return analogAddress;
    }

    public static Address fromAnalogAddressManager(AnalogAddressDto analogAddress){
        Address address = new Address();
        address.setAddress(analogAddress.getAddressRow());
        address.setAddressRow2(analogAddress.getAddressRow2());
        address.setCap(analogAddress.getCap());
        address.setCity(analogAddress.getCity());
        address.setCity2(analogAddress.getCity2());
        address.setPr(analogAddress.getPr());
        address.setCountry(analogAddress.getCountry());
        return address;
    }

}
