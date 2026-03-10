package com.maher.booking_system.model;

import java.util.ArrayList;
import java.util.List;

public class LegalContent {

    private List<LegalField> impressumFields = new ArrayList<>();
    private List<LegalField> datenschutzFields = new ArrayList<>();

    public List<LegalField> getImpressumFields() {
        return impressumFields;
    }

    public void setImpressumFields(List<LegalField> impressumFields) {
        this.impressumFields = impressumFields;
    }

    public List<LegalField> getDatenschutzFields() {
        return datenschutzFields;
    }

    public void setDatenschutzFields(List<LegalField> datenschutzFields) {
        this.datenschutzFields = datenschutzFields;
    }
}
