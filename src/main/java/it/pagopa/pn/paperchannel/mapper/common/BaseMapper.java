package it.pagopa.pn.paperchannel.mapper.common;

public interface BaseMapper<E, D>{

    E toEntity(D dto);
    D toDTO(E entity);

}
