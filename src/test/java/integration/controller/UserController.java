package integration.controller;

import integration.entity.User;
import integration.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{user-id}")
    public User findUserWithOrders(@PathVariable("user-id") Long id) {
        return this.userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("user not found by user-id: " + id));
    }
}
