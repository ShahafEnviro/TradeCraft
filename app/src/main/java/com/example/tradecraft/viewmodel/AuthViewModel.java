package com.example.tradecraft.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.tradecraft.model.User;
import com.example.tradecraft.repository.RepositoryCallback;
import com.example.tradecraft.repository.UserRepository;

/** Mediates between the Login/Main activities and UserRepository, exposing auth state as LiveData. */
public class AuthViewModel extends ViewModel {

    private final UserRepository repository = UserRepository.getInstance();

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loggedOutLiveData = new MutableLiveData<>(false);

    public LiveData<User> getUser() {
        return userLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<Boolean> getLoggedOut() {
        return loggedOutLiveData;
    }

    public boolean isLoggedIn() {
        return repository.getCurrentUser() != null;
    }

    public void signUp(String email, String password) {
        loadingLiveData.setValue(true);
        repository.signUp(email, password, new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User result) {
                loadingLiveData.setValue(false);
                userLiveData.setValue(result);
            }

            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });
    }

    public void login(String email, String password) {
        loadingLiveData.setValue(true);
        repository.login(email, password, new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User result) {
                loadingLiveData.setValue(false);
                userLiveData.setValue(result);
            }

            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });
    }

    /** Loads the profile of an already-authenticated user; used on app start and inside MainActivity. */
    public void loadCurrentUser() {
        if (!isLoggedIn()) {
            return;
        }
        loadingLiveData.setValue(true);
        repository.fetchCurrentUserProfile(new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User result) {
                loadingLiveData.setValue(false);
                userLiveData.setValue(result);
            }

            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });
    }

    public void logout() {
        repository.logout();
        userLiveData.setValue(null);
        loggedOutLiveData.setValue(true);
    }
}
