package com.example.tradecraft.repository;

import com.example.tradecraft.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for authentication and user-profile persistence.
 * Wraps FirebaseAuth and Firestore so no other layer touches Firebase directly.
 */
public class UserRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_BALANCE = "balance";
    private static final double STARTING_BALANCE = 100000.0;

    private static UserRepository instance;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    private UserRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    /** Creates the auth account, then the Firestore profile; both must succeed for overall success. */
    public void signUp(String email, String password, RepositoryCallback<User> callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Sign up failed: no user returned.");
                        return;
                    }
                    createUserProfile(firebaseUser, callback);
                })
                .addOnFailureListener(e -> callback.onError(mapAuthError(e)));
    }

    private void createUserProfile(FirebaseUser firebaseUser, RepositoryCallback<User> callback) {
        Map<String, Object> profile = new HashMap<>();
        profile.put(FIELD_EMAIL, firebaseUser.getEmail());
        profile.put(FIELD_BALANCE, STARTING_BALANCE);

        firestore.collection(USERS_COLLECTION).document(firebaseUser.getUid())
                .set(profile)
                .addOnSuccessListener(unused ->
                        callback.onSuccess(new User(firebaseUser.getEmail(), STARTING_BALANCE)))
                .addOnFailureListener(e ->
                        // Auth succeeded but the profile write didn't: roll back the auth account
                        // so sign-up is all-or-nothing and the user can safely retry.
                        firebaseUser.delete().addOnCompleteListener(deleteTask ->
                                callback.onError("Could not finish creating your account. Please try again.")));
    }

    public void login(String email, String password, RepositoryCallback<User> callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Login failed: no user returned.");
                        return;
                    }
                    fetchUserProfile(firebaseUser, callback);
                })
                .addOnFailureListener(e -> callback.onError(mapAuthError(e)));
    }

    /** Reads the Firestore profile of whichever user Firebase Auth currently has cached. */
    public void fetchCurrentUserProfile(RepositoryCallback<User> callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError("No authenticated user.");
            return;
        }
        fetchUserProfile(firebaseUser, callback);
    }

    private void fetchUserProfile(FirebaseUser firebaseUser, RepositoryCallback<User> callback) {
        firestore.collection(USERS_COLLECTION).document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onError("User profile not found.");
                        return;
                    }
                    Double balance = snapshot.getDouble(FIELD_BALANCE);
                    String storedEmail = snapshot.getString(FIELD_EMAIL);
                    callback.onSuccess(new User(storedEmail, balance != null ? balance : 0.0));
                })
                .addOnFailureListener(e -> callback.onError("Could not load your profile. Please try again."));
    }

    /** Translates Firebase's exception types into messages fit for display in the UI. */
    private String mapAuthError(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Password is too weak. Use at least 6 characters.";
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            return "An account with this email already exists.";
        } else if (e instanceof FirebaseAuthInvalidUserException) {
            return "No account found for this email.";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Invalid email or password.";
        }
        return e.getMessage() != null ? e.getMessage() : "Something went wrong. Please try again.";
    }
}
