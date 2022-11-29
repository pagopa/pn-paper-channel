package it.pagopa.pn.paperchannel.mapper.common;

import org.modelmapper.ModelMapper;

public class BaseMapperImpl<E, D> implements BaseMapper<E, D> {
    private final ModelMapper mapper = new ModelMapper();
    private final Class<E> eClass;
    private final Class<D> dClass;

    public BaseMapperImpl(Class<E> eClass, Class<D> dClass){
        this.eClass = eClass;
        this.dClass = dClass;
    }

    @Override
    public D toDTO(E entity) {
        return mapper.map(entity, dClass);
    }

    @Override
    public E toEntity(D dto) {
        return mapper.map(dto, eClass);
    }
}
