package com.accionmfb.omnix.savings.target_saving.constant;

public enum AccountProductType
{
    SAVEBRIGHTA("14", "6002");

    public String productCode;
    public String categoryCode;

    AccountProductType(String productCode, String categoryCode){
        this.productCode = productCode;
        this.categoryCode = categoryCode;
    }
}
