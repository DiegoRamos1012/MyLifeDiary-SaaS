package com.diegoramos.mylifediary.common.email;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String userName, String verificationUrl);
}
