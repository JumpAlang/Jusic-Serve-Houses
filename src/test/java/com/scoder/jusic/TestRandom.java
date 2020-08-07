package com.scoder.jusic;

import com.scoder.jusic.util.RandomUtils;

/**
 * @author JumpAlang
 * @create 2020-08-07 16:23
 */
public class TestRandom {
    public static void main(String[] args) {
        for(int i = 0; i < 10; i++){
            System.out.println(RandomUtils.getRandNumber(10));
        }
    }
}
