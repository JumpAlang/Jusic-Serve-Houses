package com.scoder.jusic.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author JumpAlang
 * @create 2020-08-07 14:18
 */
public class RandomUtils {
    private enum RandomHolderEnum{
        HOLDER;
        private Random random;
        private RandomHolderEnum(){
            random = ThreadLocalRandom.current();
        }
    }

    private static Random getRandom(){
        return RandomHolderEnum.HOLDER.random;
    }
    /**
     * 生成一个<n的随机正整数
     * @param n
     * @return
     */
    public static int getRandNumber(int n) {
       Random random = new Random();
       return random.nextInt(n);
    }
}
