package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import com.shepherdmoney.interviewproject.model.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {
    @Autowired
    UserRepository userRepository;

    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        // CREATE USER AND RETURN ID
        User user = new User();
        user.setName(payload.getName());
        user.setEmail(payload.getEmail());
        userRepository.saveAndFlush(user);
        return new ResponseEntity<>(user.getId(), HttpStatus.OK);
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        // DELETE USER
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return new ResponseEntity<>(String.format("OKAY: %s deleted", userId), HttpStatus.OK);
        }
        return new ResponseEntity<>(String.format("ERROR: %s does not exist", userId), HttpStatus.BAD_REQUEST);
    }
}
