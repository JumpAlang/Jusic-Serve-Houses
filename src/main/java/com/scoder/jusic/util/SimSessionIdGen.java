package com.scoder.jusic.util;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author JumpAlang
 * @create 2020-06-10 11:51
 */
public class SimSessionIdGen implements IdGenerator {

        private final AtomicLong mostSigBits = new AtomicLong(0);

        private final AtomicLong leastSigBits = new AtomicLong(0);


        @Override
        public UUID generateId() {
            long leastSigBits = this.leastSigBits.incrementAndGet();
            if (leastSigBits == 0) {
                this.mostSigBits.incrementAndGet();
            }
            return new UUID(this.mostSigBits.get(), leastSigBits);
        }

    public static void main(String[] args) {
        System.out.println(new SimSessionIdGen().generateId());
        IdGenerator gen = new IdGenerator() {
            @Override
            public UUID generateId() {
                return MessageHeaders.ID_VALUE_NONE;
            }
        };
        System.out.println(gen.generateId());
        gen = new AlternativeJdkIdGenerator();
        System.out.println(gen.generateId());
    }
}
