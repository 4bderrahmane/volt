package com.volt.catalog.application.usecase;

import com.volt.catalog.application.port.in.ListReferenceDataUseCase;
import com.volt.catalog.application.port.out.ReferenceDataRepositoryPort;
import com.volt.catalog.domain.model.Brand;
import com.volt.catalog.domain.model.Category;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReferenceDataQueryService implements ListReferenceDataUseCase {

    private final ReferenceDataRepositoryPort referenceData;

    public ReferenceDataQueryService(ReferenceDataRepositoryPort referenceData) {
        this.referenceData = referenceData;
    }

    @Override
    public List<Category> listCategories() {
        return referenceData.findAllCategories();
    }

    @Override
    public List<Brand> listBrands() {
        return referenceData.findAllBrands();
    }
}
