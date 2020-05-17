package com.scoder.jusic.configuration;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author H
 */
@Component
public class JusicEnvironment {

    private final Environment env;

    public JusicEnvironment(Environment env) {
        this.env = env;
    }

    /**
     * 获取服务端口
     *
     * @return port
     */
    public Integer getServerPort() {
        String result = env.getProperty("server.port");
        if (null == result) {
            return 8080;
        }
        return Integer.valueOf(result);
    }
}
