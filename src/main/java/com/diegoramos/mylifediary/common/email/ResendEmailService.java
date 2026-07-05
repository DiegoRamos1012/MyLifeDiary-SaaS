package com.diegoramos.mylifediary.common.email;

import com.diegoramos.mylifediary.common.exception.DomainException;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);
    private final ResendProperties properties;
    private final TemplateEngine templateEngine;
    private final Resend resend;

    public ResendEmailService(ResendProperties properties, TemplateEngine templateEngine, Resend resend) {
        this.properties = properties;
        this.templateEngine = templateEngine;
        this.resend = resend;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String userName, String verificationUrl) {
        Context context = new Context();
        String firstName = userName.split(" ")[0];
        context.setVariable("userName", firstName);
        context.setVariable("verificationUrl", verificationUrl);

        String htmlBody = templateEngine.process("email-verification", context);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(properties.fromEmail())
                .to(toEmail)
                .subject("Verifique seu e-mail - Solenia")
                .html(htmlBody)
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            log.error("Erro ao enviar e-mail de verificação: {}", e.getMessage(), e);
            throw new DomainException("Erro ao enviar e-mail de verificação");
        }
    }
}
