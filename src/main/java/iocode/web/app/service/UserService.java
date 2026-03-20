package iocode.web.app.service;

import iocode.web.app.dto.UserDto;
import iocode.web.app.entity.User;
import iocode.web.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(UserDto userDto){
        User user = mapToUser(userDto);
        return userRepository.save(user);
    }

    private User mapToUser(UserDto dto){
        return User.builder()
                .lastname(dto.getLastname())
                .firstname(dto.getFirstname())
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .dob(dto.getDob())
                .role(List.of("USER"))
                .tag("io_"+dto.getUsername())
                .build();
    }


}
