package com.certichain.server.aop;

import com.certichain.core.model.Certificate;
import com.certichain.server.model.AdminSession;
import com.certichain.server.repository.AuditRepository;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Cross-cutting audit trail. Business code never writes audit rows itself: any method
 * annotated with {@link Audited} is wrapped here, timed, and logged with who called it
 * and whether it succeeded. Its high precedence keeps it outside the transaction proxy,
 * so a rolled-back operation is still recorded as FAILED.
 */
@Aspect
@Component
@Order(AuditAspect.ORDER)
public class AuditAspect {

    /**
     * Before the transaction advisor (LOWEST_PRECEDENCE), but after Spring's
     * ExposeInvocationInterceptor, which must run first for annotation binding to work.
     */
    static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditRepository auditRepository;

    public AuditAspect(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        long start = System.nanoTime();
        String actor = currentActor();
        String detail = describe(pjp.getArgs());
        try {
            Object result = pjp.proceed();
            long ms = (System.nanoTime() - start) / 1_000_000;
            auditRepository.log(actor, audited.value(), describeResult(result, detail), "SUCCESS", ms);
            log.info("AUDIT {} by {} in {} ms", audited.value(), actor, ms);
            return result;
        } catch (Throwable t) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            auditRepository.log(actor, audited.value(), detail + " -> " + t.getMessage(), "FAILED", ms);
            log.warn("AUDIT {} by {} FAILED: {}", audited.value(), actor, t.getMessage());
            throw t;
        }
    }

    static String currentActor() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpSession session = attrs.getRequest().getSession(false);
            if (session != null && session.getAttribute(AdminSession.ATTR) instanceof AdminSession admin) {
                return admin.getUsername();
            }
            return "anonymous@" + attrs.getRequest().getRemoteAddr();
        }
        return "SYSTEM";
    }

    private static String describe(Object[] args) {
        return Arrays.stream(args).map(AuditAspect::describeArg).collect(Collectors.joining(", "));
    }

    private static String describeArg(Object arg) {
        if (arg instanceof Certificate c) {
            return c.getStudentName() + " / " + c.getTitle();
        }
        return String.valueOf(arg);
    }

    private static String describeResult(Object result, String fallback) {
        if (result instanceof Certificate c) {
            return c.getCertNo() + " - " + c.getStudentName() + " / " + c.getTitle();
        }
        return fallback;
    }
}
