package services;

import data.DB;
import data.UserRepository;
import models.User;

/**
 * Handles login, password hashing (SHA-256), and the currently logged-in user.
 */
public class AuthService {

    private static final UserRepository repo = new UserRepository();

    /** The user who is currently logged in. Null if nobody is logged in. */
    private static User currentUser = null;

    public static User getCurrentUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }

    /**
     * Attempts login. Returns true on success and sets currentUser.
     * Replaces BCrypt.Verify with SHA-256 comparison.
     */
    public static boolean login(String username, String password) {
        User user = repo.getByUsername(username);
        if (user == null || !user.isActive()) return false;

        String hash = DB.sha256(password);
        if (!hash.equals(user.getPasswordHash())) return false;

        currentUser = user;
        return true;
    }

    public static void logout() {
        currentUser = null;
    }

    /** Hashes a plain-text password using SHA-256. Replaces BCrypt. */
    public static String hashPassword(String plainPassword) {
        return DB.sha256(plainPassword);
    }

    /**
     * Changes the current user's password and clears the MustChangePassword flag.
     */
    public static void changePassword(String newPassword) {
        if (currentUser == null) return;
        String hash = hashPassword(newPassword);
        repo.updatePassword(currentUser.getUserID(), hash);
        currentUser.setMustChangePassword(false);
        currentUser.setPasswordHash(hash);
    }
}
