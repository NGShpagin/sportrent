package ru.saschoolsport.user.config;

import ru.saschoolsport.user.dto.RegisterRequest;
import ru.saschoolsport.user.repository.UserRepository;
import ru.saschoolsport.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @Value("${app.admin.email:admin@saschoolsport.ru}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsernameAndDeletedAtIsNull("admin").isPresent()) {
            return;
        }

        RegisterRequest dto = new RegisterRequest();
        dto.setUsername("admin");
        dto.setPassword(adminPassword);
        dto.setConfirmPassword(adminPassword);
        dto.setFirstName("Admin");
        dto.setLastName("Admin");
        dto.setEmail(adminEmail);

        userService.createUser(dto);
        log.info("Default admin user created");
    }
}
