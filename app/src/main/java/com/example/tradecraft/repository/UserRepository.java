package com.example.tradecraft.repository;

import com.example.tradecraft.model.Holding;
import com.example.tradecraft.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for authentication and user-profile persistence.
 * Wraps FirebaseAuth and Firestore so no other layer touches Firebase directly.
 */
public class UserRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String PORTFOLIO_SUBCOLLECTION = "portfolio";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_BALANCE = "balance";
    private static final String FIELD_SYMBOL = "symbol";
    private static final String FIELD_QUANTITY = "quantity";
    private static final String FIELD_AVG_BUY_PRICE = "avgBuyPrice";
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

    /** Reads every holding in the current user's portfolio subcollection. An empty list is a valid result. */
    public void getPortfolio(RepositoryCallback<List<Holding>> callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError("No authenticated user.");
            return;
        }
        firestore.collection(USERS_COLLECTION).document(firebaseUser.getUid())
                .collection(PORTFOLIO_SUBCOLLECTION)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Holding> holdings = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Holding holding = doc.toObject(Holding.class);
                        holdings.add(holding);
                    }
                    callback.onSuccess(holdings);
                })
                .addOnFailureListener(e -> callback.onError("Could not load your portfolio. Please try again."));
    }

    /** Reads a single holding by symbol; passes null to onSuccess if the user doesn't hold that symbol. */
    public void getHolding(String symbol, RepositoryCallback<Holding> callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError("No authenticated user.");
            return;
        }
        firestore.collection(USERS_COLLECTION).document(firebaseUser.getUid())
                .collection(PORTFOLIO_SUBCOLLECTION).document(symbol)
                .get()
                .addOnSuccessListener(snapshot ->
                        callback.onSuccess(snapshot.exists() ? snapshot.toObject(Holding.class) : null))
                .addOnFailureListener(e -> callback.onError("Could not load holding. Please try again."));
    }

    /**
     * Buys {@code quantity} shares of {@code symbol} at the given live {@code price} atomically:
     * deducts the cost from cash and upserts the holding (recomputing the weighted average cost).
     * Fails with "Insufficient funds." if the cost exceeds the current balance.
     */
    public void executeBuy(String symbol, long quantity, double price, RepositoryCallback<Void> callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError("No authenticated user.");
            return;
        }
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(firebaseUser.getUid());
        DocumentReference holdingRef = userRef.collection(PORTFOLIO_SUBCOLLECTION).document(symbol);

        firestore.runTransaction(transaction -> {
            // Firestore requires every read before any write inside a transaction.
            DocumentSnapshot userSnapshot = transaction.get(userRef);
            DocumentSnapshot holdingSnapshot = transaction.get(holdingRef);

            double balance = readDouble(userSnapshot, FIELD_BALANCE);
            double cost = price * quantity;
            if (cost > balance) {
                throw new FirebaseFirestoreException("Insufficient funds.",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            long newQuantity;
            double newAvgBuyPrice;
            if (holdingSnapshot.exists()) {
                long oldQuantity = readLong(holdingSnapshot, FIELD_QUANTITY);
                double oldAvg = readDouble(holdingSnapshot, FIELD_AVG_BUY_PRICE);
                newQuantity = oldQuantity + quantity;
                newAvgBuyPrice = (oldQuantity * oldAvg + quantity * price) / newQuantity;
            } else {
                newQuantity = quantity;
                newAvgBuyPrice = price;
            }

            transaction.update(userRef, FIELD_BALANCE, balance - cost);

            Map<String, Object> holding = new HashMap<>();
            holding.put(FIELD_SYMBOL, symbol);
            holding.put(FIELD_QUANTITY, newQuantity);
            holding.put(FIELD_AVG_BUY_PRICE, newAvgBuyPrice);
            transaction.set(holdingRef, holding);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(tradeError(e)));
    }

    /**
     * Sells {@code quantity} shares of {@code symbol} at the given live {@code price} atomically:
     * adds the proceeds to cash and decrements the holding (deleting it at zero). The average cost
     * basis on any remaining shares is left unchanged (no realized-P/L tracking in the MVP).
     * Fails with "You don't own enough shares." if the holding is missing or too small.
     */
    public void executeSell(String symbol, long quantity, double price, RepositoryCallback<Void> callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError("No authenticated user.");
            return;
        }
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(firebaseUser.getUid());
        DocumentReference holdingRef = userRef.collection(PORTFOLIO_SUBCOLLECTION).document(symbol);

        firestore.runTransaction(transaction -> {
            // Firestore requires every read before any write inside a transaction.
            DocumentSnapshot userSnapshot = transaction.get(userRef);
            DocumentSnapshot holdingSnapshot = transaction.get(holdingRef);

            long ownedQuantity = holdingSnapshot.exists() ? readLong(holdingSnapshot, FIELD_QUANTITY) : 0;
            if (!holdingSnapshot.exists() || quantity > ownedQuantity) {
                throw new FirebaseFirestoreException("You don't own enough shares.",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            double balance = readDouble(userSnapshot, FIELD_BALANCE);
            double proceeds = price * quantity;
            long newQuantity = ownedQuantity - quantity;

            transaction.update(userRef, FIELD_BALANCE, balance + proceeds);
            if (newQuantity == 0) {
                transaction.delete(holdingRef);
            } else {
                transaction.update(holdingRef, FIELD_QUANTITY, newQuantity);
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(tradeError(e)));
    }

    private double readDouble(DocumentSnapshot snapshot, String field) {
        Double value = snapshot.getDouble(field);
        return value != null ? value : 0.0;
    }

    private long readLong(DocumentSnapshot snapshot, String field) {
        Long value = snapshot.getLong(field);
        return value != null ? value : 0L;
    }

    /** Surfaces the thrown transaction message, falling back to a generic line if Firebase gives none. */
    private String tradeError(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Trade failed. Please try again.";
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
