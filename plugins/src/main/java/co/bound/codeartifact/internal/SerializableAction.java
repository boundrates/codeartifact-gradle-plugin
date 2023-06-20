package co.bound.codeartifact.internal;

import org.gradle.api.Action;

import java.io.Serializable;

public interface SerializableAction<T> extends Action<T>, Serializable {
}
