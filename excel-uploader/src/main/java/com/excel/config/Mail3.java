package com.excel.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class Mail3 {

    @Value("${spring.mail.host}")
    private String smtpHost;

    @Value("${spring.mail.port}")
    private String smtpPort;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Value("${spring.mail.password}")
    private String emailPassword;

    public void sendMailSSL(String to, String subject, String body) {

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");

            Session session = Session.getInstance(props,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(emailFrom, emailPassword);
                    }
                }
            );

//            Message message = new MimeMessage(session);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailFrom));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to)
            );

            // ⭐ PROFESSIONAL SUBJECT
            message.setSubject(subject, "UTF-8");


            message.setContent(body, "text/html");

            Transport.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    } 
}
