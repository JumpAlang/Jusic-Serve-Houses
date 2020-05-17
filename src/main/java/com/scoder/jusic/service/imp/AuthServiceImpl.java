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
    public boolean authRoot(String sessionId, String password) {
        if (null == password) {
            return false;
        }
        String rootPassword = configRepository.getRootPassword();
        if (null == rootPassword) {
            rootPassword = jusicProperties.getRoleRootPassword();
            configRepository.initRootPassword();
        }
        if (password.equals(rootPassword)) {
            // update role
            User user = sessionRepository.getSession(sessionId);
            user.setRole("root");
            sessionRepository.setSession(user);

            return true;
        }
        return false;
    }

    @Override
    public boolean authAdmin(String sessionId, String password) {
        if (null == password) {
            return false;
        }
        String adminPassword = configRepository.getAdminPassword();
        if (null == adminPassword) {
            adminPassword = jusicProperties.getRoleRootPassword();
            configRepository.initAdminPassword();
        }
        if (password.equals(adminPassword)) {
            // update role
            User user = sessionRepository.getSession(sessionId);
            user.setRole("admin");
            sessionRepository.setSession(user);

            return true;
        }
        return false;
    }

}
