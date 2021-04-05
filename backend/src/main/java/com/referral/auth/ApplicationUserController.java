package com.referral.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.referral.model.Session;
import com.referral.service.SessionService;
import com.referral.utils.EmailVerificationUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;

// the request class for registering
@NoArgsConstructor
@AllArgsConstructor
class RegisterRequest {

    @JsonProperty
    @Getter
    private String email;

    @JsonProperty
    @Getter
    private String password;

    @JsonProperty
    @Getter
    private String code;

    @JsonProperty
    @Getter
    private String userName;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
    
    
}

@RestController
public class ApplicationUserController {

    @Autowired
    private ApplicationUserService applicationUserService;

    @Autowired
    private SessionService sessionService;

    public static final String CONFIG_PATH = "beans/SpringMail.xml";

    public static final String UTIL_NAME = "emailVerificationUtils";

    public static final int SESSION_INTERVAL = 1800;
    
    

    @GetMapping("/v1/users")
    public List<ApplicationUser> getAllUsers() {
        return applicationUserService.getAllUsers();
    }

    @PostMapping("/register")
    public ResponseEntity<String> addUser(HttpServletRequest request,
                                          @Valid @RequestBody RegisterRequest registerRequest) {
        String code = registerRequest.getCode();
        String email = registerRequest.getEmail();
        String password = registerRequest.getPassword();

        if (code == null || email == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient information");
        }

        Session existedSession = null;

        try {
            ApplicationUser user;
            if ((user = (ApplicationUser) applicationUserService.loadUserByUsername(email)) != null) {
                existedSession = sessionService.getSessionByUserEmail(user.getUsername());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unknown server error");
        }

        // check if this email is already registered
        if (applicationUserService.loadUserByUsername(email) != null || existedSession != null) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Email already exists");
        }

        // check if verification is incorrect
        HttpSession session = request.getSession();

        String verificationCode = (String) session.getAttribute(email);
        if (!code.equals(verificationCode)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Incorrect verification code");
        }

        // if all above are ok, add the user into database
        ApplicationUser newUser = new ApplicationUser(
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getUserName()
        );
        applicationUserService.addUser(newUser);
        return ResponseEntity.ok("User " + newUser.getUsername() + " created");
    }

    @RequestMapping(value = "/v1/send-verification", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> sendVerification(HttpServletRequest request,
                                                   HttpServletResponse response, @Valid @RequestBody RegisterRequest registerRequest) {

        // Get email from request body
        String email = registerRequest.getEmail();

        if (email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email can not be null");
        }

        HttpSession session = request.getSession();
        response.addHeader("Set-Cookie", session.getId());

        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(CONFIG_PATH)) {

            EmailVerificationUtils emailVerification = (EmailVerificationUtils) context.getBean(UTIL_NAME);

            // Generate verification code
            String code = emailVerification.generateVerificationCode();

            // Store the code in http session
            session.setAttribute(email, code);
            session.setMaxInactiveInterval(SESSION_INTERVAL);

            // Send email
            emailVerification.sendEmail(email, code);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fail to send email due to server error");
        }

        return ResponseEntity.ok("Email sent");
    }

}
