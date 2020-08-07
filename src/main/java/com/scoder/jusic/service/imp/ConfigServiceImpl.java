package com.scoder.jusic.service.imp;

import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author H
 */
@Service
@Slf4j
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private ConfigRepository configRepository;

    @Override
    public void setPushSwitch(boolean pushSwitch,String houseId) {
        configRepository.setPushSwitch(pushSwitch,houseId);
    }

    @Override
    public void setEnableSwitch(boolean enableSwitch,String houseId) {
        configRepository.setEnableSwitch(enableSwitch,houseId);
    }

    @Override
    public void setEnableSearch(boolean enableSearch,String houseId) {
        configRepository.setEnableSearch(enableSearch,houseId);
    }

    @Override
    public Float getVoteRate(String houseId) {
        return configRepository.getVoteRate(houseId);
    }
    @Override
    public void setVoteRate(Float voteRate, String houseId) {
        configRepository.setVoteRate(voteRate,houseId);
    }

    @Override
    public Boolean getEnableSearch(String houseId) {
        return configRepository.getEnableSearch(houseId);
    }

    @Override
    public Boolean getEnableSwitch(String houseId) {
        return configRepository.getEnableSwitch(houseId);
    }

    @Override
    public Boolean getGoodModel(String houseId) {
        return configRepository.getGoodModel(houseId);
    }

    @Override
    public void setGoodModel(boolean goodModel,String houseId) {
        configRepository.setGoodModel(goodModel,houseId);
    }

    @Override
    public Boolean getRandomModel(String houseId) {
        return configRepository.getRandomModel(houseId);
    }

    @Override
    public void setRandomModel(boolean randomModel, String houseId) {
        configRepository.setRandomModel(randomModel,houseId);
    }
}
