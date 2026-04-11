package edu.cit.arnejo.dormshare.service;

import edu.cit.arnejo.dormshare.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.from:}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendDebtSettledReceipt(UserEntity payer,
                                      UserEntity debtor,
                                      BigDecimal amount,
                                      String expenseDescription) {
        if (!isSmtpConfigured()) {
            log.info("SMTP not configured; skipping settlement receipt emails");
            return;
        }

        String safeDesc = (expenseDescription == null || expenseDescription.isBlank())
                ? "Expense"
                : expenseDescription;
        String amountText = amount == null ? "0" : amount.toPlainString();
        String debtorName = debtor == null ? "Someone" : (debtor.getFirstName() + " " + debtor.getLastName());
        String payerName = payer == null ? "Someone" : (payer.getFirstName() + " " + payer.getLastName());

        if (payer != null && payer.getEmail() != null && !payer.getEmail().isBlank()) {
            send(
                    payer.getEmail(),
                    "DormShare: Debt settled receipt",
                    "Hi " + payerName + ",\n\n" +
                            debtorName + " settled ₱" + amountText + " for \"" + safeDesc + "\".\n\n" +
                            "— DormShare"
            );
        }

        if (debtor != null && debtor.getEmail() != null && !debtor.getEmail().isBlank()) {
            send(
                    debtor.getEmail(),
                    "DormShare: Payment confirmation",
                    "Hi " + debtorName + ",\n\n" +
                            "You marked ₱" + amountText + " as settled for \"" + safeDesc + "\" (paid to " + payerName + ").\n\n" +
                            "— DormShare"
            );
        }
    }

    public void sendDebtMarkedSettledByPayerReceipt(UserEntity payer,
                                                   UserEntity debtor,
                                                   BigDecimal amount,
                                                   String expenseDescription) {
        if (!isSmtpConfigured()) {
            log.info("SMTP not configured; skipping settlement receipt emails");
            return;
        }

        String safeDesc = (expenseDescription == null || expenseDescription.isBlank())
                ? "Expense"
                : expenseDescription;
        String amountText = amount == null ? "0" : amount.toPlainString();
        String debtorName = debtor == null ? "Someone" : (debtor.getFirstName() + " " + debtor.getLastName());
        String payerName = payer == null ? "Someone" : (payer.getFirstName() + " " + payer.getLastName());

        if (debtor != null && debtor.getEmail() != null && !debtor.getEmail().isBlank()) {
            send(
                    debtor.getEmail(),
                    "DormShare: Settlement status updated",
                    "Hi " + debtorName + ",\n\n" +
                            payerName + " marked your ₱" + amountText + " for \"" + safeDesc + "\" as settled.\n\n" +
                            "— DormShare"
            );
        }

        if (payer != null && payer.getEmail() != null && !payer.getEmail().isBlank()) {
            send(
                    payer.getEmail(),
                    "DormShare: Settlement status updated",
                    "Hi " + payerName + ",\n\n" +
                            "You marked " + debtorName + "'s ₱" + amountText + " for \"" + safeDesc + "\" as settled.\n\n" +
                            "— DormShare"
            );
        }
    }

    private boolean isSmtpConfigured() {
        return smtpHost != null && !smtpHost.isBlank() &&
                fromAddress != null && !fromAddress.isBlank();
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
