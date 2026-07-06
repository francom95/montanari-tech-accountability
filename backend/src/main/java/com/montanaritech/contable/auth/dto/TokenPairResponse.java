package com.montanaritech.contable.auth.dto;

/** Nombres de campo alineados con lo que ya espera el interceptor del frontend (F1.4, src/lib/http.ts). */
public record TokenPairResponse(String accessToken, String refreshToken) {
}
