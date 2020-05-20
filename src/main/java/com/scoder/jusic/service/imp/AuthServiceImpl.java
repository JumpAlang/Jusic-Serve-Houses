package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.repository.SessionRepository;
import com.scoder.jusic.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author H
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private ConfigRepository configRepository;

    @Override
    public boolean authRoot(String sessionId, String password,String houseId) {
        if (null == password) {
            return false;
        }
        String rootPassword = configRepository.getRootPassword(houseId);
        if (null == rootPassword) {
            rootPassword = jusicProperties.getRoleRootPassword();
            configRepository.initRootPassword(houseId);
        }
        if (password.equals(rootPassword)) {
            // update role
            User user = sessionRepository.getSession(sessionId,houseId);
            user.setRole("root");
            sessionRepository.setSession(user,houseId);

            return true;
        }
        return false;
    }

    @Override
    public boolean authAdmin(String sessionId, String password,String houseId) {
        if (null == password) {
            return false;
        }
        String adminPassword = configRepository.getAdminPassword(houseId);
        if (null == adminPassword) {
            adminPassword = jusicProperties.getRoleRootPassword();
            configRepository.initAdminPassword(houseId);
        }
        if (password.equals(adminPassword)) {
            // update role
            User user = sessionRepository.getSession(sessionId,houseId);
            user.setRole("admin");
            sessionRepository.setSession(user,houseId);

            return true;
        }
        return false;
    }

}
