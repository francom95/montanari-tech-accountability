package com.montanaritech.contable.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CuitValidoValidator implements ConstraintValidator<CuitValido, String> {

    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String cuit, ConstraintValidatorContext context) {
        if (cuit == null || cuit.isBlank()) {
            return true;
        }
        if (!cuit.matches("^\\d{2}-\\d{8}-\\d{1}$")) {
            return false;
        }
        String digitos = cuit.replace("-", "");
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(digitos.charAt(i)) * PESOS[i];
        }
        int resto = suma % 11;
        int verificador = 11 - resto;
        if (verificador == 11) {
            verificador = 0;
        } else if (verificador == 10) {
            return false;
        }
        return verificador == Character.getNumericValue(digitos.charAt(10));
    }
}
