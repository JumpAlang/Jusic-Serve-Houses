package com.scoder.jusic.service;

/**
 * @author H
 */
public interface AuthService {

    /**
     * root 认证
     *
     * @param sessionId the client session id
     * @param password password
     * @return 成功返回 true，失败返回 false
     */
    boolean authRoot(String sessionId, String password);

    /**
     * admin 认证
     *
     * @param sessionId the client session id
     * @param password password
     * @return 成功返回 true，失败返回 false
     */
    boolean authAdmin(String sessionId, String password);


}
