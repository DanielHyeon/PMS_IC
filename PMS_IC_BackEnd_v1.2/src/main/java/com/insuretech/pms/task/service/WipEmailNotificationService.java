package com.insuretech.pms.task.service;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service for sending email notifications when WIP violations occur
 * Supports soft limit warnings and hard limit violations
 */
@Service
@RequiredArgsConstructor
public class WipEmailNotificationService {

    private static final Logger logger = Logger.getLogger(WipEmailNotificationService.class.getName());

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${spring.mail.from:noreply@insuretech.com}")
    private String fromEmail;

    @Value("${app.name:PMS Insurance Claims}")
    private String appName;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Send soft limit warning email
     * Called when column WIP reaches soft limit (warning threshold)
     */
    @Async
    @Transactional(readOnly = true)
    public void sendSoftLimitWarning(String projectId, String columnId, String columnName,
                                      int currentWip, int softLimit, String projectManagerId) {
        try {
            Optional<User> pmOpt = userRepository.findById(projectManagerId);
            if (pmOpt.isEmpty()) {
                logger.warning("Project manager not found: " + projectManagerId);
                return;
            }

            User pm = pmOpt.get();
            String subject = String.format("[WARNING] WIP Soft Limit Approaching - %s", columnName);

            String htmlContent = buildSoftLimitHtmlTemplate(
                    projectId, columnName, currentWip, softLimit,
                    pm.getName(), appUrl
            );

            sendHtmlEmail(pm.getEmail(), subject, htmlContent);
            logger.info("Sent soft limit warning to " + pm.getEmail() + " for column " + columnName);
        } catch (Exception e) {
            logger.severe("Error sending soft limit warning email for column " + columnName + ": " + e.getMessage());
        }
    }

    /**
     * Send hard limit violation email
     * Called when column WIP exceeds hard limit (blocking threshold)
     */
    @Async
    @Transactional(readOnly = true)
    public void sendHardLimitViolation(String projectId, String columnId, String columnName,
                                        int currentWip, int hardLimit, String projectManagerId) {
        try {
            Optional<User> pmOpt = userRepository.findById(projectManagerId);
            if (pmOpt.isEmpty()) {
                logger.warning("Project manager not found: " + projectManagerId);
                return;
            }

            User pm = pmOpt.get();
            String subject = String.format("[CRITICAL] WIP Hard Limit Exceeded - %s", columnName);

            String htmlContent = buildHardLimitHtmlTemplate(
                    projectId, columnName, currentWip, hardLimit,
                    pm.getName(), appUrl
            );

            sendHtmlEmail(pm.getEmail(), subject, htmlContent);
            logger.info("Sent hard limit violation to " + pm.getEmail() + " for column " + columnName);
        } catch (Exception e) {
            logger.severe("Error sending hard limit violation email for column " + columnName + ": " + e.getMessage());
        }
    }

    /**
     * Send bottleneck detection alert
     * Called when workflow bottleneck is detected
     */
    @Async
    @Transactional(readOnly = true)
    public void sendBottleneckAlert(String projectId, String columnId, String columnName,
                                     int blockingTasks, int affectedTasks, String projectManagerId) {
        try {
            Optional<User> pmOpt = userRepository.findById(projectManagerId);
            if (pmOpt.isEmpty()) {
                logger.warning("Project manager not found: " + projectManagerId);
                return;
            }

            User pm = pmOpt.get();
            String subject = String.format("[ALERT] Workflow Bottleneck Detected - %s", columnName);

            String htmlContent = buildBottleneckHtmlTemplate(
                    projectId, columnName, blockingTasks, affectedTasks,
                    pm.getName(), appUrl
            );

            sendHtmlEmail(pm.getEmail(), subject, htmlContent);
            logger.info("Sent bottleneck alert to " + pm.getEmail() + " for column " + columnName);
        } catch (Exception e) {
            logger.severe("Error sending bottleneck alert email for column " + columnName + ": " + e.getMessage());
        }
    }

    /**
     * Send sprint CONWIP violation alert
     * Called when sprint constant WIP limit is exceeded
     */
    @Async
    @Transactional(readOnly = true)
    public void sendSprintConwipViolation(String projectId, String sprintId, String sprintName,
                                           int currentWip, int conwipLimit, String projectManagerId) {
        try {
            Optional<User> pmOpt = userRepository.findById(projectManagerId);
            if (pmOpt.isEmpty()) {
                logger.warning("Project manager not found: " + projectManagerId);
                return;
            }

            User pm = pmOpt.get();
            String subject = String.format("[WARNING] Sprint CONWIP Limit Exceeded - %s", sprintName);

            String htmlContent = buildConwipHtmlTemplate(
                    projectId, sprintName, currentWip, conwipLimit,
                    pm.getName(), appUrl
            );

            sendHtmlEmail(pm.getEmail(), subject, htmlContent);
            logger.info("Sent CONWIP violation to " + pm.getEmail() + " for sprint " + sprintName);
        } catch (Exception e) {
            logger.severe("Error sending CONWIP violation email for sprint " + sprintName + ": " + e.getMessage());
        }
    }

    /**
     * Send personal WIP limit violation
     * Called when assignee exceeds personal WIP limit
     */
    @Async
    @Transactional(readOnly = true)
    public void sendPersonalWipViolation(String projectId, String assigneeId, String assigneeName,
                                          int currentWip, int maxWip) {
        try {
            Optional<User> assigneeOpt = userRepository.findById(assigneeId);
            if (assigneeOpt.isEmpty()) {
                logger.warning("Assignee not found: " + assigneeId);
                return;
            }

            User assignee = assigneeOpt.get();
            String subject = String.format("[NOTICE] Your WIP Limit Exceeded - %s", assigneeName);

            String htmlContent = buildPersonalWipHtmlTemplate(
                    projectId, assigneeName, currentWip, maxWip,
                    assignee.getName(), appUrl
            );

            sendHtmlEmail(assignee.getEmail(), subject, htmlContent);
            logger.info("Sent personal WIP violation to " + assignee.getEmail() + " for assignee " + assigneeName);
        } catch (Exception e) {
            logger.severe("Error sending personal WIP violation email for assignee " + assigneeName + ": " + e.getMessage());
        }
    }

    /**
     * Send email to multiple recipients (team notification)
     */
    @Async
    public void sendTeamNotification(List<String> recipientEmails, String subject, String htmlContent) {
        try {
            for (String email : recipientEmails) {
                sendHtmlEmail(email, subject, htmlContent);
            }
            logger.info("Sent team notification to " + recipientEmails.size() + " recipients");
        } catch (Exception e) {
            logger.severe("Error sending team notification: " + e.getMessage());
        }
    }

    /**
     * Send HTML email
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Build HTML template for soft limit warning
     */
    private String buildSoftLimitHtmlTemplate(String projectId, String columnName, int currentWip, int softLimit,
                                               String pmName, String appUrl) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        int percentage = (int) ((currentWip * 100) / softLimit);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f59e0b; color: white; padding: 20px; border-radius: 4px 4px 0 0; }
                    .content { background-color: #fff8f0; padding: 20px; border: 1px solid #fed7aa; }
                    .metric { background-color: white; padding: 15px; margin: 10px 0; border-left: 4px solid #f59e0b; }
                    .metric-label { color: #666; font-size: 12px; }
                    .metric-value { font-size: 24px; font-weight: bold; color: #f59e0b; }
                    .button { display: inline-block; background-color: #3b82f6; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                    .footer { color: #999; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>⚠️ WIP Soft Limit Warning</h2>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>The <strong>%s</strong> column is approaching its soft WIP limit.</p>

                        <div class="metric">
                            <div class="metric-label">Current WIP Items</div>
                            <div class="metric-value">%d / %d</div>
                        </div>

                        <div class="metric">
                            <div class="metric-label">Utilization</div>
                            <div class="metric-value">%d%%</div>
                        </div>

                        <p>Please review the kanban board and consider starting work on the next items or completing existing work to bring the WIP count below the soft limit.</p>

                        <a href="%s/projects/%s/board" class="button">View Kanban Board</a>

                        <div class="footer">
                            <p>Timestamp: %s</p>
                            <p>This is an automated notification from %s</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, pmName, columnName, currentWip, softLimit, percentage, appUrl, projectId, timestamp, appName);
    }

    /**
     * Build HTML template for hard limit violation
     */
    private String buildHardLimitHtmlTemplate(String projectId, String columnName, int currentWip, int hardLimit,
                                               String pmName, String appUrl) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        int percentage = (int) ((currentWip * 100) / hardLimit);
        int exceeded = currentWip - hardLimit;

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #ef4444; color: white; padding: 20px; border-radius: 4px 4px 0 0; }
                    .content { background-color: #fef2f2; padding: 20px; border: 1px solid #fecaca; }
                    .metric { background-color: white; padding: 15px; margin: 10px 0; border-left: 4px solid #ef4444; }
                    .metric-label { color: #666; font-size: 12px; }
                    .metric-value { font-size: 24px; font-weight: bold; color: #ef4444; }
                    .button { display: inline-block; background-color: #ef4444; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                    .footer { color: #999; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>🚨 WIP Hard Limit Exceeded - CRITICAL</h2>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>The <strong>%s</strong> column has <strong>EXCEEDED</strong> its hard WIP limit. New work cannot start until items are completed.</p>

                        <div class="metric">
                            <div class="metric-label">Current WIP Items</div>
                            <div class="metric-value">%d / %d</div>
                        </div>

                        <div class="metric">
                            <div class="metric-label">Exceeded By</div>
                            <div class="metric-value">%d items</div>
                        </div>

                        <div class="metric">
                            <div class="metric-label">Over Limit</div>
                            <div class="metric-value">%d%%</div>
                        </div>

                        <p><strong>Immediate action required:</strong> You must complete or move items from this column before new work can be added.</p>

                        <a href="%s/projects/%s/board" class="button">Go to Kanban Board</a>

                        <div class="footer">
                            <p>Timestamp: %s</p>
                            <p>This is an automated critical alert from %s</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, pmName, columnName, currentWip, hardLimit, exceeded, percentage, appUrl, projectId, timestamp, appName);
    }

    /**
     * Build HTML template for bottleneck alert
     */
    private String buildBottleneckHtmlTemplate(String projectId, String columnName, int blockingTasks, int affectedTasks,
                                                String pmName, String appUrl) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #8b5cf6; color: white; padding: 20px; border-radius: 4px 4px 0 0; }
                    .content { background-color: #faf5ff; padding: 20px; border: 1px solid #e9d5ff; }
                    .metric { background-color: white; padding: 15px; margin: 10px 0; border-left: 4px solid #8b5cf6; }
                    .metric-label { color: #666; font-size: 12px; }
                    .metric-value { font-size: 24px; font-weight: bold; color: #8b5cf6; }
                    .button { display: inline-block; background-color: #8b5cf6; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                    .footer { color: #999; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>🔴 Workflow Bottleneck Detected</h2>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>A bottleneck has been detected in the <strong>%s</strong> column.</p>

                        <div class="metric">
                            <div class="metric-label">Blocking Tasks</div>
                            <div class="metric-value">%d</div>
                        </div>

                        <div class="metric">
                            <div class="metric-label">Affected Tasks</div>
                            <div class="metric-value">%d</div>
                        </div>

                        <p>This bottleneck is preventing %d downstream tasks from progressing. Please investigate blocking issues or allocate additional resources.</p>

                        <a href="%s/projects/%s/board" class="button">Review Details</a>

                        <div class="footer">
                            <p>Timestamp: %s</p>
                            <p>This is an automated alert from %s</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, pmName, columnName, blockingTasks, affectedTasks, affectedTasks, appUrl, projectId, timestamp, appName);
    }

    /**
     * Build HTML template for CONWIP violation
     */
    private String buildConwipHtmlTemplate(String projectId, String sprintName, int currentWip, int conwipLimit,
                                            String pmName, String appUrl) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        int percentage = (int) ((currentWip * 100) / conwipLimit);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f59e0b; color: white; padding: 20px; border-radius: 4px 4px 0 0; }
                    .content { background-color: #fff8f0; padding: 20px; border: 1px solid #fed7aa; }
                    .metric { background-color: white; padding: 15px; margin: 10px 0; border-left: 4px solid #f59e0b; }
                    .metric-label { color: #666; font-size: 12px; }
                    .metric-value { font-size: 24px; font-weight: bold; color: #f59e0b; }
                    .button { display: inline-block; background-color: #3b82f6; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                    .footer { color: #999; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>⚠️ Sprint CONWIP Limit Exceeded</h2>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>The sprint <strong>%s</strong> has exceeded its constant WIP (CONWIP) limit.</p>

                        <div class="metric">
                            <div class="metric-label">Current WIP</div>
                            <div class="metric-value">%d / %d</div>
                        </div>

                        <div class="metric">
                            <div class="metric-label">Utilization</div>
                            <div class="metric-value">%d%%</div>
                        </div>

                        <p>Please reduce the number of concurrent work items to improve flow and throughput.</p>

                        <a href="%s/projects/%s/planning" class="button">View Sprint Planning</a>

                        <div class="footer">
                            <p>Timestamp: %s</p>
                            <p>This is an automated notification from %s</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, pmName, sprintName, currentWip, conwipLimit, percentage, appUrl, projectId, timestamp, appName);
    }

    /**
     * Build HTML template for personal WIP violation
     */
    private String buildPersonalWipHtmlTemplate(String projectId, String assigneeName, int currentWip, int maxWip,
                                                 String recipientName, String appUrl) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        int percentage = (int) ((currentWip * 100) / maxWip);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #06b6d4; color: white; padding: 20px; border-radius: 4px 4px 0 0; }
                    .content { background-color: #ecf9fd; padding: 20px; border: 1px solid #a5f3fc; }
                    .metric { background-color: white; padding: 15px; margin: 10px 0; border-left: 4px solid #06b6d4; }
                    .metric-label { color: #666; font-size: 12px; }
                    .metric-value { font-size: 24px; font-weight: bold; color: #06b6d4; }
                    .button { display: inline-block; background-color: #06b6d4; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                    .footer { color: #999; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>📋 Your Personal WIP Limit Exceeded</h2>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>You currently have more items in progress than your personal WIP limit of %d.</p>

                        <div class="metric">
                            <div class="metric-label">Your Current WIP</div>
                            <div class="metric-value">%d / %d</div>
                        </div>

                        <div class="metric">
                            <div class="metric-label">Utilization</div>
                            <div class="metric-value">%d%%</div>
                        </div>

                        <p>Please focus on completing your current tasks before starting new ones to improve productivity and reduce context switching.</p>

                        <a href="%s/projects/%s/board" class="button">View My Tasks</a>

                        <div class="footer">
                            <p>Timestamp: %s</p>
                            <p>This is an automated notification from %s</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, recipientName, maxWip, currentWip, maxWip, percentage, appUrl, projectId, timestamp, appName);
    }
}
