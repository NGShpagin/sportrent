package ru.saschoolsport.user.service;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.saschoolsport.user.dto.*;
import ru.saschoolsport.user.entity.User;
import ru.saschoolsport.user.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "username", "firstName", "lastName", "middleName", "createdAt", "updatedAt"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(UUID userId) {
        return UserProfileDto.fromEntity(findActiveUserById(userId));
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        return UserDto.fromEntity(findActiveUserById(id));
    }

    @Transactional(readOnly = true)
    public UserDto getInternalUser(UUID userId, String username) {
        User user = userId != null
                ? userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null)
                : userRepository.findByUsernameAndDeletedAtIsNull(username).orElse(null);

        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }
        return UserDto.fromEntity(user);
    }

    @Transactional
    public User createUser(RegisterRequest request) {
        if (userRepository.existsByUsernameAndDeletedAtIsNull(request.getUsername())) {
            throw new EntityExistsException("Username already taken: " + request.getUsername());
        }
        if (request.getEmail() != null && userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new EntityExistsException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        // TODO переделать в хэшировка
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMiddleName(request.getMiddleName());
        user.setEmail(request.getEmail());
        user.setBirthDate(request.getBirthDate());

        User savedUser = userRepository.save(user);
        return savedUser;
    }

    @Transactional
    public void updateUser(UUID id, UpdateUserRequest request) {
        User user = findActiveUserById(id);

        if (request.getEmail() != null
                && !request.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new EntityExistsException("Email already in use: " + request.getEmail());
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMiddleName(request.getMiddleName());
        user.setEmail(request.getEmail());
        user.setBirthDate(request.getBirthDate());
        User savedUser = userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = findActiveUserById(id);
        user.setDeletedAt(Instant.now());
        User savedUser = userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserContactInfoDto> getUserContactInfo(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllByIdInAndDeletedAtIsNull(userIds).stream()
                .map(UserContactInfoDto::fromEntity)
                .toList();
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Transactional(readOnly = true)
    public Page<User> getUsers(int page, int size, String sortBy, String sortDir) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1 (1-indexed)");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        Sort.Direction direction = Sort.Direction.fromOptionalString(sortDir)
                .orElseThrow(() -> new IllegalArgumentException("sort_dir must be 'asc' or 'desc'"));

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, resolveSortProperty(sortBy)));
        return userRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public User validateCredentials(String username, String password) {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (!checkPassword(user, password)) {
            throw new IllegalArgumentException("Invalid password");
        }
        return user;
    }

    public User findActiveUserById(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    private Pageable sanitizePageable(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sanitizeSort(pageable.getSort()));
    }

    private String resolveSortProperty(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "username";
        }
        if (ALLOWED_SORT_PROPERTIES.contains(sortBy)) {
            return sortBy;
        }
        String camelCase = toCamelCase(sortBy);
        if (ALLOWED_SORT_PROPERTIES.contains(camelCase)) {
            return camelCase;
        }
        throw new IllegalArgumentException(
                "Invalid sort property: " + sortBy + ". Allowed: " + ALLOWED_SORT_PROPERTIES);
    }

    private static String toCamelCase(String snakeCase) {
        if (!snakeCase.contains("_")) {
            return snakeCase;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : snakeCase.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
            } else if (upperNext) {
                result.append(Character.toUpperCase(ch));
                upperNext = false;
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private Sort sanitizeSort(Sort sort) {
        if (sort.isUnsorted()) {
            return Sort.by(Sort.Direction.ASC, "username");
        }
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort property: " + order.getProperty()
                                + ". Allowed: " + ALLOWED_SORT_PROPERTIES);
            }
        }
        return sort;
    }
}
