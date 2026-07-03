package com.diegoramos.mylifediary.common.email;

import com.diegoramos.mylifediary.common.exception.DomainException;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ResendEmailService implements EmailService {

    private final ResendProperties properties;
    private final TemplateEngine templateEngine;

    public ResendEmailService(ResendProperties properties, TemplateEngine templateEngine) {
        this.properties = properties;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String userName, String verificationUrl) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("verificationUrl", verificationUrl);

        String htmlBody = templateEngine.process("email-verification", context);

        Resend resend = new Resend(properties.apiKey());

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(properties.fromEmail())
                .to(toEmail)
                .subject("Verifique seu e-mail - Solenia")
                .html(htmlBody)
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            throw new DomainException("Erro ao enviar e-mail de verificação");
        }
    }
}
