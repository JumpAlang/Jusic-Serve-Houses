package com.scoder.jusic.service;

/**
 * @author H
 */
public interface MailService {
    /**
     * send simple mail
     *
     * @param subject subject
     * @param content content
     * @return boolean
     */
    boolean sendSimpleMail(String subject, String content);
    boolean sendServerJ(String text,String desc);
}
