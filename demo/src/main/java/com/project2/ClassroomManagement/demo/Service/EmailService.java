package com.project2.ClassroomManagement.demo.Service;

import com.project2.ClassroomManagement.demo.util.TrustAllCertificates;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    @PostConstruct
    public void init() {
        // Install trust for all certificates when the service is initialized
        TrustAllCertificates.install();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendVerificationEmail(String email, String verificationCode) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(email);
        helper.setSubject("Xác minh email để hoàn tất đăng ký");
        String emailContent = "<h1>Mã xác minh email của bạn</h1>" +
                "<p><strong>Bạn cần xác minh email để hoàn tất quá trình đăng ký!</strong></p>" +
                "<p>Sử dụng mã xác minh sau để hoàn tất quá trình đăng ký của bạn:</p>" +
                "<h2 style='background-color: #f2f2f2; padding: 10px; text-align: center; font-size: 24px; letter-spacing: 5px;'>" + verificationCode + "</h2>" +
                "<p>Mã này sẽ hết hạn sau 10 phút.</p>" +
                "<p>Vui lòng nhập mã này vào trang xác thực email để kích hoạt tài khoản của bạn.</p>" +
                "<p><strong>Lưu ý:</strong> Không chia sẻ mã này với bất kỳ ai.</p>" +
                "<p>Xin cảm ơn bạn đã đăng ký!</p>";
        helper.setText(emailContent, true);

        mailSender.send(message);
        logger.info("Verification email with code sent to {}", email);
    }
}
