package com.revature.RevRelay.services;

import com.revature.RevRelay.models.Page;
import com.revature.RevRelay.models.dtos.UserDTO;
import com.revature.RevRelay.repositories.PageRepository;
import com.revature.RevRelay.repositories.UserRepository;
import com.revature.RevRelay.models.User;
import com.revature.RevRelay.models.dtos.UserRegisterAuthRequest;
import com.revature.RevRelay.utils.JwtUtil;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Returns a UserService object, which allows a User to update information on
 * their personal page
 */
@Service
@NoArgsConstructor
@Getter
@Setter
public class UserService implements UserDetailsService {

    private PageRepository pageRepository;
    private UserRepository userRepository;
    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;

    /**
     * All args constructor
     *
     * @param userRepository  UserRepository object autowired
     * @param jwtUtil         JwtUtil object autowired
     * @param passwordEncoder PasswordEncoder object autowired
     */
    @Autowired
    UserService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder, PageRepository pageRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.pageRepository = pageRepository;
    }

    /**
     * Logs in the user with the given username and password, then returns that
     * User.
     *
     * @param username the username to match.
     * @param password the password to match.
     * @return User of the given username AND password.
     * @throws AccessDeniedException Thrown when either a user cannot be loaded
     *                               or when there's a password mismatch.
     * @deprecated This method is marked for deletion as its function is handled
     * by the authentication method of the TokenAuthProvider class. - NL
     */
    public User login(String username, String password) throws AccessDeniedException {
        try {
            User user = loadUserByUsername(username);
            if (user.getPassword().equals(password))
                return user;
        } catch (Exception e) {
        } finally {
            throw new AccessDeniedException("Incorrect username/password");
        }
    }

    /**
     * Takes a user persists it then returns the user
     *
     * @param userAuthRequest The Auth Request corresponding to the user that is
     *                        going to be created
     * @return the full user object that was persisted is returned.
     */
    public User createUser(UserRegisterAuthRequest userAuthRequest) throws IllegalArgumentException {
        if (!isValidUsername(userAuthRequest.getUsername())) {
            throw new IllegalArgumentException("Username Not Valid");
        } else if (!isValidPassword(userAuthRequest.getPassword())) {
            throw new IllegalArgumentException("Password Not Valid");
        } else if (!isValidEmail(userAuthRequest.getEmail())) {
            throw new IllegalArgumentException("Email Not Valid");
        } else {
            User user = new User();
            user.setDisplayName(userAuthRequest.getDisplayName());
            user.setEmail(userAuthRequest.getEmail());
            user.setUsername(userAuthRequest.getUsername());
            user.setPassword(passwordEncoder.encode(userAuthRequest.getPassword()));
            user = userRepository.save(user);
            Page p = new Page(user);
            pageRepository.save(p);
            user.setUserPage(p);
            return userRepository.save(user);
        }
    }

    /**
     * Retrieves user by UserID without password
     *
     * @param userID userID expected to be in database.
     * @return UserDTO created from a User object in database.
     */
    public UserDTO loadUserDTOByUserID(int userID) {
        Optional<User> user = userRepository.findByUserID(userID);
        assert user.isPresent();
        return new UserDTO(user.get());
    }

    /**
     * Implementation of UserDetailsService method for Spring Security.
     *
     * @param username Username expected to be in database.
     * @return User object from database.
     * @throws UsernameNotFoundException Throws exception on empty optional from
     *                                   repository.
     */
    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new UsernameNotFoundException("Username Not Found");
        }
    }

    /**
     * Searches for a user by extracting the username from the jwtUtil
     *
     * @param token Token with information about the username inside the token
     * @return returns optional user
     * @throws Exception Throws exception if token does not exist OR optional is
     *                   null
     */
    public User findByToken(String token) throws Exception {
        Optional<User> user = userRepository.findByUsername(jwtUtil.extractUsername(token));
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new Exception("Token Does Not Correspond to User");
        }
    }

    /**
     * Updates a User's first name, to be displayed on their profile
     *
     * @param userID    The User's unique ID
     * @param firstName The User's desired first name
     * @return True if the update succeeds, or else false
     */
    public boolean updateFirstName(int userID, String firstName) {
        User user = userRepository.findByUserID(userID).orElse(null);
        if (user != null) {
            user.setFirstName(firstName);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Updates a User's last name, to be displayed on their profile
     *
     * @param userID   The User's unique ID
     * @param lastName The User's desired last name
     * @return True if the update succeeds, or else false
     */
    public boolean updateLastName(int userID, String lastName) {
        User user = userRepository.findByUserID(userID).orElse(null);
        if (user != null) {
            user.setLastName(lastName);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Attempts to update a User's password
     * Takes in the old password, the desired new password, and another instance of
     * the new password for confirmation
     * If the old password is incorrect, or the new password does not match the
     * confirmation, returns false
     * If a User's password is successfully updated, save the User and return true
     *
     * @param userID          The User's unique ID
     * @param oldPassword     The User's old password
     * @param newPassword     The User's desired new password
     * @param confirmPassword The User's desired new password
     * @return True if the update succeeds, or else false
     */
    public boolean updatePassword(int userID, String oldPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findByUserID(userID).orElse(null);
        if (user != null) {
            if (passwordEncoder.matches(oldPassword, user.getPassword())
                    && (newPassword.equals(confirmPassword))
                    && (isValidPassword(newPassword))) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    /**
     * Updates a User's birthday
     *
     * @param userID    The User's unique ID
     * @param birthDate The User's desired birthday
     * @return True if the update succeeds, or else false
     */
    public boolean updateBirthDate(int userID, Date birthDate) {
        User user = userRepository.findByUserID(userID).orElse(null);
        if (user != null) {
            user.setBirthDate(birthDate);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Updates a User's display name, which will be seen by other Users in chat
     * rooms/groups
     *
     * @param userID      The User's unique ID
     * @param displayName The User's desired display name
     * @return True if the update succeeds, or else false
     */
    public boolean updateDisplayName(int userID, String displayName) {
        User user = userRepository.findByUserID(userID).orElse(null);
        if (user != null) {
            user.setDisplayName(displayName);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Verifies that a username is suitable based on our constraints.
     *
     * @param username New username prior to hashing and storage to database.
     * @return True if valid, false if invalid.
     */
    private boolean isValidUsername(String username) {
        return (!userRepository.existsByUsername(username) && username != null);
    }

    /**
     * Verifies that a password is suitable based on our constraints.
     *
     * @param password New password prior to hashing and storage to database.
     * @return True if valid, false if invalid.
     */
    private boolean isValidPassword(String password) {
        return (password != null);
    }

    /**
     * Verifies that a email is suitable based on our constraints.
     *
     * @param email New email prior to hashing and storage to database.
     * @return True if valid, false if invalid.
     */
    private boolean isValidEmail(String email) {
        return (!userRepository.existsByEmail(email) && email != null);
    }

    public User addFriend(int userID, String friendUsername) throws Exception {
        User friend = userRepository.findByUsername(friendUsername).orElseThrow(() -> new Exception("No friend Found"));
        User user = userRepository.findByUserID(userID).orElseThrow(() -> new Exception("No person Found"));
        if (user.getUsername().equals(friend.getUsername())){
                return friend;
        }
        List<User> friends = user.getFriends();
        List<User> friendsFriends =friend.getFriends();
        friendsFriends.add(user);
        for (User friendInList: friends){
               if (friendInList.getUsername() == friend.getUsername()){
                   return friend;
               }
        };
        friends.add(friend);
        user.setFriends(friends);
        userRepository.save(user);
        return friend;
    }
}
