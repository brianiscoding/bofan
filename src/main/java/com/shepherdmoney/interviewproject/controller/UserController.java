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
        // TODO: Create an user entity with information given in the payload, store it
        // in the database
        // and return the id of the user in 200 OK response
        String name = payload.getName();
        String email = payload.getEmail();

        if (name.isEmpty() || email.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        userRepository.saveAndFlush(user);

        return new ResponseEntity<>(user.getId(), HttpStatus.OK);
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        // TODO: Return 200 OK if a user with the given ID exists, and the deletion is
        // successful
        // Return 400 Bad Request if a user with the ID does not exist
        // The response body could be anything you consider appropriate
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return new ResponseEntity<>(String.format("OKAY: %s deleted", userId), HttpStatus.OK);
        }
        return new ResponseEntity<>(String.format("ERROR: %s does not exist", userId), HttpStatus.BAD_REQUEST);
    }
}
