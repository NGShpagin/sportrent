package ru.saschoolsport.user.controller;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.saschoolsport.user.entity.User;
import ru.saschoolsport.user.exception.RestExceptionHandler;
import ru.saschoolsport.user.service.UserService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock UserService userService;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    private static final String VALID_REGISTRATION = """
            {"username":"newuser","password":"Passw0rd@1","confirmPassword":"Passw0rd@1",
             "firstName":"Test","lastName":"User"}""";

    @Test
    void internalLookupOfUnknownUserIs404NotA500() throws Exception {
        // authz-service relies on this to tell "no such user" apart from "user-service is broken"
        when(userService.getInternalUser(ID, null)).thenThrow(new EntityNotFoundException("User not found"));

        mvc.perform(get("/internal/users").param("userId", ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void internalLookupNeedsExactlyOneIdentifier() throws Exception {
        mvc.perform(get("/internal/users")).andExpect(status().isBadRequest());
        mvc.perform(get("/internal/users").param("userId", ID.toString()).param("username", "x"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    void getUnknownUserIs404() throws Exception {
        when(userService.getUserById(ID)).thenThrow(new EntityNotFoundException("User not found: " + ID));

        mvc.perform(get("/users/" + ID)).andExpect(status().isNotFound());
    }

    @Test
    void registeringTakenUsernameIs409NotA500() throws Exception {
        when(userService.createUser(any())).thenThrow(new EntityExistsException("Username already taken: newuser"));

        mvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken: newuser"));
    }

    @Test
    void registerReturns201() throws Exception {
        User saved = new User();
        saved.setId(ID);
        saved.setUsername("newuser");
        when(userService.createUser(any())).thenReturn(saved);

        mvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void registerRejectsWeakPasswordAndMismatchWithAReadableMessage() throws Exception {
        mvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"x\",\"confirmPassword\":\"x\","
                                + "\"firstName\":\"Test\",\"lastName\":\"User\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        mvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"Passw0rd@1\",\"confirmPassword\":\"Other0@pw\","
                                + "\"firstName\":\"Test\",\"lastName\":\"User\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    void credentialCheckNeverRevealsWhetherUserOrPasswordWasWrong() throws Exception {
        when(userService.validateCredentials("alice", "bad")).thenThrow(new IllegalArgumentException("Invalid password"));
        when(userService.validateCredentials("nobody", "x")).thenThrow(new EntityNotFoundException("User not found"));

        for (String body : new String[]{
                "{\"username\":\"alice\",\"password\":\"bad\"}", "{\"username\":\"nobody\",\"password\":\"x\"}"}) {
            mvc.perform(post("/internal/auth/validate").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));
        }
    }
}
