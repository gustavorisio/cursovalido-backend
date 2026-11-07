package com.cursovalido.backend.authentication.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record ExclusaoContaRequest(@NotBlank @Pattern(regexp="[0-9]{6}") String codigo) {}
