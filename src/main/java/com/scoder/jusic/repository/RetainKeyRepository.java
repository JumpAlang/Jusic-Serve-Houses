package com.scoder.jusic.repository;

import com.scoder.jusic.model.RetainKey;

import java.util.List;

/**
 * @author JumpAlang
 * @create 2020-07-06 16:23
 */
public interface RetainKeyRepository {

    List showRetainKey();

    void addRetainKey(RetainKey retainKey);

    void removeRetainKey(String retainKey);

    RetainKey getRetainKey(String retainKey);

    void updateRetainKey(RetainKey retainKey);

}
