package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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

//    public static void main(String[] args) {
//        RestTemplate restTemplate = new RestTemplate();
//        String url = "https://sc.ftqq.com/SCU64668T909ada7955daadfb64d5e7652b93fb135dad06e659369.send";
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
//        map.add("text", "844072586@qq.com");
//        map.add("desp","我是中国人");
//        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
//        ResponseEntity<String> response = restTemplate.postForEntity( url, request , String.class );
//        System.out.println(response.getBody());
//    }
    public boolean sendServerJ(String text,String desc){
        try{
            RestTemplate restTemplate = new RestTemplate();
            String url = jusicProperties.getServerJUrl();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
            map.add("text", text);
            map.add("desp",desc);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity( url, request , String.class );
            if(response.getBody().indexOf("success") != -1){
                return true;
            }
        }catch(Exception e){
            log.error("发送至Server酱失败：{}",e.getLocalizedMessage());
        }
        return false;

    }
    @Override
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
