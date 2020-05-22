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

    public JusicDisposable(HouseContainer houseContainer) {
        this.houseContainer = houseContainer;
    }

    @Override
    public void destroy() throws Exception {
        log.info("销毁工作开始");
        houseContainer.destroy();
        log.info("销毁工作完成");
    }

}
