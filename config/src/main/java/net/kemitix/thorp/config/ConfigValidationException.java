package net.kemitix.thorp.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ConfigValidationException extends Exception {
    private final List<ConfigValidation> errors;
}
