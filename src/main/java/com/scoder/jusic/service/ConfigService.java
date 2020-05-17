package com.scoder.jusic.service;

/**
 * @author H
 */
public interface ConfigService {

    /**
     * set push switch
     *
     * @param pushSwitch boolean
     */
    void setPushSwitch(boolean pushSwitch);

    void setEnableSwitch(boolean enableSwitch);

    void setEnableSearch(boolean enableSearch);

    Boolean getEnableSearch();

    Boolean getEnableSwitch();


    Boolean getGoodModel();
    void setGoodModel(boolean goodModel);

}
