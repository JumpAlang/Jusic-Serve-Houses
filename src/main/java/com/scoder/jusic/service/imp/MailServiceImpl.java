package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.ProviderIsolationProperties;
import com.scoder.jusic.security.ProviderIsolated;
import com.scoder.jusic.security.ProviderWorkload;
import com.scoder.jusic.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author H
 */
@Service
@Slf4j
public class MailServiceImpl implements MailService {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private ProviderIsolationProperties providerIsolationProperties;

    public static boolean commonSendJ(String webUrl, String title, String desc)
            throws IOException {
        return commonSendJ(webUrl, title, desc, 3_000, 5_000);
    }

    static boolean commonSendJ(String webUrl, String title, String desc,
                               int connectTimeoutMs, int readTimeoutMs)
            throws IOException {
        String encoded = "text=" + URLEncoder.encode(title, "UTF-8")
                + "&desp=" + URLEncoder.encode(desc, "UTF-8");
        byte[] postData = encoded.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(webUrl).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setDoOutput(true);
            connection.setConnectTimeout(Math.max(1, connectTimeoutMs));
            connection.setReadTimeout(Math.max(1, readTimeoutMs));
            connection.setFixedLengthStreamingMode(postData.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(postData);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader input = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = input.readLine()) != null) {
                    response.append(line);
                }
            }
            return response.indexOf("\"errno\":0") >= 0;
        } finally {
            connection.disconnect();
        }
    }
    @ProviderIsolated(ProviderWorkload.BACKGROUND)
    public boolean sendServerJ(String text,String desc){
        try{
            String url = jusicProperties.getServerJUrl();
            ProviderIsolationProperties.Http http =
                    providerIsolationProperties.getHttp();
            return commonSendJ(url, text, desc, http.getConnectTimeoutMs(),
                    http.getReadTimeoutMs());
        }catch(Exception e){
            log.error("发送至Server酱失败：{}",e.getMessage());
        }
        return false;

    }
    @Override
    @ProviderIsolated(ProviderWorkload.BACKGROUND)
    public boolean sendSimpleMail(String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(jusicProperties.getMailSendFrom());
        message.setTo(jusicProperties.getMailSendTo());
        message.setSubject(subject);
        message.setText(content);

        try {
            javaMailSender.send(message);
            return true;
        } catch (Exception e) {
            log.info("邮件发送异常: {}", e.getMessage());
            return false;
        }
    }

}
