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
    public void setPushSwitch(boolean pushSwitch) {
        configRepository.setPushSwitch(pushSwitch);
    }

    @Override
    public void setEnableSwitch(boolean enableSwitch) {
        configRepository.setEnableSwitch(enableSwitch);
    }

    @Override
    public void setEnableSearch(boolean enableSearch) {
        configRepository.setEnableSearch(enableSearch);
    }

    @Override
    public Boolean getEnableSearch() {
        return configRepository.getEnableSearch();
    }

    @Override
    public Boolean getEnableSwitch() {
        return configRepository.getEnableSwitch();
    }

    @Override
    public Boolean getGoodModel() {
        return configRepository.getGoodModel();
    }

    @Override
    public void setGoodModel(boolean goodModel) {
        configRepository.setGoodModel(goodModel);
    }
}
