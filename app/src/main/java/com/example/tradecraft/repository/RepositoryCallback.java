package com.example.tradecraft.repository;

/** Generic async result callback so repositories stay decoupled from any specific async framework. */
public interface RepositoryCallback<T> {
    void onSuccess(T result);

    void onError(String errorMessage);
}
