package com.scoder.jusic.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * @author H
 */
@Component
@Slf4j
public class JusicDisposable implements DisposableBean {

    private final HouseContainer houseContainer;
    private final LifecycleProperties lifecycleProperties;

    public JusicDisposable(HouseContainer houseContainer,
                           LifecycleProperties lifecycleProperties) {
        this.houseContainer = houseContainer;
        this.lifecycleProperties = lifecycleProperties;
    }

    @Override
    public void destroy() throws Exception {
        if (!lifecycleProperties.isCleanupLegacyStateOnShutdown()) {
            log.info("节点关闭，已跳过会删除房间共享状态的 legacy cleanup");
            return;
        }
        log.warn("legacy shutdown cleanup is enabled; do not use this mode with multiple nodes");
        houseContainer.destroy();
    }

}
