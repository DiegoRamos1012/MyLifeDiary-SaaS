package com.diegoramos.mylifediary.common.email.service;

import com.diegoramos.mylifediary.common.email.ResendEmailService;
import com.diegoramos.mylifediary.common.email.ResendProperties;
import com.diegoramos.mylifediary.common.exception.DomainException;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ResendEmailServiceTest {

    @Mock
    private Resend resend;

    @InjectMocks
    private ResendEmailService emailService;

    @Mock
    private Emails emails;

    @Mock
    private ResendProperties properties;

    @Mock
    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        when(resend.emails()).thenReturn(emails);
        when(properties.fromEmail()).thenReturn("noreply@solenia.app");
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>email</html>");
    }

    @Test
    void mustSendVerificationEmailSuccessfully() throws ResendException {
        String toEmail = "diego@email.com";
        String userName = "Diego";
        String verificationUrl = "http://localhost:8080/users/verify-email?token=abc123";

        emailService.sendVerificationEmail(toEmail, userName, verificationUrl);

        verify(templateEngine).process(eq("email-verification"), any(Context.class));
        verify(emails).send(any(CreateEmailOptions.class));
    }

    @Test
    void mustThrowDomainExceptionWhenResendFails() throws ResendException {
        when(emails.send(any(CreateEmailOptions.class)))
                .thenThrow(new ResendException("Simulated error"));

        assertThrows(DomainException.class, () ->
                emailService.sendVerificationEmail("guinho@email.com", "Guinho", "http://localhost:8080/users/verify-email?token=abc123")
        );
    }
}
