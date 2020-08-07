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
    void setPushSwitch(boolean pushSwitch,String houseId);

    void setEnableSwitch(boolean enableSwitch,String houseId);

    void setEnableSearch(boolean enableSearch,String houseId);
    void setVoteRate(Float voteRate,String houseId);

    Boolean getEnableSearch(String houseId);

    Boolean getEnableSwitch(String houseId);


    Boolean getGoodModel(String houseId);
    void setGoodModel(boolean goodModel,String houseId);

    Boolean getRandomModel(String houseId);
    void setRandomModel(boolean goodModel,String houseId);

    Float getVoteRate(String houseId);
}
