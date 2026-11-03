package com.certichain.server.web.servlet;

import com.certichain.core.model.Certificate;
import com.certichain.server.service.CertificateService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;

/**
 * Raw servlet streaming the certificate register as CSV. Lives under /admin, so the
 * {@code AdminSecurityFilter} protects it. Shows the servlet life cycle (init) and how a
 * plain servlet looks up Spring beans from the web application context.
 */
@WebServlet(name = "csvExportServlet", urlPatterns = "/admin/export/certificates.csv")
public class CsvExportServlet extends HttpServlet {

    private CertificateService certificateService;

    @Override
    public void init() throws ServletException {
        WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        certificateService = ctx.getBean(CertificateService.class);
        log("CsvExportServlet initialised");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/csv;charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"certichain-register-" + LocalDate.now() + ".csv\"");
        PrintWriter out = resp.getWriter();
        out.println("cert_no,student_name,roll_no,email,program,credential_type,title,grade,issue_date,status,data_hash");
        for (Certificate c : certificateService.all()) {
            out.println(String.join(",", csv(c.getCertNo()), csv(c.getStudentName()), csv(c.getRollNo()), csv(c.getEmail()),
                    csv(c.getProgram()), csv(c.getCredentialType().name()), csv(c.getTitle()), csv(c.getGrade()),
                    csv(c.getIssueDate().toString()), csv(c.getStatus().name()), csv(c.getDataHash())));
        }
    }

    /** RFC 4180 quoting, plus a leading quote on values Excel would run as formulas. */
    private static String csv(String v) {
        if (v == null) {
            return "";
        }
        String s = v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}
